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

import java.util.*;
import org.apache.bcel.generic.InstructionHandle;

/**
 * A dataflow analysis to compute dominator relationships between
 * basic blocks.  Use the {@link #getResultFact} method to get the dominator
 * set for a given basic block.  The dominator sets are represented using
 * the {@link java.util.BitSet} class, with the individual bits
 * corresponding to the IDs of basic blocks.
 *
 * <p> Exception edges may be ignored, in which case the domination
 * results only take non-exception control flow into account.
 *
 * @see DataflowAnalysis
 * @see CFG
 * @see BasicBlock
 * @author David Hovemeyer
 */
public class DominatorsAnalysis implements DataflowAnalysis<BitSet> {
	private final CFG cfg;
	private final DepthFirstSearch dfs;
	private final boolean ignoreExceptionEdges;
	private final IdentityHashMap<BasicBlock, BitSet> startFactMap;
	private final IdentityHashMap<BasicBlock, BitSet> resultFactMap;

	/**
	 * Constructor.
	 * @param cfg the CFG to compute dominator relationships for
	 * @param dfs the DepthFirstSearch on the CFG
	 */
	public DominatorsAnalysis(CFG cfg, DepthFirstSearch dfs, boolean ignoreExceptionEdges) {
		this.cfg = cfg;
		this.dfs = dfs;
		this.ignoreExceptionEdges = ignoreExceptionEdges;
		this.startFactMap = new IdentityHashMap<BasicBlock, BitSet>();
		this.resultFactMap = new IdentityHashMap<BasicBlock, BitSet>();
	}

	public BitSet createFact() {
		return new BitSet();
	}

	public BitSet getStartFact(BasicBlock block) {
		return lookupOrCreateFact(startFactMap, block);
	}

	public BitSet getResultFact(BasicBlock block) {
		return lookupOrCreateFact(resultFactMap, block);
	}

	private BitSet lookupOrCreateFact(Map<BasicBlock, BitSet> map, BasicBlock block) {
		BitSet fact = map.get(block);
		if (fact == null) {
			fact = createFact();
			map.put(block, fact);
		}
		return fact;
	}

	public void copy(BitSet source, BitSet dest) {
		dest.clear();
		dest.or(source);
	}

	public void initEntryFact(BitSet result) {
		// No blocks dominate the entry block
		result.clear();
	}

	public void initResultFact(BitSet result) {
		makeFactTop(result);
	}

	public void makeFactTop(BitSet fact) {
		fact.set(0, cfg.getNumBasicBlocks());
	}

	public boolean isForwards() {
		return true;
	}

	public BlockOrder getBlockOrder(CFG cfg) {
		return new ReversePostfixOrder(cfg, dfs);
	}

	public boolean same(BitSet fact1, BitSet fact2) {
		return fact1.equals(fact2);
	}

	public void transfer(BasicBlock basicBlock, InstructionHandle end, BitSet start, BitSet result) throws DataflowAnalysisException {
		// Start with intersection of dominators of predecessors
		copy(start, result);

		// Every block dominates itself
		result.set(basicBlock.getId());
	}

	public void meetInto(BitSet fact, Edge edge, BitSet result) throws DataflowAnalysisException {
		if (ignoreExceptionEdges && edge.isExceptionEdge())
			return;

		// Meet is intersection
		result.and(fact);
	}
}

// vim:ts=4
