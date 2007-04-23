/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
	 * Maximum number of class or method analysis results
	 * to cache for a particular ClassDescriptor/MethodDescriptor.
	 */
	private static final int CACHE_SIZE = 5;

	// Fields
	private IClassPath classPath;
	private IErrorLogger errorLogger;
	private Map<Class<?>, IClassAnalysisEngine> classAnalysisEngineMap;
	private Map<Class<?>, IMethodAnalysisEngine> methodAnalysisEngineMap;
	private Map<Class<?>, IDatabaseFactory<?>> databaseFactoryMap;
	private Map<Class<?>, Map<ClassDescriptor, Object>> classAnalysisMap;
	private Map<Class<?>, Map<MethodDescriptor, Object>> methodAnalysisMap;
	private Map<Class<?>, Object> databaseMap;

	/**
	 * Object indicating that an analysis could not be computed
	 * because an exception occurred.
	 */
	static class AnalysisError {
		CheckedAnalysisException exception;

		public AnalysisError(CheckedAnalysisException exception) {
			this.exception = exception;
		}
	}

	/**
	 * Constructor.
	 * 
	 * @param classPath    the IClassPath to load resources from
	 * @param errorLogger  the IErrorLogger
	 */
	AnalysisCache(IClassPath classPath, IErrorLogger errorLogger) {
		this.classPath = classPath;
		this.errorLogger = errorLogger;
		this.classAnalysisEngineMap = new HashMap<Class<?>, IClassAnalysisEngine>();
		this.methodAnalysisEngineMap = new HashMap<Class<?>, IMethodAnalysisEngine>();
		this.databaseFactoryMap = new HashMap<Class<?>, IDatabaseFactory<?>>();

		this.classAnalysisMap = new HashMap<Class<?>, Map<ClassDescriptor,Object>>();
		this.methodAnalysisMap = new HashMap<Class<?>, Map<MethodDescriptor,Object>>();

		this.databaseMap = new HashMap<Class<?>, Object>();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisCache#getClassPath()
	 */
	public IClassPath getClassPath() {
		return classPath;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisCache#getClassAnalysis(java.lang.Class, edu.umd.cs.findbugs.classfile.ClassDescriptor)
	 */
	public <E> E getClassAnalysis(Class<E> analysisClass,
			ClassDescriptor classDescriptor) throws CheckedAnalysisException {
		return analyzeClassOrMethod(
				this,
				classAnalysisMap,
				classAnalysisEngineMap,
				classDescriptor,
				analysisClass);
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

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisCache#getMethodAnalysis(java.lang.Class, edu.umd.cs.findbugs.classfile.MethodDescriptor)
	 */
	public <E> E getMethodAnalysis(Class<E> analysisClass,
			MethodDescriptor methodDescriptor) throws CheckedAnalysisException {
		return analyzeClassOrMethod(
				this,
				methodAnalysisMap,
				methodAnalysisEngineMap,
				methodDescriptor,
				analysisClass);
	}

	/**
	 * Analyze a class or method,
	 * or get the cached analysis result.
	 * 
	 * @param <DescriptorType> type of descriptor (class or method)
	 * @param <E> type of analysis result
	 * @param analysisCache                   the IAnalysisCache object
	 * @param analysisClassToDescriptorMapMap the map of analysis classes to descriptor->result maps
	 * @param engineMap                       engine map for this kind of descriptor
	 * @param descriptor                      the class or method descriptor
	 * @param analysisClass                   the analysis result type Class object
	 * @return the analysis result object
	 * @throws CheckedAnalysisException if an analysis error occurs
	 */
	static<DescriptorType, E> E analyzeClassOrMethod(
			final AnalysisCache analysisCache,
			final Map<Class<?>, Map<DescriptorType, Object>> analysisClassToDescriptorMapMap,
			final Map<Class<?>, ? extends IAnalysisEngine<DescriptorType>> engineMap,
			final DescriptorType descriptor,
			final Class<E> analysisClass
	) throws CheckedAnalysisException {

		// Get the descriptor->result map for this analysis class,
		// creating if necessary
		Map<DescriptorType, Object> descriptorMap = analysisClassToDescriptorMapMap.get(analysisClass);
		if (descriptorMap == null) {
			// Create a MapCache that allows the analysis engine to
			// decide that analysis results should be retained indefinitely.
			descriptorMap = new MapCache<DescriptorType, Object>(CACHE_SIZE) {
				IAnalysisEngine<DescriptorType> engine = engineMap.get(analysisClass);

				/* (non-Javadoc)
				 * @see edu.umd.cs.findbugs.util.MapCache#removeEldestEntry(java.util.Map.Entry)
				 */
				@Override
				protected boolean removeEldestEntry(Entry<DescriptorType, Object> eldest) {
					if (engine.retainAnalysisResults()) {
						return false;
					} else {
						return super.removeEldestEntry(eldest);
					}
				}
			};
			analysisClassToDescriptorMapMap.put(analysisClass, descriptorMap);
		}

		// See if there is a cached result in the descriptor map
		Object analysisResult = descriptorMap.get(descriptor);
		if (analysisResult == null) {
			// No cached result - compute (or recompute)
			IAnalysisEngine<DescriptorType> engine = engineMap.get(analysisClass);
			if (engine == null) {
				throw new IllegalArgumentException(
						"No analysis engine registered to produce " + analysisClass.getName());
			}

			// Perform the analysis
			try {
				analysisResult = engine.analyze(analysisCache, descriptor);
			} catch (CheckedAnalysisException e) {
				// Whoops, an error occurred when performing the analysis.
				// Make a note.
				analysisResult = new AnalysisError(e);
			}

			// Save the result
			descriptorMap.put(descriptor, analysisResult);
		}

		// Error occurred?
		if (analysisResult instanceof AnalysisError) {
			throw ((AnalysisError) analysisResult).exception;
		}

		// If we could assume a 1.5 or later JVM, the Class.cast()
		// method could do this cast without a warning.
		return (E) analysisResult;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisCache#registerClassAnalysisEngine(java.lang.Class, edu.umd.cs.findbugs.classfile.IClassAnalysisEngine)
	 */
	public <E> void registerClassAnalysisEngine(Class<E> analysisResultType,
			IClassAnalysisEngine classAnalysisEngine) {
		classAnalysisEngineMap.put(analysisResultType, classAnalysisEngine);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisCache#registerMethodAnalysisEngine(java.lang.Class, edu.umd.cs.findbugs.classfile.IMethodAnalysisEngine)
	 */
	public <E> void registerMethodAnalysisEngine(Class<E> analysisResultType,
			IMethodAnalysisEngine methodAnalysisEngine) {
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
				database = new AnalysisError(e);
			}

			databaseMap.put(databaseClass, database);
		}

		if (database instanceof AnalysisError) {
			throw ((AnalysisError)database).exception;
		}

		// Again, we really should be using Class.cast()
		return (E) database;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisCache#getErrorLogger()
	 */
	public IErrorLogger getErrorLogger() {
		return errorLogger;
	}
}
