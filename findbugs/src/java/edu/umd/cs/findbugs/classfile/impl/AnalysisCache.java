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

import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.IAnalysisEngine;
import edu.umd.cs.findbugs.classfile.IClassAnalysisEngine;
import edu.umd.cs.findbugs.classfile.IClassDescriptor;
import edu.umd.cs.findbugs.classfile.IClassPath;
import edu.umd.cs.findbugs.classfile.IMethodAnalysisEngine;
import edu.umd.cs.findbugs.classfile.IMethodDescriptor;

/**
 * Implementation of IAnalysisCache.
 * This object is responsible for registering class and method analysis engines
 * and caching analysis results.
 * 
 * @author David Hovemeyer
 */
public class AnalysisCache implements IAnalysisCache {
	// TODO: think about caching policy.  Right now, cache everything forever.
	
	private IClassPath classPath;
	
	private Map<Class<?>, IClassAnalysisEngine> classAnalysisEngineMap;
	private Map<Class<?>, IMethodAnalysisEngine> methodAnalysisEngineMap;
	private Map<IClassDescriptor, Map<Class<?>, Object>> classAnalysisMap;
	private Map<IMethodDescriptor, Map<Class<?>, Object>> methodAnalysisMap;
	
	static class AnalysisError {
		CheckedAnalysisException exception;
		
		public AnalysisError(CheckedAnalysisException exception) {
			this.exception = exception;
		}
	}
	
	AnalysisCache(IClassPath classPath) {
		this.classPath = classPath;
		this.classAnalysisEngineMap = new HashMap<Class<?>, IClassAnalysisEngine>();
		this.methodAnalysisEngineMap = new HashMap<Class<?>, IMethodAnalysisEngine>();
		this.classAnalysisMap = new HashMap<IClassDescriptor, Map<Class<?>,Object>>();
		this.methodAnalysisMap = new HashMap<IMethodDescriptor, Map<Class<?>,Object>>();
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisCache#getClassPath()
	 */
	public IClassPath getClassPath() {
		return classPath;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisCache#getClassAnalysis(java.lang.Class, edu.umd.cs.findbugs.classfile.IClassDescriptor)
	 */
	public <E> E getClassAnalysis(Class<E> analysisClass,
			IClassDescriptor classDescriptor) throws CheckedAnalysisException {
		return analyzeClassOrMethod(
				this,
				classAnalysisMap,
				classAnalysisEngineMap,
				classDescriptor,
				analysisClass);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisCache#getMethodAnalysis(java.lang.Class, edu.umd.cs.findbugs.classfile.IMethodDescriptor)
	 */
	public <E> E getMethodAnalysis(Class<E> analysisClass,
			IMethodDescriptor methodDescriptor) throws CheckedAnalysisException {
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
	 * @param analysisCache                the IAnalysisCache object
	 * @param descriptorToAnalysisCacheMap cache of analysis results for this kind of descriptor
	 * @param engineMap                    engine map for this kind of descriptor
	 * @param descriptor                   the class or method descriptor
	 * @param analysisClass                the analysis result type Class object
	 * @return the analysis result object
	 * @throws CheckedAnalysisException if an analysis error occurs
	 */
	static<DescriptorType, E> E analyzeClassOrMethod(
			IAnalysisCache analysisCache,
			Map<DescriptorType, Map<Class<?>, Object>> descriptorToAnalysisCacheMap,
			Map<Class<?>, ? extends IAnalysisEngine<DescriptorType>> engineMap,
			DescriptorType descriptor,
			Class<E> analysisClass
	) throws CheckedAnalysisException {
		// Get the analysis map for the class/method descriptor
		Map<Class<?>, Object> analysisMap = descriptorToAnalysisCacheMap.get(descriptor);
		if (analysisMap == null) {
			// Create empty analysis map and save it
			analysisMap = new HashMap<Class<?>, Object>();
			descriptorToAnalysisCacheMap.put(descriptor, analysisMap);
		}
		
		// See if the analysis has already been performed
		Object analysisResult = analysisMap.get(analysisClass);
		if (analysisResult == null) {
			// Analysis hasn't been performed yet.
			// Find an appropriate analysis engine.
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
			analysisMap.put(analysisClass, analysisResult);
		}
		
		// Error occurred?
		if (analysisResult instanceof AnalysisError) {
			throw ((AnalysisError) analysisResult).exception;
		}
		
		// If we could assume a 1.5 or later JVM, the Class.cast() static
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

}
