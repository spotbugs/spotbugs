/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003-2006 University of Maryland
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
import java.io.InputStream;
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
 * <p>
 * <b>NOTE</b>: this class is slated to become obsolete.
 * New code should use the IAnalysisCache object
 * returned by Global.getAnalysisCache() to access all analysis
 * information (global databases, class and method analyses, etc.)
 * </p>
 *
 * @author David Hovemeyer
 */
@NotThreadSafe
public abstract class AnalysisContext {
	public static final boolean DEBUG = Boolean.getBoolean("findbugs.analysiscontext.debug");
	
	public static final String DEFAULT_NONNULL_PARAM_DATABASE_FILENAME = "nonnullParam.db";
	public static final String DEFAULT_CHECK_FOR_NULL_PARAM_DATABASE_FILENAME = "checkForNullParam.db";
	public static final String DEFAULT_NULL_RETURN_VALUE_ANNOTATION_DATABASE = "nonnullReturn.db";
	public static final String UNCONDITIONAL_DEREF_DB_FILENAME = "unconditionalDeref.db";
	public static final String UNCONDITIONAL_DEREF_DB_RESOURCE = "jdkBaseUnconditionalDeref.db";
	public static final String DEFAULT_NULL_RETURN_VALUE_DB_FILENAME = "mayReturnNull.db";

	private static InheritableThreadLocal<AnalysisContext> currentAnalysisContext
		= new InheritableThreadLocal<AnalysisContext>();
	
	public abstract NullnessAnnotationDatabase getNullnessAnnotationDatabase();
	public abstract CheckReturnAnnotationDatabase getCheckReturnAnnotationDatabase();
	public abstract AnnotationRetentionDatabase getAnnotationRetentionDatabase();
	public abstract JCIPAnnotationDatabase getJCIPAnnotationDatabase();
	
	/** save the original SyntheticRepository so we may
	 *  obtain JavaClass objects which we can reuse.
	 *  (A URLClassPathRepository gets closed after analysis.) */
	private static final org.apache.bcel.util.Repository originalRepository =
		Repository.getRepository(); // BCEL SyntheticRepository

	/**
	 * Default maximum number of ClassContext objects to cache.
	 * FIXME: need to evaluate this parameter. Need to keep stats about accesses.
	 */
	private static final int DEFAULT_CACHE_SIZE = 3;

	/**
	 * Create a new AnalysisContext.
	 * 
	 * @param lookupFailureCallback the RepositoryLookupFailureCallback that
	 *                               the AnalysisContext should use to report errors
	 * @return a new AnalysisContext
	 */
	public static AnalysisContext create(RepositoryLookupFailureCallback lookupFailureCallback) {
		AnalysisContext analysisContext = new LegacyAnalysisContext(lookupFailureCallback);
		currentAnalysisContext.set(analysisContext);
		return analysisContext;
	}
	
	/** Instantiate the CheckReturnAnnotationDatabase.
	 *  Do this after the repository has been set up.
	 */
	public abstract void initDatabases();

	/**
	 * Get the AnalysisContext associated with this thread
	 */
	static public AnalysisContext currentAnalysisContext() {
		return currentAnalysisContext.get();
	}

	/**
	 * file a ClassNotFoundException with the lookupFailureCallback
	 * @see #getLookupFailureCallback()
	 */
	static public void reportMissingClass(ClassNotFoundException e) {
		currentAnalysisContext().getLookupFailureCallback().reportMissingClass(e);
	}
	
	/**
	 * Get the lookup failure callback.
	 */
	public abstract RepositoryLookupFailureCallback getLookupFailureCallback();

	/**
	 * Set the source path.
	 */
	public abstract void setSourcePath(List<String> sourcePath);

	/**
	 * Get the SourceFinder, for finding source files.
	 */
	public abstract SourceFinder getSourceFinder();

	/**
	 * Get the Subtypes database.
	 * 
	 * @return the Subtypes database
	 */
	public abstract Subtypes getSubtypes();
	
	/**
	 * Clear the BCEL Repository in preparation for analysis.
	 */
	public abstract void clearRepository();
	
	/**
	 * Clear the ClassContext cache.
	 * This should be done between analysis passes.
	 */
	public abstract void clearClassContextCache();
	
	/**
	 * Add an entry to the Repository's classpath.
	 * 
	 * @param url the classpath entry URL
	 * @throws IOException
	 */
	public abstract void addClasspathEntry(String url) throws IOException;

	/**
	 * Add an application class to the repository.
	 * 
	 * @param appClass the application class
	 */
	public abstract void addApplicationClassToRepository(JavaClass appClass);
	
	/**
	 * Return whether or not the given class is an application class.
	 * 
	 * @param cls the class to lookup
	 * @return true if the class is an application class, false if not
	 *         an application class or if the class cannot be located
	 */
	public abstract boolean isApplicationClass(JavaClass cls);

	/**
	 * Return whether or not the given class is an application class.
	 * 
	 * @param className name of a class
	 * @return true if the class is an application class, false if not
	 *         an application class or if the class cannot be located
	 */
	public abstract boolean isApplicationClass(String className);

