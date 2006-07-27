/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2006, University of Maryland
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

import java.io.IOException;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.ba.ch.Subtypes;
import edu.umd.cs.findbugs.ba.npe.ParameterNullnessPropertyDatabase;
import edu.umd.cs.findbugs.ba.type.FieldStoreTypeDatabase;
import edu.umd.cs.findbugs.util.MapCache;

/**
 * Original implementation of AnalysisContext.
 * (Eventual goal is a new implementation that uses
 * the IAnalysisCache.)
 * 
 * @author David Hovemeyer
 */
public class LegacyAnalysisContext extends AnalysisContext {
	private RepositoryLookupFailureCallback lookupFailureCallback;
	private SourceFinder sourceFinder;
	private MapCache<JavaClass, ClassContext> classContextCache;
	private Subtypes subtypes;
	private Map<Object,Object> analysisLocals = 
		Collections.synchronizedMap(new HashMap<Object,Object>());
	private BitSet boolPropertySet;
	private final SourceInfoMap sourceInfoMap;
	private InnerClassAccessMap innerClassAccessMap;
	
	// Interprocedural fact databases
	private String databaseInputDir;
	private String databaseOutputDir;
	// private MayReturnNullPropertyDatabase mayReturnNullDatabase;
	private FieldStoreTypeDatabase fieldStoreTypeDatabase;
	private ParameterNullnessPropertyDatabase unconditionalDerefParamDatabase;
	
	
	private NullnessAnnotationDatabase nullnessAnnotationDatabase = new NullnessAnnotationDatabase();
	
	@Override
	public NullnessAnnotationDatabase getNullnessAnnotationDatabase() {
		return nullnessAnnotationDatabase;
	}

	private CheckReturnAnnotationDatabase checkReturnAnnotationDatabase;

	@Override
	public CheckReturnAnnotationDatabase getCheckReturnAnnotationDatabase() {
		return checkReturnAnnotationDatabase;
	}
	
	private AnnotationRetentionDatabase annotationRetentionDatabase;

	@Override
	public AnnotationRetentionDatabase getAnnotationRetentionDatabase() {
		return annotationRetentionDatabase;
	}
	
	private JCIPAnnotationDatabase jcipAnnotationDatabase;

	@Override
	public JCIPAnnotationDatabase getJCIPAnnotationDatabase() {
		return jcipAnnotationDatabase;
	}

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

	LegacyAnalysisContext(RepositoryLookupFailureCallback lookupFailureCallback) {
		this.lookupFailureCallback = lookupFailureCallback;
		this.sourceFinder = new SourceFinder();
		this.subtypes = new Subtypes();
		this.boolPropertySet = new BitSet();
		this.sourceInfoMap = new SourceInfoMap();
		
		if (originalRepository instanceof URLClassPathRepository) {
			getLookupFailureCallback().logError(
				"originalRepository is a URLClassPathRepository, which may cause problems");
		}

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

	@Override
	public void initDatabases() {
		checkReturnAnnotationDatabase = new CheckReturnAnnotationDatabase();
		annotationRetentionDatabase = new AnnotationRetentionDatabase();
		jcipAnnotationDatabase = new JCIPAnnotationDatabase();
	}

	@Override
	public RepositoryLookupFailureCallback getLookupFailureCallback() {
		return lookupFailureCallback;
	}

	@Override
	public void setSourcePath(List<String> sourcePath) {
		sourceFinder.setSourceBaseList(sourcePath);
	}

	@Override
	public SourceFinder getSourceFinder() {
		return sourceFinder;
	}
	
	@Override
	public Subtypes getSubtypes() {
		return subtypes;
	}
	
	@Override
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
		getInnerClassAccessMap().clearCache();

		// Create a URLClassPathRepository and make it current.
		URLClassPathRepository repository = new URLClassPathRepository(); 
		Repository.setRepository(repository);
	}
	
	@Override
	public void clearClassContextCache() {
		if (classContextCache != null)
			classContextCache.clear();
	}
	
	@Override
	public void addClasspathEntry(String url) throws IOException {
		URLClassPathRepository repos = (URLClassPathRepository) Repository.getRepository();
		repos.addURL(url);
	}
	
	@Override
	public void addApplicationClassToRepository(JavaClass appClass) {
		Repository.addClass(appClass);
		subtypes.addApplicationClass(appClass);
	}

	@Override
	public boolean isApplicationClass(JavaClass cls) {
		return subtypes.isApplicationClass(cls);
	}
	
	@Override
	public boolean isApplicationClass(String className) {
		try {
			JavaClass javaClass = lookupClass(className);
			return isApplicationClass(javaClass);
		} catch (ClassNotFoundException e) {
			AnalysisContext.reportMissingClass(e);
			return false;
		}
	}
	
	@Override
	public JavaClass lookupClass(@NonNull String className) throws ClassNotFoundException {
		// TODO: eventually we should move to our own thread-safe repository implementation
		if (className == null) throw new IllegalArgumentException("className is null");
		return Repository.lookupClass(className);
		// note: previous line does not throw ClassNotFoundException, instead returns null
	}

	@Override
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

	@Override
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

	@Override
	public String getClassContextStats() {
		if (classContextCache == null) return null;
		return hits + "/" + misses + ":" + classContextCache.getStatistics();
	}

	@Override
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
				getUnconditionalDerefParamDatabase(),
				UNCONDITIONAL_DEREF_DB_FILENAME,
				"unconditional param deref database");
	}

	@Override
	public void loadDefaultInterproceduralDatabases() {

		unconditionalDerefParamDatabase = loadPropertyDatabaseFromResource(
				getUnconditionalDerefParamDatabase(),
				UNCONDITIONAL_DEREF_DB_RESOURCE,
				"unconditional param deref database");
	}
	
	@Override
	public void setBoolProperty(int prop, boolean value) {
		boolPropertySet.set(prop, value);
	}
	
	@Override
	public boolean getBoolProperty(int prop) {
		return boolPropertySet.get(prop);
	}

	@Override
	public SourceInfoMap getSourceInfoMap() {
		return sourceInfoMap;
	}

	@Override
	public void setDatabaseInputDir(String databaseInputDir) {
		if (DEBUG) System.out.println("Setting database input directory: " + databaseInputDir);
		this.databaseInputDir = databaseInputDir;
	}
	
	@Override
	public String getDatabaseInputDir() {
		return databaseInputDir;
	}

	@Override
	public void setDatabaseOutputDir(String databaseOutputDir) {
		if (DEBUG) System.out.println("Setting database output directory: " + databaseOutputDir);
		this.databaseOutputDir = databaseOutputDir;
	}
	
	@Override
	public String getDatabaseOutputDir() {
		return databaseOutputDir;
	}

	@Override
	public FieldStoreTypeDatabase getFieldStoreTypeDatabase() {
		return fieldStoreTypeDatabase;
	}

	@Override
	public ParameterNullnessPropertyDatabase getUnconditionalDerefParamDatabase() {
		if (unconditionalDerefParamDatabase == null) {
			unconditionalDerefParamDatabase = new ParameterNullnessPropertyDatabase();
		}
		return unconditionalDerefParamDatabase;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#getAnalyisLocals()
	 */
	@Override
	public Map<Object, Object> getAnalysisLocals() {
		return analysisLocals;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#getInnerClassAccessMap()
	 */
	@Override
	public InnerClassAccessMap getInnerClassAccessMap() {
		if (innerClassAccessMap == null) {
			innerClassAccessMap = InnerClassAccessMap.create();
		}
		return innerClassAccessMap;
	}
}
