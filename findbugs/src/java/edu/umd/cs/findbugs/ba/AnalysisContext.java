/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003-2005 University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package edu.umd.cs.findbugs.ba;

import java.io.File;
import java.io.IOException;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.jcip.annotations.NotThreadSafe;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.ba.ch.Subtypes;
import edu.umd.cs.findbugs.ba.interproc.PropertyDatabase;
import edu.umd.cs.findbugs.ba.interproc.PropertyDatabaseFormatException;
import edu.umd.cs.findbugs.ba.npe.ParameterNullnessPropertyDatabase;
import edu.umd.cs.findbugs.ba.type.FieldStoreTypeDatabase;
import edu.umd.cs.findbugs.detect.NoteAnnotationRetention;
import edu.umd.cs.findbugs.util.MapCache;


/**
 * A context for analysis of a complete project.
 * This serves as the repository for whole-program information
 * and data structures.
 *
 * @author David Hovemeyer
 */
@NotThreadSafe
public class AnalysisContext {
	private static final boolean DEBUG = Boolean.getBoolean("findbugs.analysiscontext.debug");
	
	public static final String DEFAULT_NONNULL_PARAM_DATABASE_FILENAME = "nonnullParam.db";
	public static final String DEFAULT_CHECK_FOR_NULL_PARAM_DATABASE_FILENAME = "checkForNullParam.db";
	public static final String DEFAULT_NULL_RETURN_VALUE_ANNOTATION_DATABASE = "nonnullReturn.db";
	public static final String UNCONDITIONAL_DEREF_DB_FILENAME = "unconditionalDeref.db";
	public static final String DEFAULT_NULL_RETURN_VALUE_DB_FILENAME = "mayReturnNull.db";
	
	private RepositoryLookupFailureCallback lookupFailureCallback;
	private SourceFinder sourceFinder;
	private MapCache<JavaClass, ClassContext> classContextCache;
	private Subtypes subtypes;
	public Map<Object,Object> analysisLocals = 
		Collections.synchronizedMap(new HashMap<Object,Object>());
	private BitSet boolPropertySet;
	private final SourceInfoMap sourceInfoMap;
	
	// Interprocedural fact databases
	private String databaseInputDir;
	private String databaseOutputDir;
	// private MayReturnNullPropertyDatabase mayReturnNullDatabase;
	private FieldStoreTypeDatabase fieldStoreTypeDatabase;
	private ParameterNullnessPropertyDatabase unconditionalDerefParamDatabase;
	
	
	private NullnessAnnotationDatabase nullnessAnnotationDatabase = new NullnessAnnotationDatabase();
	public NullnessAnnotationDatabase getNullnessAnnotationDatabase() {
		return nullnessAnnotationDatabase;
	}

	private CheckReturnAnnotationDatabase checkReturnAnnotationDatabase;
	public CheckReturnAnnotationDatabase getCheckReturnAnnotationDatabase() {
		return checkReturnAnnotationDatabase;
	}
	
	private AnnotationRetentionDatabase annotationRetentionDatabase;
	public AnnotationRetentionDatabase getAnnotationRetentionDatabase() {
		return annotationRetentionDatabase;
	}
	
	private JCIPAnnotationDatabase jcipAnnotationDatabase;
	public JCIPAnnotationDatabase getJCIPAnnotationDatabase() {
		return jcipAnnotationDatabase;
	}
	private static InheritableThreadLocal<AnalysisContext> currentAnalysisContext
		= new InheritableThreadLocal<AnalysisContext>();

	/**
	 * Default maximum number of ClassContext objects to cache.
	 * FIXME: need to evaluate this parameter. Need to keep stats about accesses.
	 */
	private static final int DEFAULT_CACHE_SIZE = 3;

