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

import org.apache.bcel.generic.*;

/**
 * Family of dataflow analyses for counting the number of locks held
 * at points in a method.  Subclasses just need to override
 * the initEntryFact() and getDelta() methods.
 *
 * @author David Hovemeyer
 * @see Dataflow
 * @see DataflowAnalysis
 * @see LockCount
 */
public abstract class LockCountAnalysis extends ForwardDataflowAnalysis<LockCount> {

	private static final boolean DEBUG = Boolean.getBoolean("dataflow.debug");

	protected final MethodGen methodGen;
	protected final ValueNumberDataflow vnaDataflow;

	/**
	 * Constructor.
	 *
	 * @param methodGen   method being analyzed
	 * @param vnaDataflow the Dataflow object used to execute ValueNumberAnalysis on the method
	 * @param dfs         DepthFirstSearch on the method
	 */
	public LockCountAnalysis(MethodGen methodGen, ValueNumberDataflow vnaDataflow, DepthFirstSearch dfs) {
		super(dfs);
		this.methodGen = methodGen;
		this.vnaDataflow = vnaDataflow;
	}

	public boolean isThisValue(ValueNumber valNum) {
		return vnaDataflow.getAnalysis().isThisValue(valNum);
	}

	public LockCount createFact() {
		return new LockCount(LockCount.BOTTOM);
	}

	public void copy(LockCount source, LockCount dest) {
		dest.setCount(source.getCount());
	}

	public void initResultFact(LockCount result) {
		result.setCount(LockCount.TOP);
	}

	public void makeFactTop(LockCount fact) {
		fact.setCount(LockCount.TOP);
	}

	public boolean isFactValid(LockCount fact) {
		return !fact.isTop() && !fact.isBottom();
	}

	public boolean same(LockCount fact1, LockCount fact2) {
		return fact1.getCount() == fact2.getCount();
	}

	public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, LockCount fact) throws DataflowAnalysisException {
		Instruction ins = handle.getInstruction();

		// Optimization: don't even bother with instructions
		// other than MONITORENTER and MONITOREXIT
		if (!(ins instanceof MONITORENTER || ins instanceof MONITOREXIT))
			return;

		// Get the ValueNumberFrame representing values in the stack frame.
		// (Note that null is returned if we are analyzing a static method.)
		ValueNumberFrame frame = getFrame(handle, basicBlock);

		// Get the lock count delta for the instruction
		int delta = getDelta(ins, frame);
		int count = fact.getCount() + delta;
		if (count < 0)
			throw new DataflowAnalysisException("lock count going negative! " + ins);
		fact.setCount(count);
	}

	private ValueNumberFrame getFrame(InstructionHandle handle, BasicBlock basicBlock) {
		ValueNumberFrame result = null;
		if (vnaDataflow != null)
			result = vnaDataflow.getFactAtLocation(new Location(handle, basicBlock));
		return result;
	}

	public void meetInto(LockCount fact, Edge edge, LockCount result) throws DataflowAnalysisException {
		// Standard lattice thing
		if (fact.isTop() || result.isBottom())
			; // no change
		else if (result.isTop())
			result.setCount(fact.getCount());
		else if (fact.isBottom())
			result.setCount(LockCount.BOTTOM);
		else if (fact.getCount() == result.getCount())
			; // no change
		else
			result.setCount(LockCount.BOTTOM);
	}

	/**
	 * Get the lock count delta resulting from the execution of the given instruction.
	 *
	 * @param ins   the instruction
	 * @param frame the ValueNumberFrame representing the values in the Java stack
	 *              frame at the point in the control-flow graph before the instruction
	 */
	public abstract int getDelta(Instruction ins, ValueNumberFrame frame) throws DataflowAnalysisException;

}

// vim:ts=4
