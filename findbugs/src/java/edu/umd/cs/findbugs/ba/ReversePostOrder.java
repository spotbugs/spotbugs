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
 * A BlockOrder for visiting the blocks of a CFG in
 * the reverse of the order in which they are finished in
 * a depth first search.  This is the most efficient visitation
 * order for forward dataflow analyses.
 *
 * @see BlockOrder
 * @see DepthFirstSearch
 * @see CFG
 * @see BasicBlock
 */
public class ReversePostOrder extends AbstractBlockOrder {
	/**
	 * A Comparator to order the blocks in the reverse of the
	 * order in which they would be finished by a depth first search.
	 */
	private static class ReversePostfixComparator implements Comparator<BasicBlock> {
		private DepthFirstSearch dfs;

		public ReversePostfixComparator(DepthFirstSearch dfs) {
			this.dfs = dfs;
		}

		public int compare(BasicBlock aa, BasicBlock bb) {
			return dfs.getFinishTime(bb) - dfs.getFinishTime(aa);
		}
	}

	/**
	 * Constructor.
	 *
	 * @param cfg the CFG for the method
	 * @param dfs the DepthFirstSearch on the method
	 */
	public ReversePostOrder(CFG cfg, DepthFirstSearch dfs) {
		super(cfg, new ReversePostfixComparator(dfs));
	}
}

// vim:ts=4
