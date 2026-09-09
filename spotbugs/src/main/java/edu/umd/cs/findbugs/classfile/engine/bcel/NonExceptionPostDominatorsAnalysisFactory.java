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

import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.Dataflow;
import edu.umd.cs.findbugs.ba.PostDominatorsAnalysis;
import edu.umd.cs.findbugs.ba.ReverseDepthFirstSearch;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * Analysis engine to produce NonExceptionPostDominatorsAnalysis objects for
 * analyzed methods.
 *
 * @author David Hovemeyer
 */
public class NonExceptionPostDominatorsAnalysisFactory extends AnalysisFactory<NonExceptionPostDominatorsAnalysis> {
    public NonExceptionPostDominatorsAnalysisFactory() {
        super("non-exception postdominators analysis", NonExceptionPostDominatorsAnalysis.class);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.IAnalysisEngine#analyze(edu.umd.cs.findbugs
     * .classfile.IAnalysisCache, java.lang.Object)
     */
    @Override
    public NonExceptionPostDominatorsAnalysis analyze(IAnalysisCache analysisCache, MethodDescriptor descriptor)
            throws CheckedAnalysisException {
        CFG cfg = getCFG(analysisCache, descriptor);
        ReverseDepthFirstSearch rdfs = getReverseDepthFirstSearch(analysisCache, descriptor);
        NonExceptionPostDominatorsAnalysis analysis = new NonExceptionPostDominatorsAnalysis(cfg, rdfs, getDepthFirstSearch(
                analysisCache, descriptor));
        Dataflow<java.util.BitSet, PostDominatorsAnalysis> dataflow = new Dataflow<>(cfg,
                analysis);
        dataflow.execute();
        return analysis;
    }
}
