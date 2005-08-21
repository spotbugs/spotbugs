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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.ba.ch.Subtypes;
import edu.umd.cs.findbugs.ba.interproc.PropertyDatabase;
import edu.umd.cs.findbugs.ba.interproc.PropertyDatabaseFormatException;
import edu.umd.cs.findbugs.ba.npe.MayReturnNullPropertyDatabase;
import edu.umd.cs.findbugs.ba.npe.ParameterNullnessPropertyDatabase;
import edu.umd.cs.findbugs.ba.type.FieldStoreTypeDatabase;


/**
 * A context for analysis of a complete project.
 * This serves as the repository for whole-program information
 * and data structures.
 *
 * @author David Hovemeyer
 */
public class AnalysisContext {
	private static final boolean DEBUG = Boolean.getBoolean("findbugs.analysiscontext.debug");
	private static final boolean DEBUG_HIERARCHY = Boolean.getBoolean("findbugs.debug.hierarchy");
	
	public static final String DEFAULT_NONNULL_PARAM_DATABASE_FILENAME = "nonnullParam.db";
	public static final String DEFAULT_CHECK_FOR_NULL_PARAM_DATABASE_FILENAME = "checkForNullParam.db";
	public static final String DEFAULT_NULL_RETURN_VALUE_ANNOTATION_DATABASE = "nonnullReturn.db";
	public static final String UNCONDITIONAL_DEREF_DB_FILENAME = "unconditionalDeref.db";
	public static final String DEFAULT_NULL_RETURN_VALUE_DB_FILENAME = "mayReturnNull.db";
	
	private RepositoryLookupFailureCallback lookupFailureCallback;
	private SourceFinder sourceFinder;
	private ClassContextCache classContextCache;
	private Subtypes subtypes;
	public Map<Object,Object> analysisLocals = 
		Collections.synchronizedMap(new HashMap<Object,Object>());
	private BitSet boolPropertySet;
	private int cacheSize;
	
	// Interprocedural fact databases
	private String databaseInputDir;
	private String databaseOutputDir;
	// private MayReturnNullPropertyDatabase mayReturnNullDatabase;
	private MayReturnNullPropertyDatabase nullReturnValueAnnotationDatabase;
	private FieldStoreTypeDatabase fieldStoreTypeDatabase;
	private ParameterNullnessPropertyDatabase nonNullParamDatabase;
	private ParameterNullnessPropertyDatabase checkForNullParamDatabase;
	private ParameterNullnessPropertyDatabase unconditionalDerefParamDatabase;
	
	
	private NullnessAnnotationDatabase nullnessAnnotationDatabase = new NullnessAnnotationDatabase();
	public NullnessAnnotationDatabase getNullnessAnnotationDatabase() {
		return nullnessAnnotationDatabase;
	}

	private static InheritableThreadLocal<AnalysisContext> currentAnalysisContext
		= new InheritableThreadLocal<AnalysisContext>();

	/**
	 * Default maximum number of ClassContext objects to cache.
	 * FIXME: need to evaluate this parameter. Need to keep stats about accesses.
	 */
	private static final int DEFAULT_CACHE_SIZE = 60;

	private class ClassContextCache extends LinkedHashMap<JavaClass, ClassContext> {
		private static final long serialVersionUID = 1L;

		public boolean removeEldestEntry(Map.Entry<JavaClass, ClassContext> entry) {
			return size() >= cacheSize;
		}
	}

	/**
	 * Constructor.
	 */
	public AnalysisContext(RepositoryLookupFailureCallback lookupFailureCallback) {
		this.lookupFailureCallback = lookupFailureCallback;
		this.sourceFinder = new SourceFinder();
		this.classContextCache = new ClassContextCache();
		this.subtypes = new Subtypes();
		this.boolPropertySet = new BitSet();

		currentAnalysisContext.set(this);
	}

	/**
	 * Get the AnalysisContext associated with this thread
	 */
	static public AnalysisContext currentAnalysisContext() {
		return currentAnalysisContext.get();
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
	public JavaClass lookupClass(String className) throws ClassNotFoundException {
		// TODO: eventually we should move to our own thread-safe repository implementation
		return Repository.lookupClass(className);
	}

	/**
	 * Get the ClassContext for a class.
	 *
	 * @param javaClass the class
	 * @return the ClassContext for that class
	 */
	public ClassContext getClassContext(JavaClass javaClass) {
		if (cacheSize == 0) {
			cacheSize = getBoolProperty(AnalysisFeatures.CONSERVE_SPACE) ? 1 : DEFAULT_CACHE_SIZE;
		}
		
		ClassContext classContext = classContextCache.get(javaClass);
		if (classContext == null) {
			classContext = new ClassContext(javaClass, this);
			classContextCache.put(javaClass, classContext);
		}
		return classContext;
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
		nonNullParamDatabase = loadPropertyDatabase(
				new ParameterNullnessPropertyDatabase(),
				DEFAULT_NONNULL_PARAM_DATABASE_FILENAME,
				"@NonNull parameter annotation database");
		checkForNullParamDatabase = loadPropertyDatabase(
				new ParameterNullnessPropertyDatabase(),
				DEFAULT_CHECK_FOR_NULL_PARAM_DATABASE_FILENAME,
				"@CheckForNull parameter annotation database");
		nullReturnValueAnnotationDatabase= loadPropertyDatabase(
				new MayReturnNullPropertyDatabase(),
				DEFAULT_NULL_RETURN_VALUE_ANNOTATION_DATABASE,
				"@NonNull/@CheckForNull return value annotation database"
				);
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
	
	/**
	 * Set non-null param database.
	 * Based on NonNull annotations.
	 * 
	 * @param nonNullParamDatabase the non-null param database
	 */
	public void setNonNullParamDatabase(
			ParameterNullnessPropertyDatabase nonNullParamDatabase) {
		this.nonNullParamDatabase = nonNullParamDatabase;
	}
	
	public ParameterNullnessPropertyDatabase getNonNullParamDatabase() {
		return nonNullParamDatabase;
	}
	

	/**
	 * Set possibly-null param database.
	 * Based on CheckForNull annotations.
	 * 
	 * @param possiblyNullParamDatabase the possibly-null param database
	 */
	public void setCheckForNullParamDatabase(
			ParameterNullnessPropertyDatabase possiblyNullParamDatabase) {
		this.checkForNullParamDatabase = possiblyNullParamDatabase;
	}
	
	public ParameterNullnessPropertyDatabase getCheckForNullParamDatabase() {
		return checkForNullParamDatabase;
	}
	
	public void setNullReturnValueAnnotationDatabase(
			MayReturnNullPropertyDatabase nullReturnValueAnnotationDatabase) {
		this.nullReturnValueAnnotationDatabase = nullReturnValueAnnotationDatabase;
	}
	
	public MayReturnNullPropertyDatabase getNullReturnValueAnnotationDatabase() {
		return nullReturnValueAnnotationDatabase;
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
