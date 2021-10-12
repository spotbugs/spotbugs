/*
 * SpotBugs - Find Bugs in Java programs
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
import edu.umd.cs.findbugs.ba.DominatorsAnalysis;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

import java.util.BitSet;

/**
 * Analysis engine to produce NonImplicitExceptionDominatorsAnalysis objects
 * for analyzed methods.
 */
public class NonImplicitExceptionDominatorsAnalysisFactory extends
        AnalysisFactory<NonImplicitExceptionDominatorsAnalysis> {
    /**
     * Constructor.
     */
    public NonImplicitExceptionDominatorsAnalysisFactory() {
        super("non-implicit-exception postdominators analysis", NonImplicitExceptionDominatorsAnalysis.class);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.classfile.IAnalysisEngine#analyze(edu.umd.cs.findbugs
     * .classfile.IAnalysisCache, java.lang.Object)
     */
    @Override
    public NonImplicitExceptionDominatorsAnalysis analyze(IAnalysisCache analysisCache, MethodDescriptor descriptor)
            throws CheckedAnalysisException {
        CFG cfg = getCFG(analysisCache, descriptor);
        NonImplicitExceptionDominatorsAnalysis analysis = new NonImplicitExceptionDominatorsAnalysis(cfg,
                getDepthFirstSearch(analysisCache, descriptor));
        Dataflow<BitSet, DominatorsAnalysis> dataflow = new Dataflow<>(cfg, analysis);
        dataflow.execute();

        return analysis;
    }
}
