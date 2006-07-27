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

package edu.umd.cs.findbugs.ba;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.ba.ch.Subtypes;
import edu.umd.cs.findbugs.ba.npe.ParameterNullnessPropertyDatabase;
import edu.umd.cs.findbugs.ba.type.FieldStoreTypeDatabase;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IErrorLogger;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * An AnalysisContext implementation that uses the
 * IAnalysisCache.
 * 
 * <p>
 * <b>NOTE</b>: not fully implemented yet.
 * </p>
 * 
 * @author David Hovemeyer
 */
public class AnalysisCacheToAnalysisContextAdapter extends AnalysisContext {
	
	static class DelegatingRepositoryLookupFailureCallback implements RepositoryLookupFailureCallback {

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.classfile.IErrorLogger#logError(java.lang.String)
		 */
		public void logError(String message) {
			Global.getAnalysisCache().getErrorLogger().logError(message);
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.classfile.IErrorLogger#logError(java.lang.String, java.lang.Throwable)
		 */
		public void logError(String message, Throwable e) {
			Global.getAnalysisCache().getErrorLogger().logError(message, e);
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.classfile.IErrorLogger#reportMissingClass(java.lang.ClassNotFoundException)
		 */
		public void reportMissingClass(ClassNotFoundException ex) {
			Global.getAnalysisCache().getErrorLogger().reportMissingClass(ex);
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.classfile.IErrorLogger#reportSkippedAnalysis(edu.umd.cs.findbugs.classfile.MethodDescriptor)
		 */
		public void reportSkippedAnalysis(MethodDescriptor method) {
			Global.getAnalysisCache().getErrorLogger().reportSkippedAnalysis(method);
		}
		
	}
	
	private RepositoryLookupFailureCallback lookupFailureCallback;
	
	AnalysisCacheToAnalysisContextAdapter() {
		this.lookupFailureCallback = new DelegatingRepositoryLookupFailureCallback();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#addApplicationClassToRepository(org.apache.bcel.classfile.JavaClass)
	 */
	@Override
	public void addApplicationClassToRepository(JavaClass appClass) {
		Repository.addClass(appClass);
		getSubtypes().addApplicationClass(appClass);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#addClasspathEntry(java.lang.String)
	 */
	@Override
	public void addClasspathEntry(String url) throws IOException {
		// FIXME: can't support this - AnalysisContext is responsible for the classpath 
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#clearClassContextCache()
	 */
	@Override
	public void clearClassContextCache() {
		// TODO
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#clearRepository()
	 */
	@Override
	public void clearRepository() {
		// TODO 

	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#getAnalysisLocals()
	 */
	@Override
	public Map getAnalysisLocals() {
		// TODO 
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#getAnnotationRetentionDatabase()
	 */
	@Override
	public AnnotationRetentionDatabase getAnnotationRetentionDatabase() {
		// TODO 
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#getBoolProperty(int)
	 */
	@Override
	public boolean getBoolProperty(int prop) {
		// TODO 
		return false;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#getCheckReturnAnnotationDatabase()
	 */
	@Override
	public CheckReturnAnnotationDatabase getCheckReturnAnnotationDatabase() {
		// TODO 
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#getClassContext(org.apache.bcel.classfile.JavaClass)
	 */
	@Override
	public ClassContext getClassContext(JavaClass javaClass) {
		// TODO 
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#getClassContextStats()
	 */
	@Override
	public String getClassContextStats() {
		// TODO 
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#getDatabaseInputDir()
	 */
	@Override
	public String getDatabaseInputDir() {
		// TODO 
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#getDatabaseOutputDir()
	 */
	@Override
	public String getDatabaseOutputDir() {
		// TODO 
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#getFieldStoreTypeDatabase()
	 */
	@Override
	public FieldStoreTypeDatabase getFieldStoreTypeDatabase() {
		// TODO 
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#getJCIPAnnotationDatabase()
	 */
	@Override
	public JCIPAnnotationDatabase getJCIPAnnotationDatabase() {
		// TODO 
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#getLookupFailureCallback()
	 */
	@Override
	public RepositoryLookupFailureCallback getLookupFailureCallback() {
		// TODO 
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#getNullnessAnnotationDatabase()
	 */
	@Override
	public NullnessAnnotationDatabase getNullnessAnnotationDatabase() {
		// TODO 
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#getSourceFinder()
	 */
	@Override
	public SourceFinder getSourceFinder() {
		// TODO 
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#getSourceInfoMap()
	 */
	@Override
	public SourceInfoMap getSourceInfoMap() {
		// TODO 
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#getSubtypes()
	 */
	@Override
	public Subtypes getSubtypes() {
		return getDatabase(Subtypes.class);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#getUnconditionalDerefParamDatabase()
	 */
	@Override
	public ParameterNullnessPropertyDatabase getUnconditionalDerefParamDatabase() {
		// TODO 
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#initDatabases()
	 */
	@Override
	public void initDatabases() {
		// TODO 

	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#isApplicationClass(org.apache.bcel.classfile.JavaClass)
	 */
	@Override
	public boolean isApplicationClass(JavaClass cls) {
		// TODO 
		return false;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#isApplicationClass(java.lang.String)
	 */
	@Override
	public boolean isApplicationClass(String className) {
		// TODO 
		return false;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#loadDefaultInterproceduralDatabases()
	 */
	@Override
	public void loadDefaultInterproceduralDatabases() {
		// TODO 

	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#loadInterproceduralDatabases()
	 */
	@Override
	public void loadInterproceduralDatabases() {
		// TODO 

	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#lookupClass(java.lang.String)
	 */
	@Override
	public JavaClass lookupClass(String className)
			throws ClassNotFoundException {
		// TODO 
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#lookupSourceFile(java.lang.String)
	 */
	@Override
	public String lookupSourceFile(String className) {
		// TODO 
		return null;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#setBoolProperty(int, boolean)
	 */
	@Override
	public void setBoolProperty(int prop, boolean value) {
		// TODO 

	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#setDatabaseInputDir(java.lang.String)
	 */
	@Override
	public void setDatabaseInputDir(String databaseInputDir) {
		// TODO 

	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#setDatabaseOutputDir(java.lang.String)
	 */
	@Override
	public void setDatabaseOutputDir(String databaseOutputDir) {
		// TODO 

	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#setSourcePath(java.util.List)
	 */
	@Override
	public void setSourcePath(List<String> sourcePath) {
		// TODO 

	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.AnalysisContext#setUnconditionalDerefParamDatabase(edu.umd.cs.findbugs.ba.npe.ParameterNullnessPropertyDatabase)
	 */
	@Override
	public void setUnconditionalDerefParamDatabase(
			ParameterNullnessPropertyDatabase unconditionalDerefParamDatabase) {
		// TODO 

	}

	/**
	 * Helper method to get a database
	 * without having to worry about a
	 * CheckedAnalysisException.
	 * 
	 * @param cls Class of the database to get
	 * @return the database
	 */
	private<E> E getDatabase(Class<E> cls) {
		try {
			return Global.getAnalysisCache().getDatabase(cls);
		} catch (CheckedAnalysisException e) {
			throw new IllegalStateException("Could not get database " + cls.getName(), e);
		}
	}

}
