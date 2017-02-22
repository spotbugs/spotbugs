/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003,2004 University of Maryland
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

package edu.umd.cs.findbugs.ba;

import java.util.Comparator;

/**
 * A BlockOrder for visiting the blocks of a CFG in the order they would be
 * visited in a depth first search of the reversed CFG. This is the most
 * efficient visitation order for backwards dataflow analyses.
 *
 * @see BlockOrder
 * @see ReverseDepthFirstSearch
 * @see CFG
 * @see BasicBlock
 */
public class ReverseDFSOrder extends AbstractBlockOrder {

    private static class ReverseDFSComparator implements Comparator<BasicBlock> {
        private final DepthFirstSearch dfs;

        public ReverseDFSComparator(ReverseDepthFirstSearch rdfs, DepthFirstSearch dfs) {
            if (dfs == null) {
                throw new IllegalArgumentException();
            }
            this.dfs = dfs;
        }

        @Override
        public int compare(BasicBlock a, BasicBlock b) {
            return dfs.getFinishTime(a) - dfs.getFinishTime(b);
        }
    }

    public final ReverseDepthFirstSearch rdfs;

    public final DepthFirstSearch dfs;

    /**
     * Constructor.
     *
     * @param cfg
     *            the CFG
     * @param rdfs
     *            the ReverseDepthFirstSearch of the CFG
     * @param dfs
     *            TODO
     */
    public ReverseDFSOrder(CFG cfg, ReverseDepthFirstSearch rdfs, DepthFirstSearch dfs) {
        super(cfg, new ReverseDFSComparator(rdfs, dfs));
        this.rdfs = rdfs;
        this.dfs = dfs;
    }
}

