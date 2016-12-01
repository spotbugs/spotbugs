/*
 * FindBugs - Find Bugs in Java programs
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

package edu.umd.cs.findbugs.bcel;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.Detector2;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * Base class for detectors that analyze CFG (and/or use CFG-based analyses).
 *
 * @author David Hovemeyer
 */
public abstract class CFGDetector implements Detector2 {

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.Detector2#finishPass()
     */
    @Override
    public void finishPass() {
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.Detector2#getDetectorClassName()
     */
    @Override
    public String getDetectorClassName() {
        return getClass().getName();
    }

    protected ClassContext classContext;
    protected Method method;
    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.Detector2#visitClass(edu.umd.cs.findbugs.classfile
     * .ClassDescriptor)
     */
    @Override
    public void visitClass(ClassDescriptor classDescriptor) throws CheckedAnalysisException {
        IAnalysisCache analysisCache = Global.getAnalysisCache();

        JavaClass jclass = analysisCache.getClassAnalysis(JavaClass.class, classDescriptor);
        classContext = analysisCache.getClassAnalysis(ClassContext.class, classDescriptor);

        for (Method m : classContext.getMethodsInCallOrder()) {
            if (m.getCode() == null) {
                continue;
            }
            method = m;

            MethodDescriptor methodDescriptor = BCELUtil.getMethodDescriptor(jclass, method);

            // Try to get MethodGen. If we can't get one,
            // then this method should be skipped.
            MethodGen methodGen = analysisCache.getMethodAnalysis(MethodGen.class, methodDescriptor);
            if (methodGen == null) {
                continue;
            }

            CFG cfg = analysisCache.getMethodAnalysis(CFG.class, methodDescriptor);
            visitMethodCFG(methodDescriptor, cfg);
        }
    }

    /**
     * Visit the CFG (control flow graph) of a method to be analyzed. Should be
     * overridded by subclasses.
     *
     * @param methodDescriptor
     * @param cfg
     * @throws CheckedAnalysisException
     */
    protected abstract void visitMethodCFG(MethodDescriptor methodDescriptor, CFG cfg) throws CheckedAnalysisException;

}