	/**
	 * Constructor.
	 */
	public AnalysisContext(RepositoryLookupFailureCallback lookupFailureCallback) {
		this.lookupFailureCallback = lookupFailureCallback;
		this.sourceFinder = new SourceFinder();
		this.subtypes = new Subtypes();
		this.boolPropertySet = new BitSet();
		this.sourceInfoMap = new SourceInfoMap();
		
		currentAnalysisContext.set(this);
		
		//CheckReturnAnnotationDatabase may reportMissingClass, so do it after the currentAnalysisContext is set.
		//Otherwise null ptr exceptions will happen.
		
		/*
		checkReturnAnnotationDatabase = new CheckReturnAnnotationDatabase();

		To take the above comment one step further, there is a circular dependency here.
		CheckReturnAnnotationDatabase calls Repository.lookupClass() when it is initializing,
		so we don't want to instantiate it until after we have set up the repository.
		Unfortunately, we can't set up the repository properly without an AnalysisContext,
		and therefore not until after this AnalysisContext constructor returns.

		To handle this, we no longer instantiate the CheckReturnAnnotationDatabase here in
		the constructor, but instead require that initDatabases() be called later on, after 
		the repository has been set up.
		Yes this is ugly, but it will do for now. It's not worth the effort to redisign out
		the circular dependency properly if we plan to move from BCEL to ASM soon anyway.
		*/
	}

	/** Instantiate the CheckReturnAnnotationDatabase.
	 * Do this after the repository has been set up.
	 */
	public void initDatabases() {
		checkReturnAnnotationDatabase = new CheckReturnAnnotationDatabase();
		annotationRetentionDatabase = new AnnotationRetentionDatabase();
		 jcipAnnotationDatabase = new JCIPAnnotationDatabase();
	}

	/**
	 * Get the AnalysisContext associated with this thread
	 */
	static public AnalysisContext currentAnalysisContext() {
		return currentAnalysisContext.get();
	}

	/**
	 * 
	 */
	static public void reportMissingClass(ClassNotFoundException e) {
		currentAnalysisContext().getLookupFailureCallback().reportMissingClass(e);
	}
	
	/**
	 * Get the lookup failure callback.
	 */
	public RepositoryLookupFailureCallback getLookupFailureCallback() {
		return lookupFailureCallback;
	}

	/**
	 * Set the source path.
	 */
	public void setSourcePath(List<String> sourcePath) {
		sourceFinder.setSourceBaseList(sourcePath);
	}

	/**
	 * Get the SourceFinder, for finding source files.
	 */
	public SourceFinder getSourceFinder() {
		return sourceFinder;
	}

	/**
	 * Get the Subtypes database.
	 * 
	 * @return the Subtypes database
	 */
	public Subtypes getSubtypes() {
		return subtypes;
	}
	
	/**
	 * Clear the BCEL Repository in preparation for analysis.
	 */
	public void clearRepository() {
		// If the old repository backing store is a URLClassPathRepository
		// (which it certainly should be), destroy it.
		// This will close all underlying resources (archive files, etc.)
		org.apache.bcel.util.Repository repos = Repository.getRepository();
		if (repos instanceof URLClassPathRepository) {
			((URLClassPathRepository) repos).destroy();
		}
		
		// Purge repository of previous contents
		Repository.clearCache();
		
		// Clear any ClassContexts
		clearClassContextCache();

		// Clear InnerClassAccessMap cache.
		InnerClassAccessMap.instance().clearCache();

		// Create a URLClassPathRepository and make it current.
		URLClassPathRepository repository = new URLClassPathRepository(); 
		Repository.setRepository(repository);
	}
	
	/**
	 * Clear the ClassContext cache.
	 * This should be done between analysis passes.
	 */
	public void clearClassContextCache() {
		if (classContextCache != null)
			classContextCache.clear();
	}
	
	/**
	 * Add an entry to the Repository's classpath.
	 * 
	 * @param url the classpath entry URL
	 * @throws IOException
	 */
	public void addClasspathEntry(String url) throws IOException {
		URLClassPathRepository repos = (URLClassPathRepository) Repository.getRepository();
		repos.addURL(url);
	}

	/**
	 * Add an application class to the repository.
	 * 
	 * @param appClass the application class
	 */
	public void addApplicationClassToRepository(JavaClass appClass) {
		Repository.addClass(appClass);
		subtypes.addApplicationClass(appClass);
	}
	
