/*
 * SpotBugs - Find bugs in Java programs
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
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.DominatorsAnalysis;
import edu.umd.cs.findbugs.ba.EdgeTypes;

/**
 * DominatorsAnalysis variant in which implicit exception edges are ignored.
 * Implicit exception edges represent the control flow due to the emission
 * of unchecked exceptions, so the analysis only considers control flow
 * resulting from normal control structures and checked exceptions.
 */
public class NonImplicitExceptionDominatorsAnalysis extends DominatorsAnalysis {
    /**
     * Constructor.
     *
     * @param cfg
     *            the CFG to compute dominator relationships for
     * @param dfs
     *            the DepthFirstSearch on the CFG
     */
    public NonImplicitExceptionDominatorsAnalysis(CFG cfg, DepthFirstSearch dfs) {
        super(cfg, dfs, edge -> !edge.isExceptionEdge() || edge.isFlagSet(EdgeTypes.EXPLICIT_EXCEPTIONS_FLAG));
    }
}
