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

import edu.umd.cs.findbugs.AbstractBugReporter;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.ba.ch.Subtypes;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.ba.interproc.PropertyDatabase;
import edu.umd.cs.findbugs.ba.interproc.PropertyDatabaseFormatException;
import edu.umd.cs.findbugs.ba.npe.ParameterNullnessPropertyDatabase;
import edu.umd.cs.findbugs.ba.npe.ReturnValueNullnessPropertyDatabase;
import edu.umd.cs.findbugs.ba.type.FieldStoreTypeDatabase;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.FieldOrMethodDescriptor;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.detect.NoteAnnotationRetention;
import edu.umd.cs.findbugs.detect.UnreadFields;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.util.ClassName;
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
 * @see IAnalysisCache
 * @see edu.umd.cs.findbugs.classfile.Global
 */
@NotThreadSafe
public abstract class AnalysisContext {
	public static final boolean DEBUG = SystemProperties.getBoolean("findbugs.analysiscontext.debug");
	public static final boolean IGNORE_BUILTIN_MODELS = SystemProperties.getBoolean("findbugs.ignoreBuiltinModels");

	public static final String DEFAULT_NONNULL_PARAM_DATABASE_FILENAME = "nonnullParam.db";


	public static final String DEFAULT_CHECK_FOR_NULL_PARAM_DATABASE_FILENAME = "checkForNullParam.db";
	public static final String DEFAULT_NULL_RETURN_VALUE_ANNOTATION_DATABASE = "nullReturn.db";

	public static final String UNCONDITIONAL_DEREF_DB_FILENAME = "unconditionalDeref.db";
	public static final String NONNULL_RETURN_DB_FILENAME = "nonnullReturn.db";

	public static final String UNCONDITIONAL_DEREF_DB_RESOURCE = "jdkBaseUnconditionalDeref.db";
	public static final String NONNULL_RETURN_DB_RESOURCE = "jdkBaseNonnullReturn.db";

	public static final String DEFAULT_NULL_RETURN_VALUE_DB_FILENAME = "mayReturnNull.db";

	private static InheritableThreadLocal<AnalysisContext> currentAnalysisContext
		= new InheritableThreadLocal<AnalysisContext>() {
		@Override
		public AnalysisContext initialValue() {
			// throw new IllegalStateException("currentAnalysisContext should be set by AnalysisContext.setCurrentAnalysisContext");
			return null;
		}
	};

	private static InheritableThreadLocal<XFactory> currentXFactory
	= new InheritableThreadLocal<XFactory>() {
		@Override
		public XFactory initialValue() {
			throw new IllegalStateException("currentXFactory should be set by AnalysisContext.setCurrentAnalysisContext");
		}
	};

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


	// Instance fields
	private BitSet boolPropertySet;
	private Map<Object,Object> analysisLocals;
	private String databaseInputDir;
	private String databaseOutputDir;

	protected AnalysisContext() {
		this.boolPropertySet = new BitSet();
		this.analysisLocals = Collections.synchronizedMap(new HashMap<Object,Object>());
	}

	/**
	 * Create a new AnalysisContext.
	 * 
	 * @param lookupFailureCallback the RepositoryLookupFailureCallback that
	 *                               the AnalysisContext should use to report errors
	 * @return a new AnalysisContext
	 */
	public static AnalysisContext create(RepositoryLookupFailureCallback lookupFailureCallback) {
		AnalysisContext analysisContext = new LegacyAnalysisContext(lookupFailureCallback);
		setCurrentAnalysisContext(analysisContext);
		return analysisContext;
	}

	/** Instantiate the CheckReturnAnnotationDatabase.
	 *  Do this after the repository has been set up.
	 */
	public abstract void initDatabases();


	/**
	 * After a pass has been completed, allow the analysis context to update information.
	 * @param pass -- the first pass is pass 0
	 */
	public abstract void updateDatabases(int pass);
	/**
	 * Get the AnalysisContext associated with this thread
	 */
	static public AnalysisContext currentAnalysisContext() {
		return currentAnalysisContext.get();
	}

