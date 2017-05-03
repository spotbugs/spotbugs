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

import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.ba.AssertionMethods;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.JavaClassAndMethod;
import edu.umd.cs.findbugs.ba.MethodUnprofitableException;
import edu.umd.cs.findbugs.ba.npe.IsNullValueAnalysis;
import edu.umd.cs.findbugs.ba.npe.IsNullValueDataflow;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * Analysis engine to produce IsNullValueDataflow objects for an analyzed
 * method.
 *
 * @author David Hovemeyer
 */
public class IsNullValueDataflowFactory extends AnalysisFactory<IsNullValueDataflow> {
    /**
     * Constructor.
     */
    public IsNullValueDataflowFactory() {
        super("null value analysis", IsNullValueDataflow.class);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.IAnalysisEngine#analyze(edu.umd.cs.findbugs
     * .classfile.IAnalysisCache, java.lang.Object)
     */
    @Override
    public IsNullValueDataflow analyze(IAnalysisCache analysisCache, MethodDescriptor descriptor) throws CheckedAnalysisException {
        MethodGen methodGen = getMethodGen(analysisCache, descriptor);
        if (methodGen == null) {
            throw new MethodUnprofitableException(descriptor);
        }
        CFG cfg = getCFG(analysisCache, descriptor);
        ValueNumberDataflow vnaDataflow = getValueNumberDataflow(analysisCache, descriptor);
        DepthFirstSearch dfs = getDepthFirstSearch(analysisCache, descriptor);
        AssertionMethods assertionMethods = getAssertionMethods(analysisCache, descriptor.getClassDescriptor());
        TypeDataflow typeDataflow = getTypeDataflow(analysisCache, descriptor);

        IsNullValueAnalysis invAnalysis = new IsNullValueAnalysis(descriptor, methodGen, cfg, vnaDataflow, typeDataflow, dfs,
                assertionMethods);

        // Set return value and parameter databases

        invAnalysis.setClassAndMethod(new JavaClassAndMethod(getJavaClass(analysisCache, descriptor.getClassDescriptor()),
                getMethod(analysisCache, descriptor)));

        IsNullValueDataflow invDataflow = new IsNullValueDataflow(cfg, invAnalysis);
        invDataflow.execute();
        if (ClassContext.DUMP_DATAFLOW_ANALYSIS) {
            invDataflow.dumpDataflow(invAnalysis);
        }
        return invDataflow;

    }
}