	/**
	 * Return whether or not the given class is an application class.
	 * 
	 * @param cls the class to lookup
	 * @return true if the class is an application class, false if not
	 *         an application class or if the class cannot be located
	 */
	public boolean isApplicationClass(JavaClass cls) {
		return subtypes.isApplicationClass(cls);
	}

	/**
	 * Return whether or not the given class is an application class.
	 * 
	 * @param className name of a class
	 * @return true if the class is an application class, false if not
	 *         an application class or if the class cannot be located
	 */
	public boolean isApplicationClass(String className) {
		try {
			JavaClass javaClass = lookupClass(className);
			return isApplicationClass(javaClass);
		} catch (ClassNotFoundException e) {
			AnalysisContext.reportMissingClass(e);
			return false;
		}
	}

	/**
	 * Lookup a class.
	 * <em>Use this method instead of Repository.lookupClass().</em>
	 * 
	 * @param className the name of the class
	 * @return the JavaClass representing the class
	 * @throws ClassNotFoundException
	 */
	public JavaClass lookupClass(@NonNull String className) throws ClassNotFoundException {
		// TODO: eventually we should move to our own thread-safe repository implementation
		if (className == null) throw new IllegalArgumentException("className is null");
		return Repository.lookupClass(className);
	}
	
	/**
	 * Lookup a class's sourfe file
	 * 
	 * @param className the name of the class
	 * @return the source file for the class, or SourceLineAnnotation.UNKNOWN_SOURCE_FILE if unable to determine
	 */
	public String lookupSourceFile(@NonNull String className) {
		if (className == null) 
			throw new IllegalArgumentException("className is null");
		try {
			JavaClass jc = AnalysisContext.currentAnalysisContext().lookupClass(className);
			String name = jc.getSourceFileName();
			if (name == null) {
				System.out.println("No sourcefile for " + className);
				return SourceLineAnnotation.UNKNOWN_SOURCE_FILE;
			}
			return name;
		} catch (ClassNotFoundException cnfe) {
		  return SourceLineAnnotation.UNKNOWN_SOURCE_FILE;
		}
	}

	/**
	 * Get the ClassContext for a class.
	 *
	 * @param javaClass the class
	 * @return the ClassContext for that class
	 */
	int hits = 0;
	int misses = 0;
	public ClassContext getClassContext(JavaClass javaClass) {
		if (classContextCache == null) {
		int cacheSize = getBoolProperty(AnalysisFeatures.CONSERVE_SPACE) ? 1 : DEFAULT_CACHE_SIZE;
		classContextCache = new MapCache<JavaClass,ClassContext>(cacheSize);
		}
		ClassContext classContext = classContextCache.get(javaClass);
		if (classContext == null) {
			classContext = new ClassContext(javaClass, this);
			classContextCache.put(javaClass, classContext);
			misses++;
		} else hits++;
		return classContext;
	}

	public String getClassContextStats() {
		if (classContextCache == null) return null;
		return hits + "/" + misses + ":" + classContextCache.getStatistics();
	}
	/**
	 * If possible, load interprocedural property databases.
	 */
	public void loadInterproceduralDatabases() {
//		mayReturnNullDatabase = loadPropertyDatabase(
//				new MayReturnNullPropertyDatabase(),
//				DEFAULT_NULL_RETURN_VALUE_DB_FILENAME,
//				"may return null database");
		fieldStoreTypeDatabase = loadPropertyDatabase(
				new FieldStoreTypeDatabase(),
				FieldStoreTypeDatabase.DEFAULT_FILENAME,
				"field store type database");
			unconditionalDerefParamDatabase = loadPropertyDatabase(
				new ParameterNullnessPropertyDatabase(),
				UNCONDITIONAL_DEREF_DB_FILENAME,
				"unconditional param deref database");
	}
	
	/**
	 * Set a boolean property.
	 * 
	 * @param prop  the property to set
	 * @param value the value of the property
	 */
	public void setBoolProperty(int prop, boolean value) {
		boolPropertySet.set(prop, value);
	}
	
