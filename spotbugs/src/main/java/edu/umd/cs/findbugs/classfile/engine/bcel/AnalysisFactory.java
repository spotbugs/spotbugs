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

import edu.umd.cs.findbugs.ba.AssertionMethods;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CompactLocationNumbering;
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
public abstract class AnalysisFactory<Analysis> implements IMethodAnalysisEngine<Analysis> {
    private final String analysisName;

    private final Class<Analysis> analysisClass;

    /**
     * Constructor.
     *
     * @param analysisName
     *            name of the analysis factory: for diagnostics/debugging
     */
    public AnalysisFactory(String analysisName, Class<Analysis> analysisClass) {
        this.analysisName = analysisName;
        this.analysisClass = analysisClass;
    }

    @Override
    public String toString() {
        return analysisName + " : " + analysisClass.getName();
    }

    /*
     * ----------------------------------------------------------------------
     * IAnalysisEngine methods
     * ----------------------------------------------------------------------
     */

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.IAnalysisEngine#registerWith(edu.umd.cs
     * .findbugs.classfile.IAnalysisCache)
     */
    @Override
    public void registerWith(IAnalysisCache analysisCache) {
        analysisCache.registerMethodAnalysisEngine(analysisClass, this);
    }

    private static final Object NULL_ANALYSIS_RESULT = new Object();

    /*
     * ----------------------------------------------------------------------
     * Helper methods to get required analysis objects.
     * ----------------------------------------------------------------------
     */

    protected CFG getCFG(IAnalysisCache analysisCache, MethodDescriptor methodDescriptor) throws CheckedAnalysisException {
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

    protected Method getMethod(IAnalysisCache analysisCache, MethodDescriptor methodDescriptor) throws CheckedAnalysisException {
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
