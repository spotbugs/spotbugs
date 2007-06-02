/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003-2007 University of Maryland
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
package edu.umd.cs.findbugs.classfile.engine.bcel;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.FindBugsAnalysisFeatures;
import edu.umd.cs.findbugs.ba.AssertionMethods;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.CompactLocationNumbering;
import edu.umd.cs.findbugs.ba.Debug;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.ReverseDepthFirstSearch;
import edu.umd.cs.findbugs.ba.npe.IsNullValueDataflow;
import edu.umd.cs.findbugs.ba.type.ExceptionSetFactory;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.vna.LoadedFieldSet;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.IMethodAnalysisEngine;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * Abstract factory class for creating analysis objects.
 */
public abstract class AnalysisFactory <Analysis> implements IMethodAnalysisEngine {
	private String analysisName;
	private Class<Analysis> analysisClass;

	/**
	 * Constructor.
	 * 
	 * @param analysisName name of the analysis factory: for diagnostics/debugging
	 */
	public AnalysisFactory(String analysisName, Class<Analysis> analysisClass) {
		this.analysisName = analysisName;
		this.analysisClass= analysisClass;
	}

	/* ----------------------------------------------------------------------
	 * IAnalysisEngine methods
	 * ---------------------------------------------------------------------- */

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisEngine#retainAnalysisResults()
	 */
	public final boolean retainAnalysisResults() {
		throw new IllegalStateException();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisEngine#registerWith(edu.umd.cs.findbugs.classfile.IAnalysisCache)
	 */
	public void registerWith(IAnalysisCache analysisCache) {
		analysisCache.registerMethodAnalysisEngine(analysisClass, this);
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.classfile.IAnalysisEngine#noCache()
	 */
	public final boolean noCache() {
		// The caching will be done in the ClassContext.
		return true;
	}
	
	private static final Object NULL_ANALYSIS_RESULT = new Object();
	
//	/* (non-Javadoc)
//	 * @see edu.umd.cs.findbugs.classfile.IAnalysisEngine#analyze(edu.umd.cs.findbugs.classfile.IAnalysisCache, java.lang.Object)
//	 */
//	public final Object analyze(IAnalysisCache analysisCache, MethodDescriptor descriptor) throws CheckedAnalysisException {
//		ClassContext classContext = analysisCache.getClassAnalysis(ClassContext.class, descriptor.getClassDescriptor());
//		Object object = classContext.getMethodAnalysis(analysisClass, descriptor);
//
//		if (object == null) {
//			try {
//				object = create(analysisCache, descriptor);
//				if (object == null) {
//					object = NULL_ANALYSIS_RESULT;
//				}
//			} catch (RuntimeException e) {
//				object = e;
//			} catch (CheckedAnalysisException e) {
//				object = e;
//			}
//		}
//		if (Debug.VERIFY_INTEGRITY && object == null) {
//			throw new IllegalStateException("AnalysisFactory failed to produce a result object");
//		}
//
//		if (object == NULL_ANALYSIS_RESULT) {
//			return null;
//		}
//		if (object instanceof Exception) {
//			if (object instanceof RuntimeException) {
//				throw (RuntimeException) object;
//			} else {
//				throw (CheckedAnalysisException) object;
//			}
//		}
//		
//		return object;
//	}

//	/* ----------------------------------------------------------------------
//	 * Downcall method to create the analysis object
//	 * ---------------------------------------------------------------------- */
//	protected abstract Analysis create(IAnalysisCache analysisCache, MethodDescriptor methodDescriptor)
//			throws CheckedAnalysisException;
	
	/* ----------------------------------------------------------------------
	 * Helper methods to get required analysis objects.
	 * ---------------------------------------------------------------------- */

	protected CFG getCFG(IAnalysisCache analysisCache, MethodDescriptor methodDescriptor)
	throws CheckedAnalysisException {
		return analysisCache.getMethodAnalysis(CFG.class, methodDescriptor);
	}

	protected DepthFirstSearch getDepthFirstSearch(IAnalysisCache analysisCache, MethodDescriptor methodDescriptor)
	throws CheckedAnalysisException {
		return analysisCache.getMethodAnalysis(DepthFirstSearch.class, methodDescriptor);
	}

	protected ConstantPoolGen getConstantPoolGen(IAnalysisCache analysisCache, ClassDescriptor classDescriptor)
	throws CheckedAnalysisException {
		return analysisCache.getClassAnalysis(ConstantPoolGen.class, classDescriptor);
	}

	protected MethodGen getMethodGen(IAnalysisCache analysisCache, MethodDescriptor methodDescriptor)
	throws CheckedAnalysisException {
		return analysisCache.getMethodAnalysis(MethodGen.class, methodDescriptor);
	}

	protected CompactLocationNumbering getCompactLocationNumbering(IAnalysisCache analysisCache, MethodDescriptor methodDescriptor)
	throws CheckedAnalysisException {
		return analysisCache.getMethodAnalysis(CompactLocationNumbering.class, methodDescriptor);
	}

	protected ValueNumberDataflow getValueNumberDataflow(IAnalysisCache analysisCache, MethodDescriptor methodDescriptor) 
	throws CheckedAnalysisException {
		return analysisCache.getMethodAnalysis(ValueNumberDataflow.class, methodDescriptor);
	}

	protected AssertionMethods getAssertionMethods(IAnalysisCache analysisCache, ClassDescriptor classDescriptor) 
	throws CheckedAnalysisException {
		return analysisCache.getClassAnalysis(AssertionMethods.class, classDescriptor);
	}

	protected JavaClass getJavaClass(IAnalysisCache analysisCache, ClassDescriptor classDescriptor)
	throws CheckedAnalysisException {
		return analysisCache.getClassAnalysis(JavaClass.class, classDescriptor);
	}

	protected Method getMethod(IAnalysisCache analysisCache, MethodDescriptor methodDescriptor)
	throws CheckedAnalysisException {
		return analysisCache.getMethodAnalysis(Method.class, methodDescriptor);
	}

	protected ReverseDepthFirstSearch getReverseDepthFirstSearch(IAnalysisCache analysisCache, MethodDescriptor methodDescriptor)
	throws CheckedAnalysisException {
		return analysisCache.getMethodAnalysis(ReverseDepthFirstSearch.class, methodDescriptor);
	}

	protected ExceptionSetFactory getExceptionSetFactory(IAnalysisCache analysisCache, MethodDescriptor methodDescriptor)
	throws CheckedAnalysisException {
		return analysisCache.getMethodAnalysis(ExceptionSetFactory.class, methodDescriptor);
	}

	protected IsNullValueDataflow getIsNullValueDataflow(IAnalysisCache analysisCache, MethodDescriptor methodDescriptor) 
	throws CheckedAnalysisException {
		return analysisCache.getMethodAnalysis(IsNullValueDataflow.class, methodDescriptor);
	}

	protected TypeDataflow getTypeDataflow(IAnalysisCache analysisCache, MethodDescriptor methodDescriptor)
	throws CheckedAnalysisException {
		return analysisCache.getMethodAnalysis(TypeDataflow.class, methodDescriptor);
	}

	protected LoadedFieldSet getLoadedFieldSet(IAnalysisCache analysisCache, MethodDescriptor methodDescriptor) 
	throws CheckedAnalysisException {
		return analysisCache.getMethodAnalysis(LoadedFieldSet.class, methodDescriptor);
	}
}
