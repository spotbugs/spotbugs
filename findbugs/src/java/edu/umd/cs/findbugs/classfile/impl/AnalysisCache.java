/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006-2007 University of Maryland
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

package edu.umd.cs.findbugs.classfile.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Debug;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.IAnalysisEngine;
import edu.umd.cs.findbugs.classfile.IClassAnalysisEngine;
import edu.umd.cs.findbugs.classfile.IClassPath;
import edu.umd.cs.findbugs.classfile.IDatabaseFactory;
import edu.umd.cs.findbugs.classfile.IErrorLogger;
import edu.umd.cs.findbugs.classfile.IMethodAnalysisEngine;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.log.Profiler;
import edu.umd.cs.findbugs.util.MapCache;

/**
 * Implementation of IAnalysisCache.
 * This object is responsible for registering class and method analysis engines
 * and caching analysis results.
 * 
 * @author David Hovemeyer
 */
public class AnalysisCache implements IAnalysisCache {
	/**
     * 
     */
    private static final int MAX_JAVACLASS_RESULTS_TO_CACHE = 5000;

	/**
	 * Maximum number of class analysis results to cache.
	 */
	private static final int MAX_CLASS_RESULTS_TO_CACHE = 5000;

	// Fields
	private IClassPath classPath;
	private IErrorLogger errorLogger;
	private Map<Class<?>, IClassAnalysisEngine<?>> classAnalysisEngineMap;
	private Map<Class<?>, IMethodAnalysisEngine<?>> methodAnalysisEngineMap;
	private Map<Class<?>, IDatabaseFactory<?>> databaseFactoryMap;
	private Map<Class<?>, Map<ClassDescriptor, Object>> classAnalysisMap;
	private Map<Class<?>, Object> databaseMap;

	static class AbnormalAnalysisResult {
		final CheckedAnalysisException checkedAnalysisException;
		final RuntimeException runtimeException;
		final boolean isNull;
		
		AbnormalAnalysisResult(CheckedAnalysisException checkedAnalysisException) {
			this.checkedAnalysisException = checkedAnalysisException;
			this.runtimeException = null;
			isNull = false;
		}
		
		AbnormalAnalysisResult(RuntimeException runtimeException) {
			this.runtimeException = runtimeException;
			this.checkedAnalysisException = null;
			isNull = false;
		}
		
		AbnormalAnalysisResult() {
			this.isNull = true;
			this.checkedAnalysisException = null;
			this.runtimeException = null;
		}

		public Object returnOrThrow() throws CheckedAnalysisException {
			if (isNull) {
				return null;
			} else if (runtimeException != null) {
				runtimeException.fillInStackTrace();
				throw runtimeException;
			} else if (checkedAnalysisException != null) {
				checkedAnalysisException.fillInStackTrace();
				throw checkedAnalysisException;
			}
		
		throw new IllegalStateException("It has to be something");
		}
	}
	
	static final AbnormalAnalysisResult NULL_ANALYSIS_RESULT = new AbnormalAnalysisResult();

