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

package edu.umd.cs.findbugs.classfile.engine.bcel;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * Method analysis engine to produce BCEL Method objects.
 *
 * @author David Hovemeyer
 */
public class MethodFactory extends AnalysisFactory<Method> {

    public MethodFactory() {
        super("Method factory", Method.class);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.IAnalysisEngine#analyze(edu.umd.cs.findbugs
     * .classfile.IAnalysisCache, java.lang.Object)
     */
    @Override
    public Method analyze(IAnalysisCache analysisCache, MethodDescriptor descriptor) throws CheckedAnalysisException {
        JavaClass jclass = analysisCache.getClassAnalysis(JavaClass.class, descriptor.getClassDescriptor());
        Method[] methodList = jclass.getMethods();

        Method result = null;

        // As a side-effect, cache all of the Methods for this JavaClass
        for (Method method : methodList) {
            MethodDescriptor methodDescriptor = DescriptorFactory.instance().getMethodDescriptor(
                    descriptor.getSlashedClassName(), method.getName(), method.getSignature(), method.isStatic());

            // Put in cache eagerly
            analysisCache.eagerlyPutMethodAnalysis(Method.class, methodDescriptor, method);

            if (methodDescriptor.equals(descriptor)) {
                result = method;
            }
        }

        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.IAnalysisEngine#registerWith(edu.umd.cs
     * .findbugs.classfile.IAnalysisCache)
     */
    @Override
    public void registerWith(IAnalysisCache analysisCache) {
        analysisCache.registerMethodAnalysisEngine(Method.class, this);
    }

}