	static public XFactory currentXFactory() {
		return currentXFactory.get();
	}

	UnreadFields unreadFields;
	public UnreadFields getUnreadFields() {
		if (unreadFields == null) throw new IllegalStateException("UnreadFields detector not set");
		return unreadFields;
	}
	public void setUnreadFields(@NonNull UnreadFields unreadFields) {
		if (this.unreadFields != null) throw new IllegalStateException("UnreadFields detector already set");
		this.unreadFields = unreadFields;
	}
	/**
	 * file a ClassNotFoundException with the lookupFailureCallback
	 * @see #getLookupFailureCallback()
	 */
	static public void reportMissingClass(ClassNotFoundException e) {
		String missing = AbstractBugReporter.getMissingClassName(e);
		if (missing.charAt(0) == '[') return;
		if (e == null) throw new NullPointerException("argument is null");
		AnalysisContext currentAnalysisContext2 = currentAnalysisContext();
		if (currentAnalysisContext2 == null) return;
		if (currentAnalysisContext2.missingClassWarningsSuppressed) return;
		RepositoryLookupFailureCallback lookupFailureCallback = currentAnalysisContext2.getLookupFailureCallback();
		if (lookupFailureCallback != null) lookupFailureCallback.reportMissingClass(e);
	}

	/**
	 * Report an error
	 */
	static public void logError(String msg, Exception e) {
		AnalysisContext currentAnalysisContext2 = currentAnalysisContext();
		if (currentAnalysisContext2 == null) return;
		RepositoryLookupFailureCallback lookupFailureCallback = currentAnalysisContext2.getLookupFailureCallback();
		if (lookupFailureCallback != null) lookupFailureCallback.logError(msg, e);
	}
	/**
	 * Report an error
	 */
	static public void logError(String msg) {
		AnalysisContext currentAnalysisContext2 = currentAnalysisContext();
		if (currentAnalysisContext2 == null) return;
		RepositoryLookupFailureCallback lookupFailureCallback = currentAnalysisContext2.getLookupFailureCallback();
		if (lookupFailureCallback != null) lookupFailureCallback.logError(msg);
	}

	boolean missingClassWarningsSuppressed = false;

	public boolean setMissingClassWarningsSuppressed(boolean value) {
		boolean oldValue = missingClassWarningsSuppressed;
		missingClassWarningsSuppressed = value;
		return oldValue;
	}
	/**
	 * Get the lookup failure callback.
	 */
	public abstract RepositoryLookupFailureCallback getLookupFailureCallback();

	/**
	 * Set the source path.
	 */
	public final void setSourcePath(List<String> sourcePath) {
		getSourceFinder().setSourceBaseList(sourcePath);
	}

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
	public boolean isApplicationClass(JavaClass cls) {
		return getSubtypes().isApplicationClass(cls);
	}

