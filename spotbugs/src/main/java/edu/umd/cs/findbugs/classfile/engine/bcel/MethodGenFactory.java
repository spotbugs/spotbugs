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

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.AnalysisFeatures;
import edu.umd.cs.findbugs.ba.JavaClassAndMethod;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * Analysis engine to produce MethodGen objects for analyzed methods.
 *
 * @author David Hovemeyer
 * @author Bill Pugh
 */
public class MethodGenFactory extends AnalysisFactory<MethodGen> {
    /**
     * Constructor.
     */
    public MethodGenFactory() {
        super("MethodGen construction", MethodGen.class);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.IAnalysisEngine#analyze(edu.umd.cs.findbugs
     * .classfile.IAnalysisCache, java.lang.Object)
     */
    @Override
    public MethodGen analyze(IAnalysisCache analysisCache, MethodDescriptor descriptor) throws CheckedAnalysisException {
        Method method = getMethod(analysisCache, descriptor);

        if (method.getCode() == null) {
            return null;
        }
        XMethod xmethod =  XFactory.createXMethod(descriptor);
        if (xmethod.usesInvokeDynamic() && false) {
            AnalysisContext.currentAnalysisContext().analysisSkippedDueToInvokeDynamic(xmethod);
            return null;
        }

        try {
            AnalysisContext analysisContext = AnalysisContext.currentAnalysisContext();
            JavaClass jclass = getJavaClass(analysisCache, descriptor.getClassDescriptor());
            ConstantPoolGen cpg = getConstantPoolGen(analysisCache, descriptor.getClassDescriptor());

            String methodName = method.getName();
            int codeLength = method.getCode().getCode().length;
            String superclassName = jclass.getSuperclassName();
            if (codeLength > 6000 && "<clinit>".equals(methodName) && "java.lang.Enum".equals(superclassName)) {
                analysisContext.getLookupFailureCallback().reportSkippedAnalysis(
                        new JavaClassAndMethod(jclass, method).toMethodDescriptor());
                return null;
            }
            if (analysisContext.getBoolProperty(AnalysisFeatures.SKIP_HUGE_METHODS)) {
                if (codeLength > 6000 || ("<clinit>".equals(methodName) || "getContents".equals(methodName)) && codeLength > 2000) {
                    analysisContext.getLookupFailureCallback().reportSkippedAnalysis(
                            new JavaClassAndMethod(jclass, method).toMethodDescriptor());
                    return null;
                }
            }

            return new MethodGen(method, jclass.getClassName(), cpg);
        } catch (Exception e) {
            AnalysisContext.logError("Error constructing methodGen", e);
            return null;
        }
    }
}
