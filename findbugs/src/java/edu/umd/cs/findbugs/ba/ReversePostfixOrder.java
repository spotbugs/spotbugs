package edu.umd.cs.daveho.ba;

import java.util.*;

/**
 * A BlockOrder for visiting the blocks of a CFG in
 * the reverse of the order in which they are finished in
 * a depth first search.  This is the most efficient visitation
 * order for forward dataflow analyses.
 * @see BlockOrder
 * @see DepthFirstSearch
 * @see CFG
 * @see BasicBlock
 */
public class ReversePostfixOrder extends AbstractBlockOrder {
	/**
	 * A Comparator to order the blocks in the reverse of the
	 * order in which they would be finished by a depth first search.
	 */
	private static class ReversePostfixComparator implements Comparator<BasicBlock> {
		private DepthFirstSearch dfs;

		public ReversePostfixComparator(CFG cfg) {
			// Perform the depth first search
			dfs = new DepthFirstSearch(cfg);
			dfs.search();
		}

		public int compare(BasicBlock aa, BasicBlock bb) {
			return dfs.getFinishTime(bb) - dfs.getFinishTime(aa);
		}
	}

	/**
	 * Constructor.
	 * @param cfg the CFG whose blocks should be put in reverse postfix order
	 */
	public ReversePostfixOrder(CFG cfg) {
		super(cfg, new ReversePostfixComparator(cfg));
	}
}

// vim:ts=4
