/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003, University of Maryland
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

package edu.umd.cs.daveho.ba;

import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * A dataflow analysis to produce LockSets for all points in a Java method.
 * The intent here is to have a lock count associated with every
 * ValueNumber produced by ValueNumberAnalysis.
 *
 * @see LockSet
 * @see ValueNumberAnalysis
 * @author David Hovemeyer
 */
public class LockSetAnalysis extends ForwardDataflowAnalysis<LockSet> {
	private MethodGen methodGen;
	private ValueNumberDataflow valueNumberDataflow;

	public LockSetAnalysis(MethodGen methodGen, ValueNumberDataflow valueNumberDataflow) {
		this.methodGen = methodGen;
		this.valueNumberDataflow = valueNumberDataflow;
	}

	public LockSet createFact() {
		return new LockSet(valueNumberDataflow.getAnalysis().getNumValuesAllocated());
	}

	public void copy(LockSet source, LockSet dest) {
		dest.copyFrom(source);
	}

	public void initEntryFact(LockSet result) {
		result.makeZero();

		// Set lock count of "this value" to 1 for synchronized
		// instance methods.
		if (methodGen.isSynchronized() && !methodGen.isStatic()) {
			ValueNumber thisValue = valueNumberDataflow.getAnalysis().getEntryValue(0);
			result.setCount(thisValue.getNumber(), 1);
		}
	}

	public void initResultFact(LockSet result) {
		result.makeTop();
	}

	public void makeFactTop(LockSet fact) {
		fact.makeTop();
	}

	public boolean same(LockSet fact1, LockSet fact2) {
		return fact1.sameAs(fact2);
	}

	public void meetInto(LockSet fact, Edge edge, LockSet result) throws DataflowAnalysisException {
		result.mergeWith(fact);
	}

	public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, LockSet fact) throws DataflowAnalysisException {
		Instruction ins = handle.getInstruction();

		if (ins instanceof MONITORENTER) {
			lockOperation(handle, basicBlock, fact, 1);
		} else if (ins instanceof MONITOREXIT) {
			lockOperation(handle, basicBlock, fact, -1);
		}
	}

	private void lockOperation(InstructionHandle handle, BasicBlock basicBlock, LockSet fact, int delta)
		throws DataflowAnalysisException {
		// See what's on the top of the stack
		Location location = new Location(handle, basicBlock);
		ValueNumberFrame frame = valueNumberDataflow.getFactAtLocation(location);
		ValueNumber topOfStack = frame.getTopValue();
		int valNum = topOfStack.getNumber();
		int lockCount = fact.getCount(valNum);
		if (lockCount == LockSet.TOP)
			throw new IllegalStateException("transferring top value!");
		else if (lockCount != LockSet.BOTTOM)
			fact.setCount(valNum, lockCount + delta);
	}

	public boolean isFactValid(LockSet fact) {
		// We consider all LockSets valid.
		return true;
	}

}

// vim:ts=4
