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

import java.util.BitSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * A dataflow analysis to compute dominator relationships between
 * basic blocks.  Use the {@link #getResultFact} method to get the dominator
 * set for a given basic block.  The dominator sets are represented using
 * the {@link java.util.BitSet} class, with the individual bits
 * corresponding to the IDs of basic blocks.
 * <p/>
 * <p> Subclasses extend this class to compute either dominators
 * or postdominators.
 * <p/>
 * <p> An EdgeChooser may be specified to select which edges
 * to take into account. For example, exception edges could be
 * ignored.</p>
 *
 * @author David Hovemeyer
 * @see DataflowAnalysis
 * @see CFG
 * @see BasicBlock
 */
public abstract class AbstractDominatorsAnalysis extends BasicAbstractDataflowAnalysis<BitSet> {
	private final CFG cfg;
	private EdgeChooser edgeChooser;

	/**
	 * Constructor.
	 *
	 * @param cfg                  the CFG to compute dominator relationships for
	 * @param ignoreExceptionEdges true if exception edges should be ignored
	 */
	public AbstractDominatorsAnalysis(CFG cfg, final boolean ignoreExceptionEdges) {
		this(cfg, new EdgeChooser() {
			public boolean choose(Edge edge) {
				if (ignoreExceptionEdges && edge.isExceptionEdge())
					return false;
				else
					return true;
			}
		});
	}

	/**
	 * Constructor.
	 * 
	 * @param cfg         the CFG to compute dominator relationships for
	 * @param edgeChooser EdgeChooser to choose which Edges to consider significant
	 */
	public AbstractDominatorsAnalysis(CFG cfg, EdgeChooser edgeChooser) {
		this.cfg = cfg;
		this.edgeChooser = edgeChooser;
	}

	public BitSet createFact() {
		return new BitSet();
	}

	public void copy(BitSet source, BitSet dest) {
		dest.clear();
		dest.or(source);
	}

	public void initEntryFact(BitSet result) {
		// No blocks dominate the entry block
		result.clear();
	}

	public boolean isTop(BitSet fact) {
		// We represent TOP as a bitset with an illegal bit set
		return fact.get(cfg.getNumBasicBlocks());
	}

	public void makeFactTop(BitSet fact) {
		// We represent TOP as a bitset with an illegal bit set
		fact.set(cfg.getNumBasicBlocks());
	}

	public boolean same(BitSet fact1, BitSet fact2) {
		return fact1.equals(fact2);
	}

	public void transfer(BasicBlock basicBlock, @CheckForNull InstructionHandle end, BitSet start, BitSet result) throws DataflowAnalysisException {
		// Start with intersection of dominators of predecessors
		copy(start, result);

		if (!isTop(result)) {
			// Every block dominates itself
			result.set(basicBlock.getId());
		}
	}

	public void meetInto(BitSet fact, Edge edge, BitSet result) throws DataflowAnalysisException {
		if (!edgeChooser.choose(edge))
			return;

		if (isTop(fact))
			return;
		else if (isTop(result))
			copy(fact, result);
		else
		// Meet is intersection
			result.and(fact);
	}

	/**
	 * Get a bitset containing the unique IDs of
	 * all blocks which dominate (or postdominate) the given block.
	 * 
	 * @param block a BasicBlock
	 * @return BitSet of the unique IDs of all blocks that dominate
	 *         (or postdominate) the BasicBlock
	 */
	public BitSet getAllDominatorsOf(BasicBlock block) {
		return getResultFact(block);
	}

	/**
	 * Get a bitset containing the unique IDs of 
	 * all blocks in CFG dominated (or postdominated, depending
	 * on how the analysis was done) by given block.
	 *
	 * @param dominator we want to get all blocks dominated (or postdominated)
	 *                  by this block
	 * @return BitSet of the ids of all blocks dominated by the given block
	 */
	public BitSet getAllDominatedBy(BasicBlock dominator) {
		BitSet allDominated = new BitSet();
		for (Iterator<BasicBlock> i = cfg.blockIterator(); i.hasNext();) {
			BasicBlock block = i.next();
			BitSet dominators = getResultFact(block);
			if (dominators.get(dominator.getId()))
				allDominated.set(block.getId());
		}
		return allDominated;
	}

}

// vim:ts=4
