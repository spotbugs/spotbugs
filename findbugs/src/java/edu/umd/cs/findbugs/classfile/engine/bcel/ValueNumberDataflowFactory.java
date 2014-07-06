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

import java.util.Iterator;
import java.util.TreeSet;

import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.MethodUnprofitableException;
import edu.umd.cs.findbugs.ba.vna.LoadedFieldSet;
import edu.umd.cs.findbugs.ba.vna.MergeTree;
import edu.umd.cs.findbugs.ba.vna.ValueNumberAnalysis;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * Analysis engine to produce ValueNumberDataflow objects for analyzed methods.
 *
 * @author David Hovemeyer
 */
public class ValueNumberDataflowFactory extends AnalysisFactory<ValueNumberDataflow> {
    /**
     * Constructor.
     */
    public ValueNumberDataflowFactory() {
        super("value number analysis", ValueNumberDataflow.class);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.IAnalysisEngine#analyze(edu.umd.cs.findbugs
     * .classfile.IAnalysisCache, java.lang.Object)
     */
    @Override
    public ValueNumberDataflow analyze(IAnalysisCache analysisCache, MethodDescriptor descriptor) throws CheckedAnalysisException {
        MethodGen methodGen = getMethodGen(analysisCache, descriptor);
        if (methodGen == null) {
            throw new MethodUnprofitableException(descriptor);
        }

        DepthFirstSearch dfs = getDepthFirstSearch(analysisCache, descriptor);
        LoadedFieldSet loadedFieldSet = getLoadedFieldSet(analysisCache, descriptor);
        ValueNumberAnalysis analysis = new ValueNumberAnalysis(methodGen, dfs, loadedFieldSet, AnalysisContext
                .currentAnalysisContext().getLookupFailureCallback());
        analysis.setMergeTree(new MergeTree(analysis.getFactory()));
        CFG cfg = getCFG(analysisCache, descriptor);

        ValueNumberDataflow vnaDataflow = new ValueNumberDataflow(cfg, analysis);
        vnaDataflow.execute();
        if (ClassContext.DUMP_DATAFLOW_ANALYSIS) {
            TreeSet<Location> tree = new TreeSet<Location>();
            for (Iterator<Location> locs = cfg.locationIterator(); locs.hasNext();) {
                Location loc = locs.next();
                tree.add(loc);
            }
            System.out.println("\n\nValue number analysis for " + descriptor.getName() + descriptor.getSignature() + " {");
            for (Location loc : tree) {
                System.out.println("\nBefore: " + vnaDataflow.getFactAtLocation(loc));
                System.out.println("Location: " + loc);
                System.out.println("After: " + vnaDataflow.getFactAfterLocation(loc));
            }
            System.out.println("}\n");
        }
        return vnaDataflow;
    }
}