	/**
	 * Get a boolean property.
	 * 
	 * @param prop the property
	 * @return value of the property; defaults to false if the property
	 *         has not had a value assigned explicitly
	 */
	public boolean getBoolProperty(int prop) {
		return boolPropertySet.get(prop);
	}
	
	/**
	 * Get the SourceInfoMap.
	 */
	public SourceInfoMap getSourceInfoMap() {
		return sourceInfoMap;
	}
	
	/**
	 * Set the interprocedural database input directory.
	 * 
	 * @param databaseInputDir the interprocedural database input directory
	 */
	public void setDatabaseInputDir(String databaseInputDir) {
		if (DEBUG) System.out.println("Setting database input directory: " + databaseInputDir);
		this.databaseInputDir = databaseInputDir;
	}
	
	/**
	 * Get the interprocedural database input directory.
	 * 
	 * @return the interprocedural database input directory
	 */
	public String getDatabaseInputDir() {
		return databaseInputDir;
	}
	
	/**
	 * Set the interprocedural database output directory.
	 * 
	 * @param databaseOutputDir the interprocedural database output directory
	 */
	public void setDatabaseOutputDir(String databaseOutputDir) {
		if (DEBUG) System.out.println("Setting database output directory: " + databaseOutputDir);
		this.databaseOutputDir = databaseOutputDir;
	}
	
	/**
	 * Get the interprocedural database output directory.
	 * 
	 * @return the interprocedural database output directory
	 */
	public String getDatabaseOutputDir() {
		return databaseOutputDir;
	}
	

	
	/**
	 * Get the property database recording the types of values stored
	 * into fields.
	 * 
	 * @return the database, or null if there is no database available
	 */
	public FieldStoreTypeDatabase getFieldStoreTypeDatabase() {
		return fieldStoreTypeDatabase;
	}
	
		


	

	public void setUnconditionalDerefParamDatabase(
			ParameterNullnessPropertyDatabase unconditionalDerefParamDatabase) {
		this.unconditionalDerefParamDatabase = unconditionalDerefParamDatabase;
	}

	public ParameterNullnessPropertyDatabase getUnconditionalDerefParamDatabase() {
		return unconditionalDerefParamDatabase;
	}

	/**
	 * Load an interprocedural property database.
	 * 
	 * @param <DatabaseType> actual type of the database
	 * @param <KeyType>      type of key (e.g., method or field)
	 * @param <Property>     type of properties stored in the database
	 * @param database       the empty database object
	 * @param fileName       file to load database from
	 * @param description    description of the database (for diagnostics)
	 * @return the database object, or null if the database couldn't be loaded
	 */
	public<
		DatabaseType extends PropertyDatabase<KeyType,Property>,
		KeyType,
		Property
		> DatabaseType loadPropertyDatabase(
			DatabaseType database,
			String fileName,
			String description) {
		try {
			File dbFile = new File(getDatabaseInputDir(), fileName);
			if (DEBUG) System.out.println("Loading " + description + " from " + dbFile.getPath() + "...");
			
			database.readFromFile(dbFile.getPath());
			return database;
		} catch (IOException e) {
			getLookupFailureCallback().logError("Error loading " + description, e);
		} catch (PropertyDatabaseFormatException e) {
			getLookupFailureCallback().logError("Invalid " + description, e);
		}
		
		return null;
	}
	
	/**
	 * Write an interprocedural property database.
	 * 
	 * @param <DatabaseType> actual type of the database
	 * @param <KeyType>      type of key (e.g., method or field)
	 * @param <Property>     type of properties stored in the database
	 * @param database    the database
	 * @param fileName    name of database file
	 * @param description description of the database
	 */
	public<
		DatabaseType extends PropertyDatabase<KeyType,Property>,
		KeyType,
		Property
		> void storePropertyDatabase(DatabaseType database, String fileName, String description) {

		try {
			File dbFile = new File(getDatabaseOutputDir(), fileName);
			if (DEBUG) System.out.println("Writing " + description + " to " + dbFile.getPath() + "...");
			database.writeToFile(dbFile.getPath());
		} catch (IOException e) {
			getLookupFailureCallback().logError("Error writing " + description, e);
		}
	}
}

// vim:ts=4
