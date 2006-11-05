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

import org.apache.bcel.generic.InstructionHandle;
import edu.umd.cs.findbugs.annotations.*;

/**
 * A dataflow analysis to be used with the {@link Dataflow} class.
 *
 * @author David Hovemeyer
 * @see Dataflow
 */
public interface DataflowAnalysis <Fact> {
	/**
	 * Create empty (uninitialized) dataflow facts for one program point.
	 * A valid value will be copied into it before it is used.
	 */
	public Fact createFact();

	
	/**
	 * Get the start fact for given basic block.
	 *
	 * @param block the basic block
	 */
	public Fact getStartFact(BasicBlock block);

	/**
	 * Get the result fact for given basic block.
	 *
	 * @param block the basic block
	 */
	public Fact getResultFact(BasicBlock block);

	/**
	 * Copy dataflow facts.
	 */
	public void copy(Fact source, Fact dest);

	/**
	 * Initialize the "entry" fact for the graph.
	 */
	public void initEntryFact(Fact result) throws DataflowAnalysisException;

	/**
	 * Initialize result fact for block.
	 * The start facts for a block are initialized as the meet of the
	 * "logical" predecessor's result facts.  Note that a "logical predecessor"
	 * is actually a CFG successor if the analysis is backwards.
	 */
	public void initResultFact(Fact result);

	/**
	 * Make given fact the top value.
	 */
	public void makeFactTop(Fact fact);

	/**
	 * Is the given fact the top value.
	 */
	public boolean isTop(Fact fact);
	/**
	 * Returns true if the analysis is forwards, false if backwards.
	 */
	public boolean isForwards();

	/**
	 * Return the BlockOrder specifying the order in which BasicBlocks
	 * should be visited in the main dataflow loop.
	 *
	 * @param cfg the CFG upon which we're performing dataflow analysis
	 */
	public BlockOrder getBlockOrder(CFG cfg);

	/**
	 * Are given dataflow facts the same?
	 */
	public boolean same(Fact fact1, Fact fact2);

	/**
	 * Transfer function for the analysis.
	 * Taking dataflow facts at start (which might be either the entry or
	 * exit of the block, depending on whether the analysis is forwards
	 * or backwards), modify result to be the facts at the other end
	 * of the block.
	 *
	 * @param basicBlock the basic block
	 * @param end        if nonnull, stop before considering this instruction;
	 *                   otherwise, consider all of the instructions in the basic block
	 * @param start      dataflow facts at beginning of block (if forward analysis)
	 *                   or end of block (if backwards analysis)
	 * @param result     resulting dataflow facts at other end of block
	 */
	public void transfer(BasicBlock basicBlock, @CheckForNull InstructionHandle end, Fact start, Fact result) throws DataflowAnalysisException;

	/**
	 * Edge transfer function.
	 * Modify the given fact that is true on the (logical) edge source
	 * to modify it so that it is true at the (logical) edge target.
	 * 
	 * <p>
	 * A do-nothing implementation is legal, and appropriate for
	 * analyses where branches are not significant.
	 * </p>
	 * 
	 * @param edge the Edge
	 * @param fact a dataflow fact
	 * @throws DataflowAnalysisException
	 */
	public void edgeTransfer(Edge edge, Fact fact) throws DataflowAnalysisException;
	
	/**
	 * Meet a dataflow fact associated with an incoming edge into another fact.
	 * This is used to determine the start fact for a basic block.
	 *
	 * @param fact   the predecessor fact (incoming edge)
	 * @param edge   the edge from the predecessor
	 * @param result the result fact
	 */
	public void meetInto(Fact fact, Edge edge, Fact result) throws DataflowAnalysisException;
	
	/**
	 * Called before beginning an iteration of analysis.
	 * Each iteration visits every basic block in the CFG.
	 */
	public void startIteration();

	/**
	 * Called after finishing an iteration of analysis. 
	 */
	public void finishIteration();
	
	public int getLastUpdateTimestamp(Fact fact);
	
	public void setLastUpdateTimestamp(Fact fact, int timestamp);
}

// vim:ts=4