	/**
	 * Lookup a class.
	 * <em>Use this method instead of Repository.lookupClass().</em>
	 * 
	 * @param className the name of the class
	 * @return the JavaClass representing the class
	 * @throws ClassNotFoundException (but not really)
	 */
	public abstract JavaClass lookupClass(@NonNull String className) throws ClassNotFoundException;
	
	/**
	 * This is equivalent to Repository.lookupClass() or this.lookupClass(),
	 * except it uses the original Repository instead of the current one.
	 * 
	 * This can be important because URLClassPathRepository objects are
	 * closed after an analysis, so JavaClass objects obtained from them
	 * are no good on subsequent runs.
	 * 
	 * @param className the name of the class
	 * @return the JavaClass representing the class
	 * @throws ClassNotFoundException
	 */
	public static JavaClass lookupSystemClass(@NonNull String className) throws ClassNotFoundException {
		// TODO: eventually we should move to our own thread-safe repository implementation
		if (className == null) throw new IllegalArgumentException("className is null");
		if (originalRepository == null) throw new IllegalStateException("originalRepository is null");

		JavaClass clazz = originalRepository.findClass(className);
		return (clazz==null ? originalRepository.loadClass(className) : clazz);
	}

	/**
	 * Lookup a class's source file
	 * 
	 * @param className the name of the class
	 * @return the source file for the class, or SourceLineAnnotation.UNKNOWN_SOURCE_FILE if unable to determine
	 */
	public abstract String lookupSourceFile(@NonNull String className);

	/**
	 * Get the ClassContext for a class.
	 *
	 * @param javaClass the class
	 * @return the ClassContext for that class
	 */
	public abstract ClassContext getClassContext(JavaClass javaClass);
	
	/**
	 * Get stats about hit rate for ClassContext cache.
	 * 
	 * @return stats about hit rate for ClassContext cache
	 */
	public abstract String getClassContextStats();
	
	/**
	 * If possible, load interprocedural property databases.
	 */
	public abstract void loadInterproceduralDatabases();
	
	/**
	 * If possible, load default (built-in) interprocedural property databases.
	 * These are the databases for things like Java core APIs that
	 * unconditional dereference parameters.
	 */
	public abstract void loadDefaultInterproceduralDatabases();

	/**
	 * Set a boolean property.
	 * 
	 * @param prop  the property to set
	 * @param value the value of the property
	 */
	public abstract void setBoolProperty(int prop, boolean value);

	/**
	 * Get a boolean property.
	 * 
	 * @param prop the property
	 * @return value of the property; defaults to false if the property
	 *         has not had a value assigned explicitly
	 */
	public abstract boolean getBoolProperty(int prop);
	
	/**
	 * Get the SourceInfoMap.
	 */
	public abstract SourceInfoMap getSourceInfoMap();
	
	/**
	 * Set the interprocedural database input directory.
	 * 
	 * @param databaseInputDir the interprocedural database input directory
	 */
	public abstract void setDatabaseInputDir(String databaseInputDir);

	/**
	 * Get the interprocedural database input directory.
	 * 
	 * @return the interprocedural database input directory
	 */
	public abstract String getDatabaseInputDir();
	
	/**
	 * Set the interprocedural database output directory.
	 * 
	 * @param databaseOutputDir the interprocedural database output directory
	 */
	public abstract void setDatabaseOutputDir(String databaseOutputDir);

	/**
	 * Get the interprocedural database output directory.
	 * 
	 * @return the interprocedural database output directory
	 */
	public abstract String getDatabaseOutputDir();
	
	/**
	 * Get the property database recording the types of values stored
	 * into fields.
	 * 
	 * @return the database, or null if there is no database available
	 */
	public abstract FieldStoreTypeDatabase getFieldStoreTypeDatabase();

	/**
	 * Get the property database recording which methods unconditionally
	 * dereference parameters.
	 * 
	 * @return the database, or null if there is no database available
	 */
	public abstract ParameterNullnessPropertyDatabase getUnconditionalDerefParamDatabase();

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
	 * Load an interprocedural property database.
	 * 
	 * @param <DatabaseType> actual type of the database
	 * @param <KeyType>      type of key (e.g., method or field)
	 * @param <Property>     type of properties stored in the database
	 * @param database       the empty database object
	 * @param resourceName   name of resource to load the database from
	 * @param description    description of the database (for diagnostics)
	 * @return the database object, or null if the database couldn't be loaded
	 */
	public<
		DatabaseType extends PropertyDatabase<KeyType,Property>,
		KeyType,
		Property
		> DatabaseType loadPropertyDatabaseFromResource(
			DatabaseType database,
			String resourceName,
			String description) {
		try {
			if (DEBUG) System.out.println("Loading default " + description + " from " 
					+ resourceName + " @ "
			 + PropertyDatabase.class.getResource(resourceName) + " ... ");
			InputStream in = PropertyDatabase.class.getResourceAsStream(resourceName);
			database.read(in);
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
	
	public abstract Map getAnalysisLocals();
	
	public abstract InnerClassAccessMap getInnerClassAccessMap();
}

// vim:ts=4
