/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2008, University of Maryland
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

import org.apache.bcel.generic.ConstantPoolGen;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.DataflowCFGPrinter;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.npe.IsNullValueDataflow;
import edu.umd.cs.findbugs.ba.obl.ObligationAnalysis;
import edu.umd.cs.findbugs.ba.obl.ObligationDataflow;
import edu.umd.cs.findbugs.ba.obl.ObligationFactory;
import edu.umd.cs.findbugs.ba.obl.ObligationPolicyDatabase;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.log.Profiler;

/**
 * Analysis factory which creates instances of ObligationDataflow.
 *
 * @author David Hovemeyer
 */
public class ObligationDataflowFactory extends AnalysisFactory<ObligationDataflow> {

    private static final boolean DEBUG_PRINTCFG = SystemProperties.getBoolean("oa.printcfg");

    public ObligationDataflowFactory() {
        super("Obligation dataflow", ObligationDataflow.class);
    }

    @Override
    public ObligationDataflow analyze(IAnalysisCache analysisCache, MethodDescriptor methodDescriptor)
            throws CheckedAnalysisException {
        CFG cfg = analysisCache.getMethodAnalysis(CFG.class, methodDescriptor);
        DepthFirstSearch dfs = analysisCache.getMethodAnalysis(DepthFirstSearch.class, methodDescriptor);
        XMethod xmethod = XFactory.createXMethod(methodDescriptor);
        ConstantPoolGen cpg = analysisCache.getClassAnalysis(ConstantPoolGen.class, methodDescriptor.getClassDescriptor());

        ObligationPolicyDatabase database = analysisCache.getDatabase(ObligationPolicyDatabase.class);

        TypeDataflow typeDataflow = analysisCache.getMethodAnalysis(TypeDataflow.class, methodDescriptor);
        IsNullValueDataflow invDataflow = analysisCache.getMethodAnalysis(IsNullValueDataflow.class, methodDescriptor);

        ObligationFactory factory = database.getFactory();

        ObligationAnalysis analysis = new ObligationAnalysis(dfs, xmethod, cpg, factory, database, typeDataflow, invDataflow,
                analysisCache.getErrorLogger());
        ObligationDataflow dataflow = new ObligationDataflow(cfg, analysis);

        Profiler profiler = analysisCache.getProfiler();
        profiler.start(analysis.getClass());
        try {
            dataflow.execute();
        } finally {
            profiler.end(analysis.getClass());
        }

        if (DEBUG_PRINTCFG) {
            System.out.println("Dataflow CFG:");
            DataflowCFGPrinter.printCFG(dataflow, System.out);
        }

        return dataflow;
    }
}
