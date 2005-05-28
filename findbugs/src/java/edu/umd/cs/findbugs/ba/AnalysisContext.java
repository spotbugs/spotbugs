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
import edu.umd.cs.findbugs.ba.type.FieldStoreTypeDatabase;


/**
 * A context for analysis of a complete project.
 * This serves as the repository for whole-program information
 * and data structures.
 *
 * @author David Hovemeyer
 */
public class AnalysisContext implements AnalysisFeatures {
	private static final boolean DEBUG = Boolean.getBoolean("findbugs.analysiscontext.debug");
	private static final boolean DEBUG_HIERARCHY = Boolean.getBoolean("findbugs.debug.hierarchy");
	
	public static final boolean USE_INTERPROC_DATABASE = Boolean.getBoolean("findbugs.interproc");
	public static final String INTERPROC_DATABASE_DIR =
		System.getProperty("findbugs.interproc.dbdir", ".");
	
	private RepositoryLookupFailureCallback lookupFailureCallback;
	private SourceFinder sourceFinder;
	private ClassContextCache classContextCache;
	private Subtypes subtypes;
	public Map analysisLocals = 
		Collections.synchronizedMap(new HashMap());
	private BitSet boolPropertySet;
	
	// Interprocedural fact databases
	private MayReturnNullPropertyDatabase mayReturnNullDatabase;
	private FieldStoreTypeDatabase fieldStoreTypeDatabase;
	private boolean interprocDatabasesLoaded;

    /*
      // JSR14 does not support Generic ThreadLocal
	private static InheritableThreadLocal<AnalysisContext> currentAnalysisContext
		= new InheritableThreadLocal<AnalysisContext>();
    */
	private static InheritableThreadLocal currentAnalysisContext
		= new InheritableThreadLocal();

	/**
	 * The maximum number of ClassContext objects to cache.
	 * FIXME: need to evaluate this parameter. Need to keep stats
	 * about accesses.
	 */
	private static final int MAX_SIZE = CONSERVE_SPACE ? 1 : 60;

	private static class ClassContextCache extends LinkedHashMap<JavaClass, ClassContext> {
		private static final long serialVersionUID = 3258410621153196086L;

		public boolean removeEldestEntry(Map.Entry<JavaClass, ClassContext> entry) {
			return size() >= MAX_SIZE;
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
		return (AnalysisContext)currentAnalysisContext.get();
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

		// Clear InnerClassAccessMap cache.
		InnerClassAccessMap.instance().clearCache();

		// Create a URLClassPathRepository and make it current.
		URLClassPathRepository repository = new URLClassPathRepository(); 
		Repository.setRepository(repository);
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
		loadInterproceduralDatabasesIfNeeded();
		
		ClassContext classContext = classContextCache.get(javaClass);
		if (classContext == null) {
			classContext = new ClassContext(javaClass, this);
			classContextCache.put(javaClass, classContext);
		}
		return classContext;
	}

	private void loadInterproceduralDatabasesIfNeeded() {
		if (USE_INTERPROC_DATABASE && !interprocDatabasesLoaded) {
			mayReturnNullDatabase = loadPropertyDatabase(
					new MayReturnNullPropertyDatabase(),
					MayReturnNullPropertyDatabase.DEFAULT_FILENAME,
					"may return null database");
			fieldStoreTypeDatabase = loadPropertyDatabase(
					new FieldStoreTypeDatabase(),
					FieldStoreTypeDatabase.DEFAULT_FILENAME,
					"field store type database");
			interprocDatabasesLoaded = true;
		}
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
	 * Get the method property database containing methods which may return
	 * a null value. 
	 * 
	 * @return the database, or null if there is no database available
	 */
	public MayReturnNullPropertyDatabase getMayReturnNullDatabase() {
		return mayReturnNullDatabase;
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
			if (DEBUG) System.out.println("Loading " + description + " from " + fileName + "...");
			
			File dbFile = new File(INTERPROC_DATABASE_DIR, fileName);
			
			database.readFromFile(dbFile.getPath());
			return database;
		} catch (IOException e) {
			getLookupFailureCallback().logError("Error loading " + description, e);
		} catch (PropertyDatabaseFormatException e) {
			getLookupFailureCallback().logError("Invalid " + description, e);
		}
		
		return null;
	}
}

// vim:ts=4
