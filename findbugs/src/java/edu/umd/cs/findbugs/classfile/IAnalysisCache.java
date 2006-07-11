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

package edu.umd.cs.findbugs.classfile;

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
	public<E> void registerClassAnalysisEngine(
			Class<E> analysisResultType, IClassAnalysisEngine classAnalysisEngine);

	/**
	 * Register the given method analysis engine as producing the
	 * analysis result type whose Class is given.
	 * 
	 * @param <E>                  analysis result type
	 * @param analysisResultType   analysis result type Class object 
	 * @param methodAnalysisEngine the method analysis engine to register
	 */
	public<E> void registerMethodAnalysisEngine(
			Class<E> analysisResultType, IMethodAnalysisEngine methodAnalysisEngine);

	/**
	 * Get an analysis of the given class.
	 * 
	 * @param <E>              the type of the analysis (e.g., FoobarAnalysis)
	 * @param analysisClass    the analysis class object (e.g., FoobarAnalysis.class)
	 * @param classDescriptor  the descriptor of the class to analyze
	 * @return                 the analysis object (e.g., instance of FoobarAnalysis for the class)
	 * @throws CheckedAnalysisException if an error occurs performing the analysis
	 */
	public<E> E getClassAnalysis(Class<E> analysisClass, ClassDescriptor classDescriptor)
		throws CheckedAnalysisException;

	/**
	 * Get an analysis of the given method.
	 * 
	 * @param <E>              the type of the analysis (e.g., FoobarAnalysis)
	 * @param analysisClass    the analysis class object (e.g., FoobarAnalysis.class)
	 * @param methodDescriptor the descriptor of the method to analyze
	 * @return                 the analysis object (e.g., instance of FoobarAnalysis for the method)
	 * @throws CheckedAnalysisException if an error occurs performing the analysis
	 */
	public<E> E getMethodAnalysis(Class<E> analysisClass, MethodDescriptor methodDescriptor)
		throws CheckedAnalysisException;

	/**
	 * Register a database factory.
	 * 
	 * @param <E>             type of database
	 * @param databaseClass   Class of database
	 * @param databaseFactory the database factory
	 */
	public<E> void registerDatabaseFactory(Class<E> databaseClass, IDatabaseFactory<E> databaseFactory);
	
	/**
	 * Get a database.
	 * 
	 * @param <E>           type of database
	 * @param databaseClass Class of database
	 * @return the database (which is created by a database factory if required)
	 * @throws CheckedAnalysisException 
	 */
	public<E> E getDatabase(Class<E> databaseClass) throws CheckedAnalysisException;
	
	/**
	 * Get the classpath from which classes are loaded. 
	 * 
	 * @return the classpath
	 */
	public IClassPath getClassPath();
}