	/**
	 * Constructor.
	 * 
	 * @param classPath    the IClassPath to load resources from
	 * @param errorLogger  the IErrorLogger
	 */
	AnalysisCache(IClassPath classPath, IErrorLogger errorLogger) {
		this.classPath = classPath;
		this.errorLogger = errorLogger;
		this.classAnalysisEngineMap = new HashMap<Class<?>, IClassAnalysisEngine<?>>();
		this.methodAnalysisEngineMap = new HashMap<Class<?>, IMethodAnalysisEngine<?>>();
		this.databaseFactoryMap = new HashMap<Class<?>, IDatabaseFactory<?>>();
		this.classAnalysisMap = new HashMap<Class<?>, Map<ClassDescriptor,Object>>();
		this.databaseMap = new HashMap<Class<?>, Object>();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisCache#getClassPath()
	 */
	public IClassPath getClassPath() {
		return classPath;
	}

	public void purgeAllMethodAnalysis() {
		// System.out.println("ZZZ : purging all method analyses");
		
		try {
			Map<ClassDescriptor, ClassContext> map = getAllClassAnalysis(ClassContext.class);
			Collection<?> allClassContexts = map.values();
			for(Object c : allClassContexts) {
				if (c instanceof ClassContext) 
					((ClassContext)c).purgeAllMethodAnalyses();
			}
		}
		catch (ClassCastException e) {
			AnalysisContext.logError("Unable to purge method analysis" , e);
		} catch (CheckedAnalysisException e) {
			AnalysisContext.logError("Unable to purge method analysis" , e);
		}
	}
	private  <E> Map<ClassDescriptor, E> getAllClassAnalysis(Class<E> analysisClass) 
	throws CheckedAnalysisException {
		Map<ClassDescriptor, Object> descriptorMap =
			findOrCreateDescriptorMap(classAnalysisMap, (Map)classAnalysisEngineMap, analysisClass);
		return (Map<ClassDescriptor, E>) descriptorMap;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisCache#getClassAnalysis(java.lang.Class, edu.umd.cs.findbugs.classfile.ClassDescriptor)
	 */
	public <E> E getClassAnalysis(Class<E> analysisClass,
			ClassDescriptor classDescriptor) throws CheckedAnalysisException {
		if (classDescriptor == null)
			throw new NullPointerException("classDescriptor is null");
		// Get the descriptor->result map for this analysis class,
		// creating if necessary
		Map<ClassDescriptor, Object> descriptorMap =
			findOrCreateDescriptorMap(classAnalysisMap, (Map)classAnalysisEngineMap, analysisClass);

		// See if there is a cached result in the descriptor map
		Object analysisResult = descriptorMap.get(classDescriptor);
		if (analysisResult == null) {
			// No cached result - compute (or recompute)
			if (false && analysisClass == ClassContext.class)
				System.out.println("ZZZ Generating " + analysisClass.getSimpleName() + " for " + classDescriptor);
			IAnalysisEngine<ClassDescriptor, E> engine = (IAnalysisEngine<ClassDescriptor, E>) classAnalysisEngineMap.get(analysisClass);
			if (engine == null) {
				throw new IllegalArgumentException(
						"No analysis engine registered to produce " + analysisClass.getName());
			}
			Profiler profiler = Profiler.getInstance();
			// Perform the analysis
			try {
				profiler.start(engine.getClass());
				analysisResult = engine.analyze(this, classDescriptor);

				// If engine returned null, we need to construct
				// an AbnormalAnalysisResult object to record that fact.
				// Otherwise we will try to recompute the value in
				// the future.
				if (analysisResult == null) {
					analysisResult = NULL_ANALYSIS_RESULT;
				}
			} catch (CheckedAnalysisException e) {
				// Exception - make note
				analysisResult = new AbnormalAnalysisResult(e);
			} catch (RuntimeException e) {
				// Exception - make note
				analysisResult = new AbnormalAnalysisResult(e);
			} finally {
				profiler.end(engine.getClass());
			}

			if (false && analysisClass == ClassContext.class)
				System.out.println("ZZZ Generated " + hex(analysisResult) + " for " + classDescriptor);
		
			// Save the result
			descriptorMap.put(classDescriptor, analysisResult);
		}

		// Abnormal analysis result?
		if (analysisResult instanceof AbnormalAnalysisResult) {
			return (E) ((AbnormalAnalysisResult) analysisResult).returnOrThrow();
		}

		return (E) analysisResult;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisCache#probeClassAnalysis(java.lang.Class, edu.umd.cs.findbugs.classfile.ClassDescriptor)
	 */
	public <E> E probeClassAnalysis(Class<E> analysisClass, ClassDescriptor classDescriptor) {
		Map<ClassDescriptor, Object> descriptorMap = classAnalysisMap.get(analysisClass);
		if (descriptorMap == null) {
			return null;
		}
		return (E) descriptorMap.get(classDescriptor);
	}

	String hex(Object o) {
		return Integer.toHexString(System.identityHashCode(o));
	}
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisCache#getMethodAnalysis(java.lang.Class, edu.umd.cs.findbugs.classfile.MethodDescriptor)
	 */
	public <E> E getMethodAnalysis(Class<E> analysisClass,
			MethodDescriptor methodDescriptor) throws CheckedAnalysisException {
		if (methodDescriptor == null)
			throw new NullPointerException("methodDescriptor is null");
		ClassContext classContext = getClassAnalysis(ClassContext.class, methodDescriptor.getClassDescriptor());
		Object object = classContext.getMethodAnalysis(analysisClass, methodDescriptor);

		if (object == null) {
			try {
				object = analyzeMethod(classContext, analysisClass, methodDescriptor);
				if (object == null) {
					object = NULL_ANALYSIS_RESULT;
				}
			} catch (RuntimeException e) {
				object = new AbnormalAnalysisResult(e);
			} catch (CheckedAnalysisException e) {
				object = new AbnormalAnalysisResult(e);
			}
			
			classContext.putMethodAnalysis(analysisClass, methodDescriptor, object);
			if (false) System.out.println("ZZZ updated to " + classContext.getObjectMap(analysisClass).keySet() 
					+ " in " + Integer.toString(System.identityHashCode(classContext.getObjectMap(analysisClass)),16));
			
		}
		if (Debug.VERIFY_INTEGRITY && object == null) {
			throw new IllegalStateException("AnalysisFactory failed to produce a result object");
		}

		if (object instanceof AbnormalAnalysisResult) {
			return (E) ((AbnormalAnalysisResult) object).returnOrThrow();
		}
		
		return analysisClass.cast(object);
	}
	
	/**
	 * Analyze a method.
	 * 
     * @param classContext     ClassContext storing method analysis objects for method's class
     * @param analysisClass    class the method analysis object should belong to
     * @param methodDescriptor method descriptor identifying the method to analyze
     * @return the computed analysis object for the method
	 * @throws CheckedAnalysisException 
     */
    private <E> E analyzeMethod(
    		ClassContext classContext,
    		Class<E> analysisClass,
    		MethodDescriptor methodDescriptor) throws CheckedAnalysisException {
    	IMethodAnalysisEngine<E> engine = (IMethodAnalysisEngine<E>) methodAnalysisEngineMap.get(analysisClass);
    	if (engine == null) {
    		throw new IllegalArgumentException(
					"No analysis engine registered to produce " + analysisClass.getName());
    	}
    	Profiler profiler = Profiler.getInstance();
    	profiler.start(engine.getClass());
    	try {
    	return engine.analyze(this, methodDescriptor);
    	} finally {
    		profiler.end(engine.getClass());
    	}
    }

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisCache#eagerlyPutMethodAnalysis(java.lang.Class, edu.umd.cs.findbugs.classfile.MethodDescriptor, java.lang.Object)
	 */
	public <E> void eagerlyPutMethodAnalysis(Class<E> analysisClass, MethodDescriptor methodDescriptor, Object analysisObject) {
		try {
	        ClassContext classContext = getClassAnalysis(ClassContext.class, methodDescriptor.getClassDescriptor());
	        classContext.putMethodAnalysis(analysisClass, methodDescriptor, analysisObject);
        } catch (CheckedAnalysisException e) {
        	IllegalStateException ise = new IllegalStateException("Unexpected exception adding method analysis to cache");
        	ise.initCause(e);
        	throw ise;
        }
		
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisCache#purgeMethodAnalyses(edu.umd.cs.findbugs.classfile.MethodDescriptor)
	 */
	public void purgeMethodAnalyses(MethodDescriptor methodDescriptor) {
		try {
			if (false) System.out.println("ZZZ purging analysis for " + methodDescriptor);
	        ClassContext classContext = getClassAnalysis(ClassContext.class, methodDescriptor.getClassDescriptor());
	        classContext.purgeMethodAnalyses(methodDescriptor);
        } catch (CheckedAnalysisException e) {
        	IllegalStateException ise = new IllegalStateException("Unexpected exception purging method analyses from cache");
        	ise.initCause(e);
        	throw ise;
        }
	}

	/**
	 * Find or create a descriptor to analysis object map.
	 * 
     * @param <DescriptorType> type of descriptor used as the map's key type (ClassDescriptor or MethodDescriptor)
     * @param <E> type of analysis class
     * @param analysisClassToDescriptorMapMap analysis class to descriptor map map
     * @param engineMap                       analysis class to analysis engine map
     * @param analysisClass                   the analysis map
     * @return the descriptor to analysis object map
     */
    private static <DescriptorType, E> Map<DescriptorType, Object> 
    findOrCreateDescriptorMap(final Map<Class<?>, Map<DescriptorType, Object>> analysisClassToDescriptorMapMap, 
    		                  final Map<Class<?>, ? extends IAnalysisEngine<DescriptorType,E>> engineMap, 
    		                  final Class<E> analysisClass) {
	    Map<DescriptorType, Object> descriptorMap = analysisClassToDescriptorMapMap.get(analysisClass);
		if (descriptorMap == null) {
			// Create a MapCache that allows the analysis engine to
			// decide that analysis results should be retained indefinitely.
			IAnalysisEngine<DescriptorType, E> engine = engineMap.get(analysisClass);
			if (analysisClass.equals(JavaClass.class))
				descriptorMap = new MapCache<DescriptorType, Object>(MAX_JAVACLASS_RESULTS_TO_CACHE);
			else if (analysisClass.equals(ClassContext.class))
				descriptorMap = new MapCache<DescriptorType, Object>(10);
			else if (engine instanceof IClassAnalysisEngine && ((IClassAnalysisEngine)engine).canRecompute()) 
				descriptorMap = new MapCache<DescriptorType, Object>(MAX_CLASS_RESULTS_TO_CACHE);
			else 
				descriptorMap = new HashMap<DescriptorType, Object>();
			
			analysisClassToDescriptorMapMap.put(analysisClass, descriptorMap);
		}
	    return descriptorMap;
    }

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisCache#registerClassAnalysisEngine(java.lang.Class, edu.umd.cs.findbugs.classfile.IClassAnalysisEngine)
	 */
	public <E> void registerClassAnalysisEngine(Class<E> analysisResultType,
			IClassAnalysisEngine<E> classAnalysisEngine) {
		classAnalysisEngineMap.put(analysisResultType, classAnalysisEngine);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisCache#registerMethodAnalysisEngine(java.lang.Class, edu.umd.cs.findbugs.classfile.IMethodAnalysisEngine)
	 */
	public <E> void registerMethodAnalysisEngine(Class<E> analysisResultType,
			IMethodAnalysisEngine<E> methodAnalysisEngine) {
		methodAnalysisEngineMap.put(analysisResultType, methodAnalysisEngine);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisCache#registerDatabaseFactory(java.lang.Class, edu.umd.cs.findbugs.classfile.IDatabaseFactory)
	 */
	public <E> void registerDatabaseFactory(Class<E> databaseClass, IDatabaseFactory<E> databaseFactory) {
		databaseFactoryMap.put(databaseClass, databaseFactory);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisCache#getDatabase(java.lang.Class)
	 */
	public <E> E getDatabase(Class<E> databaseClass) throws CheckedAnalysisException {
		Object database = databaseMap.get(databaseClass);

		if (database == null) {
			try {
				// Find the database factory
				IDatabaseFactory<?> databaseFactory = databaseFactoryMap.get(databaseClass);
				if (databaseFactory == null) {
					throw new IllegalArgumentException(
							"No database factory registered for " + databaseClass.getName());
				}

				// Create the database
				database = databaseFactory.createDatabase();
			} catch (CheckedAnalysisException e) {
				// Error - record the analysis error
				database = new AbnormalAnalysisResult(e);
			}
			// FIXME: should catch and re-throw RuntimeExceptions?

			databaseMap.put(databaseClass, database);
		}

		if (database instanceof AbnormalAnalysisResult) {
			throw ((AbnormalAnalysisResult)database).checkedAnalysisException;
		}

		// Again, we really should be using Class.cast()
		return (E) database;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisCache#eagerlyPutDatabase(java.lang.Class, java.lang.Object)
	 */
	public <E> void eagerlyPutDatabase(Class<E> databaseClass, E database) {
		databaseMap.put(databaseClass, database);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisCache#getErrorLogger()
	 */
	public IErrorLogger getErrorLogger() {
		return errorLogger;
	}
}