	/**
	 * Return whether or not the given class is an application class.
	 * 
	 * @param className name of a class
	 * @return true if the class is an application class, false if not
	 *         an application class or if the class cannot be located
	 */
	public boolean isApplicationClass(@DottedClassName String className) {
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
	 * @throws ClassNotFoundException (but not really)
	 */
	public abstract JavaClass lookupClass(@NonNull String className) throws ClassNotFoundException;

	/**
	 * Lookup a class.
	 * <em>Use this method instead of Repository.lookupClass().</em>
	 * 
	 * @param classDescriptor descriptor specifying the class to look up
	 * @return the class
	 * @throws ClassNotFoundException if the class can't be found
	 */
	public JavaClass lookupClass(@NonNull ClassDescriptor classDescriptor) throws ClassNotFoundException {
		return lookupClass(classDescriptor.toDottedClassName());
	}

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
	public final String lookupSourceFile(@NonNull String className) {
		if (className == null) 
			throw new IllegalArgumentException("className is null");
		try {
			JavaClass jc = this.lookupClass(className);
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
	public final void loadInterproceduralDatabases() {
		loadPropertyDatabase(
				getFieldStoreTypeDatabase(),
				FieldStoreTypeDatabase.DEFAULT_FILENAME,
				"field store type database");
		loadPropertyDatabase(
				getUnconditionalDerefParamDatabase(),
				UNCONDITIONAL_DEREF_DB_FILENAME,
				"unconditional param deref database");
		loadPropertyDatabase(
				getReturnValueNullnessPropertyDatabase(),
				NONNULL_RETURN_DB_FILENAME,
				"nonnull return db database");
	}

	/**
	 * If possible, load default (built-in) interprocedural property databases.
	 * These are the databases for things like Java core APIs that
	 * unconditional dereference parameters.
	 */
	public final void loadDefaultInterproceduralDatabases() {
		if (IGNORE_BUILTIN_MODELS) return;
		loadPropertyDatabaseFromResource(
				getUnconditionalDerefParamDatabase(),
				UNCONDITIONAL_DEREF_DB_RESOURCE,
				"unconditional param deref database");
		loadPropertyDatabaseFromResource(
				getReturnValueNullnessPropertyDatabase(),
				NONNULL_RETURN_DB_RESOURCE,
				"nonnull return db database");
	}

	/**
	 * Set a boolean property.
	 * 
	 * @param prop  the property to set
	 * @param value the value of the property
	 */
	public final void setBoolProperty(int prop, boolean value) {
		boolPropertySet.set(prop, value);
	}

	/**
	 * Get a boolean property.
	 * 
	 * @param prop the property
	 * @return value of the property; defaults to false if the property
	 *         has not had a value assigned explicitly
	 */
	public final boolean getBoolProperty(int prop) {
		return boolPropertySet.get(prop);
	}

	/**
	 * Get the SourceInfoMap.
	 */
	public abstract SourceInfoMap getSourceInfoMap();

	/**
	 * Set the interprocedural database input directory.
	 * 
	 * @param databaseInputDir the interprocedural database input directory
	 */
	public final void setDatabaseInputDir(String databaseInputDir) {
		if (DEBUG) System.out.println("Setting database input directory: " + databaseInputDir);
		this.databaseInputDir = databaseInputDir;
	}

	/**
	 * Get the interprocedural database input directory.
	 * 
	 * @return the interprocedural database input directory
	 */
	public final String getDatabaseInputDir() {
		return databaseInputDir;
	}

	/**
	 * Set the interprocedural database output directory.
	 * 
	 * @param databaseOutputDir the interprocedural database output directory
	 */
	public final void setDatabaseOutputDir(String databaseOutputDir) {
		if (DEBUG) System.out.println("Setting database output directory: " + databaseOutputDir);
		this.databaseOutputDir = databaseOutputDir;
	}

	/**
	 * Get the interprocedural database output directory.
	 * 
	 * @return the interprocedural database output directory
	 */
	public final String getDatabaseOutputDir() {
		return databaseOutputDir;
	}

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
	 * Get the property database recording which methods always return nonnull values
	 * 
	 * @return the database, or null if there is no database available
	 */
	public abstract ReturnValueNullnessPropertyDatabase getReturnValueNullnessPropertyDatabase();

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
		KeyType extends FieldOrMethodDescriptor,
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
		KeyType extends FieldOrMethodDescriptor,
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
			in.close();
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
		KeyType extends FieldOrMethodDescriptor,
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


	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#getAnalyisLocals()
	 */
	public final Map<Object, Object> getAnalysisLocals() {
		return analysisLocals;
	}

	public abstract InnerClassAccessMap getInnerClassAccessMap();

	/**
	 * Set the current analysis context for this thread.
	 * 
	 * @param analysisContext the current analysis context for this thread
	 */
	public static void setCurrentAnalysisContext(AnalysisContext analysisContext) {
		currentAnalysisContext.set(analysisContext);
		currentXFactory.set(new XFactory());
	}
	
	/**
	 * Get the database which keeps track of synthetic fields/methods/classes.
	 */
	public abstract SyntheticElements getSyntheticElements();

	/**
	 * Get the Subtypes2 inheritance hierarchy database.
	 */
	public abstract Subtypes2 getSubtypes2();

}

// vim:ts=4
