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
package edu.umd.cs.findbugs.classfile;

import java.util.Map;

import javax.annotation.Nonnull;

/**
 * The analysis cache performs analyses on classes and methods
 * and caches the results.
 * 
 * @author David Hovemeyer
 */
public interface IAnalysisCache {

	/**
	 * Register the given class analysis engine as producing the
	 * analysis result type whose Class is given.
	 * 
	 * @param <E>                 analysis result type
	 * @param analysisResultType  analysis result type Class object 
	 * @param classAnalysisEngine the class analysis engine to register
	 */
	public <E> void registerClassAnalysisEngine(
		Class<E> analysisResultType, IClassAnalysisEngine<E> classAnalysisEngine);

	/**
	 * Register the given method analysis engine as producing the
	 * analysis result type whose Class is given.
	 * 
	 * @param <E>                  analysis result type
	 * @param analysisResultType   analysis result type Class object 
	 * @param methodAnalysisEngine the method analysis engine to register
	 */
	public <E> void registerMethodAnalysisEngine(
		Class<E> analysisResultType, IMethodAnalysisEngine<E> methodAnalysisEngine);

	/**
	 * Get an analysis of the given class.
	 * 
	 * @param <E>              the type of the analysis (e.g., FoobarAnalysis)
	 * @param analysisClass    the analysis class object (e.g., FoobarAnalysis.class)
	 * @param classDescriptor  the descriptor of the class to analyze
	 * @return                 the analysis object (e.g., instance of FoobarAnalysis for the class)
	 * @throws CheckedAnalysisException if an error occurs performing the analysis
	 */
	public <E> E getClassAnalysis(Class<E> analysisClass, @Nonnull ClassDescriptor classDescriptor)
		throws CheckedAnalysisException;

	/**
	 * See if the cache contains a cached class analysis result
	 * for given class descriptor.
	 * 
	 * @param analysisClass   analysis result class
	 * @param classDescriptor the class descriptor
	 * @return a cached analysis result, or null if there is no cached analysis result
	 */
	public <E> E probeClassAnalysis(Class<E> analysisClass, @Nonnull ClassDescriptor classDescriptor);

	/**
	 * Get an analysis of the given method.
	 * 
	 * @param <E>              the type of the analysis (e.g., FoobarAnalysis)
	 * @param analysisClass    the analysis class object (e.g., FoobarAnalysis.class)
	 * @param methodDescriptor the descriptor of the method to analyze
	 * @return                 the analysis object (e.g., instance of FoobarAnalysis for the method)
	 * @throws CheckedAnalysisException if an error occurs performing the analysis
	 */
	public <E> E getMethodAnalysis(Class<E> analysisClass, @Nonnull MethodDescriptor methodDescriptor)
		throws CheckedAnalysisException;

	/**
	 * Eagerly put a method analysis object in the cache.
	 * This can be necessary if an method analysis engine invokes other
	 * analysis engines that might recursively require the analysis
	 * being produced.
	 * 
	 * @param <E>              the type of the analysis (e.g., FoobarAnalysis)
	 * @param analysisClass    the analysis class object (e.g., FoobarAnalysis.class)
	 * @param methodDescriptor the descriptor of the method to analyze
	 * @param analysisObject
	 */
	public <E> void eagerlyPutMethodAnalysis(Class<E> analysisClass, @Nonnull MethodDescriptor methodDescriptor, Object analysisObject);

	/**
	 * Purge all analysis results for given method.
	 * This can be called when a CFG is pruned and we want to
	 * compute more accurate analysis results on the new CFG.
	 * 
	 * @param methodDescriptor method whose analysis results should be purged
	 */
	public void purgeMethodAnalyses(@Nonnull MethodDescriptor methodDescriptor);

	/**
	 * Purge all analysis results for all methods.
	 */
	public void purgeAllMethodAnalysis();

	/**
	 * Register a database factory.
	 * 
	 * @param <E>             type of database
	 * @param databaseClass   Class of database
	 * @param databaseFactory the database factory
	 */
	public <E> void registerDatabaseFactory(Class<E> databaseClass, IDatabaseFactory<E> databaseFactory);

	/**
	 * Get a database.
	 * 
	 * @param <E>           type of database
	 * @param databaseClass Class of database
	 * @return the database (which is created by a database factory if required)
	 * @throws CheckedAnalysisException 
	 */
	public <E> E getDatabase(Class<E> databaseClass) throws CheckedAnalysisException;

	/**
	 * Eagerly install a database.
	 * This avoids the need to register a database factory.
	 * 
	 * @param <E>           type of database
	 * @param databaseClass Class of database
	 * @param database      database object
	 */
	public <E> void eagerlyPutDatabase(Class<E> databaseClass, E database);

	/**
	 * Get the classpath from which classes are loaded. 
	 * 
	 * @return the classpath
	 */
	public IClassPath getClassPath();

	/**
	 * Get the error logger.
	 * 
	 * @return the error logger
	 */
	public IErrorLogger getErrorLogger();

	/**
	 * Get map of analysis-local objects.
	 */
	public Map<?, ?> getAnalysisLocals();
}
