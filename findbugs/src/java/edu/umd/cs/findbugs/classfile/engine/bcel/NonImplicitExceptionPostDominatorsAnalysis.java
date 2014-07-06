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
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.EdgeChooser;
import edu.umd.cs.findbugs.ba.EdgeTypes;
import edu.umd.cs.findbugs.ba.PostDominatorsAnalysis;
import edu.umd.cs.findbugs.ba.ReverseDepthFirstSearch;

/**
 * PostDominatorsAnalysis variant in which implicit exception edges are ignored.
 * Implicit exception edges correspond to undeclared runtime exceptions; thus,
 * this analysis considers only normal control edges and declared exception
 * edges.
 *
 * @author David Hovemeyer
 */
public class NonImplicitExceptionPostDominatorsAnalysis extends PostDominatorsAnalysis {
    public NonImplicitExceptionPostDominatorsAnalysis(CFG cfg, ReverseDepthFirstSearch rdfs, DepthFirstSearch dfs) {
        super(cfg, rdfs, dfs, new EdgeChooser() {
            @Override
            public boolean choose(Edge edge) {
                return !edge.isExceptionEdge() || edge.isFlagSet(EdgeTypes.EXPLICIT_EXCEPTIONS_FLAG);
            }
        });
    }
}
