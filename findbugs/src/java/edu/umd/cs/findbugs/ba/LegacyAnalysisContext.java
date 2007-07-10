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

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.ba.ch.Subtypes;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.ba.npe.ParameterNullnessPropertyDatabase;
import edu.umd.cs.findbugs.ba.npe.ReturnValueNullnessPropertyDatabase;
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
	private final SourceInfoMap sourceInfoMap;
	private InnerClassAccessMap innerClassAccessMap;

	// Interprocedural fact databases
	// private MayReturnNullPropertyDatabase mayReturnNullDatabase;
	private FieldStoreTypeDatabase fieldStoreTypeDatabase;
	private ParameterNullnessPropertyDatabase unconditionalDerefParamDatabase;
	private ReturnValueNullnessPropertyDatabase  returnValueNullnessDatabase;

	private NullnessAnnotationDatabase nullnessAnnotationDatabase; //= new NullnessAnnotationDatabase();

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
	private static final int DEFAULT_CACHE_SIZE = 6;

	/**
	 * Constructor.
	 * 
	 * @param lookupFailureCallback the RepositoryLookupFailureCallback to use when
	 *                               reporting errors and class lookup failures
	 */
	LegacyAnalysisContext(RepositoryLookupFailureCallback lookupFailureCallback) {
		this.lookupFailureCallback = lookupFailureCallback;
		this.sourceFinder = new SourceFinder();
		this.subtypes = new Subtypes();
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
		nullnessAnnotationDatabase = new NullnessAnnotationDatabase();
	}


	@Override
	public void updateDatabases(int pass) {
		if (pass == 0) {
			checkReturnAnnotationDatabase.loadAuxiliaryAnnotations();
			nullnessAnnotationDatabase.loadAuxiliaryAnnotations();
		}
	}
	@Override
	public RepositoryLookupFailureCallback getLookupFailureCallback() {
		return lookupFailureCallback;
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
	public JavaClass lookupClass(@NonNull String className) throws ClassNotFoundException {
		// TODO: eventually we should move to our own thread-safe repository implementation
		if (className == null) throw new IllegalArgumentException("className is null");
		return Repository.lookupClass(className);
		// note: previous line does not throw ClassNotFoundException, instead returns null
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
		int cacheSize = getBoolProperty(AnalysisFeatures.CONSERVE_SPACE) ? 3 : DEFAULT_CACHE_SIZE;
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
	public SourceInfoMap getSourceInfoMap() {
		return sourceInfoMap;
	}

	@Override
	public FieldStoreTypeDatabase getFieldStoreTypeDatabase() {
		if (fieldStoreTypeDatabase == null) {
			fieldStoreTypeDatabase = new FieldStoreTypeDatabase();
		}
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
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#getInnerClassAccessMap()
	 */
	@Override
	public InnerClassAccessMap getInnerClassAccessMap() {
		if (innerClassAccessMap == null) {
			innerClassAccessMap = InnerClassAccessMap.create();
		}
		return innerClassAccessMap;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#getReturnValueNullnessPropertyDatabase()
	 */
	@Override
	public ReturnValueNullnessPropertyDatabase getReturnValueNullnessPropertyDatabase() {
		if (returnValueNullnessDatabase  == null) {
			returnValueNullnessDatabase = new ReturnValueNullnessPropertyDatabase();
		}
		return returnValueNullnessDatabase;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#getSyntheticElements()
	 */
	@Override
	public SyntheticElements getSyntheticElements() {
		throw new UnsupportedOperationException();
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#getSubtypes2()
	 */
	@Override
	public Subtypes2 getSubtypes2() {
		throw new UnsupportedOperationException();
	}

}
