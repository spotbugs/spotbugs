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

import java.util.*;
import org.apache.bcel.Constants;
import org.apache.bcel.generic.*;

/**
 * A dataflow analysis to detect potential null pointer dereferences.
 * TODO: this should really be a base class to allow deriving other
 * analyses.  I.e., right now we only keep track of when values
 * are definitely null on some incoming path; that approach eliminates
 * false positives.  Another approach would be to keep track of whether
 * they <em>might</em> be null on some incoming path, which could produce
 * false positives.
 *
 * @see IsNullValue
 * @author David Hovemeyer
 */
public class IsNullValueAnalysis extends ForwardDataflowAnalysis<IsNullValueFrame> implements EdgeTypes {

	private MethodGen methodGen;
	private CFG cfg;
	private ValueNumberDataflow vnaDataflow;
	private IdentityHashMap<BasicBlock, Integer> nonExceptionSuccCountMap;

	public IsNullValueAnalysis(MethodGen methodGen, CFG cfg, ValueNumberDataflow vnaDataflow) {
		this.methodGen = methodGen;
		this.cfg = cfg;
		this.vnaDataflow = vnaDataflow;
		this.nonExceptionSuccCountMap = new IdentityHashMap<BasicBlock, Integer>();
	}

	public IsNullValueFrame createFact() {
		return new IsNullValueFrame(methodGen.getMaxLocals());
	}

	public void copy(IsNullValueFrame source, IsNullValueFrame dest) {
		dest.copyFrom(source);
	}

	public void initEntryFact(IsNullValueFrame result) {
		result.setValid();
		int numLocals = methodGen.getMaxLocals();
		for (int i = 0; i < numLocals; ++i)
			result.setValue(i, IsNullValue.notDefinitelyNull());
	}

	public void initResultFact(IsNullValueFrame result) {
		result.setTop();
	}

	public void makeFactTop(IsNullValueFrame fact) {
		fact.setTop();
	}

	public boolean isFactValid(IsNullValueFrame fact) {
		return fact.isValid();
	}

	public boolean same(IsNullValueFrame fact1, IsNullValueFrame fact2) {
		return fact1.sameAs(fact2);
	}

	public void transfer(BasicBlock basicBlock, InstructionHandle end, IsNullValueFrame start, IsNullValueFrame result)
		throws DataflowAnalysisException {

		// This will call transferInstruction() for all of the instructions
		// in the basic block.
		super.transfer(basicBlock, end, start, result);

		// Special case: if this block has multiple non-exception successors,
		// we downgrade "definitely null on some path" values to
		// "not definitely null".  This should eliminate false positives
		// due to (non-exception) infeasible paths.  Note that in the case of
		// IFNULL and IFNONNULL branches, we will recover more precise
		// information later on in the meetInto() method.
		if (getNumNonExceptionSuccessors(basicBlock) > 1) {
			// TODO: figure out whether exception edges should really be ignored
			int numSlots = result.getNumSlots();
			for (int i = 0; i < numSlots; ++i) {
				IsNullValue value = result.getValue(i);
				if (value == IsNullValue.definitelyNullOnSomePath())
					result.setValue(i, IsNullValue.notDefinitelyNull());
			}
		}
	}

	private int getNumNonExceptionSuccessors(BasicBlock basicBlock) {
		Integer count = nonExceptionSuccCountMap.get(basicBlock);
		if (count == null) {
			int nonExceptionSuccCount = 0;
			Iterator<Edge> i = cfg.outgoingEdgeIterator(basicBlock);
			while (i.hasNext()) {
				Edge edge = i.next();
				int edgeType = edge.getType();
				if (edgeType != EdgeTypes.UNHANDLED_EXCEPTION_EDGE && edgeType != EdgeTypes.HANDLED_EXCEPTION_EDGE)
					++nonExceptionSuccCount;
			}
			count = new Integer(nonExceptionSuccCount);
			nonExceptionSuccCountMap.put(basicBlock, count);
		}
		return count.intValue();
	}

	public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, IsNullValueFrame fact)
		throws DataflowAnalysisException {

		// TODO: implement

	}

	/** Set of instruction opcodes that have an implicit null check. */
	private static final BitSet nullCheckInstructionSet = new BitSet();
	static {
		nullCheckInstructionSet.set(Constants.GETFIELD);
		nullCheckInstructionSet.set(Constants.PUTFIELD);
		nullCheckInstructionSet.set(Constants.INVOKESPECIAL);
		nullCheckInstructionSet.set(Constants.INVOKEVIRTUAL);
		nullCheckInstructionSet.set(Constants.INVOKEINTERFACE);
		nullCheckInstructionSet.set(Constants.AALOAD);
		nullCheckInstructionSet.set(Constants.AASTORE);
		nullCheckInstructionSet.set(Constants.BALOAD);
		nullCheckInstructionSet.set(Constants.BASTORE);
		nullCheckInstructionSet.set(Constants.CALOAD);
		nullCheckInstructionSet.set(Constants.CASTORE);
		nullCheckInstructionSet.set(Constants.DALOAD);
		nullCheckInstructionSet.set(Constants.DASTORE);
		nullCheckInstructionSet.set(Constants.FALOAD);
		nullCheckInstructionSet.set(Constants.FASTORE);
		nullCheckInstructionSet.set(Constants.IALOAD);
		nullCheckInstructionSet.set(Constants.IASTORE);
		nullCheckInstructionSet.set(Constants.LALOAD);
		nullCheckInstructionSet.set(Constants.LASTORE);
		nullCheckInstructionSet.set(Constants.SALOAD);
		nullCheckInstructionSet.set(Constants.SASTORE);
		nullCheckInstructionSet.set(Constants.MONITORENTER);
		nullCheckInstructionSet.set(Constants.MONITOREXIT);
		// Any others?
	}

	public void meetInto(IsNullValueFrame fact, Edge edge, IsNullValueFrame result) throws DataflowAnalysisException {
		// Normal dataflow merge
		result.mergeWith(fact);

		// If we're at TOP or BOTTOM after merge, nothing more to do
		if (!result.isValid())
			return;

		final int numSlots = result.getNumSlots();

		// If we have a single predecessor then we can use the following
		// information to make the result frame more precise:
		//   - the type of edge
		//   - the last instruction in the predecessor block
		//   - the first instruction in the successor block (i.e., if the
		//     predecessor is the exception throwing block (ETB) for
		//     the instruction; for example, the null check for a field access)
		BasicBlock dest = edge.getDest();
		if (dest.getNumIncomingEdges() == 1) {
			BasicBlock source = edge.getSource();

			int edgeType = edge.getType();
			if (edgeType == IFCMP_EDGE || edgeType == FALL_THROUGH_EDGE) {
				// Check for IFNULL and IFNONNULL branches.
				// This will tell us something about the value on the
				// top of the Java operand stack.
				InstructionHandle lastInSourceHandle = source.getLastInstruction();
				if (lastInSourceHandle == null)
					return;

				Instruction lastInSource = lastInSourceHandle.getInstruction();
				short opcode = lastInSource.getOpcode();

				// If the last instruction was IFNULL or IFNONNULL
				// then we have more precise information about the value
				// on the top of the stack.
				IsNullValue newTOS = null;
				if (opcode == Constants.IFNULL) {
					newTOS = (edgeType == IFCMP_EDGE) ? IsNullValue.definitelyNull() : IsNullValue.notDefinitelyNull();
				} else if (opcode == Constants.IFNONNULL) {
					newTOS = (edgeType == IFCMP_EDGE) ? IsNullValue.notDefinitelyNull() : IsNullValue.definitelyNull();
				}

				if (newTOS != null) {
					// Get value number frame at start of dest block.
					ValueNumberFrame vnaFrame = getVnaFrameAtEntry(dest);
					if (vnaFrame == null)
						return;
					assert vnaFrame.getNumSlots() == numSlots;

					// Get the value number of the value on top of the stack.
					// All occurrences of that value in the frame can
					// be updated to reflect the more precise null/non-null
					// information.
					ValueNumber oldTOS = vnaFrame.getTopValue();

					for (int i = 0; i < numSlots; ++i) {
						if (vnaFrame.getValue(i).equals(oldTOS))
							result.setValue(i, newTOS);
					}
				}
			} else {
				// Check to see if the first instruction in the destination
				// block is one that has a null check exception associated with it.
				// If so, then at entry to the destination block, we know that
				// the null check has occurred and the value on top of
				// the stack is not null.

				InstructionHandle firstInDestHandle = dest.getFirstInstruction();
				if (firstInDestHandle == null)
					return;
				Instruction firstInDest = firstInDestHandle.getInstruction();
				short opcode = firstInDest.getOpcode();
				if (!nullCheckInstructionSet.get(opcode))
					return;

				ValueNumberFrame vnaFrame = getVnaFrameAtEntry(dest);
				if (vnaFrame == null)
					return;
				assert vnaFrame.getNumSlots() == numSlots;

				// The block starts with an instruction whose implicit null check
				// has succeeded.  Now we need to figure out the stack slot which
				// contains the reference that was checked for null.
				// This is extremely easy.  We just find out how many
				// slots the instruction consumes, and subtract that value from
				// the number of slots in the frame.
				int numSlotsConsumed = firstInDest.consumeStack(methodGen.getConstantPool());
				if (numSlotsConsumed == Constants.UNPREDICTABLE)
					throw new DataflowAnalysisException("Unpredictable stack consumption for " + firstInDest);
				ValueNumber checkedValue = vnaFrame.getValue(numSlots - numSlotsConsumed);

				// Now we know that all occurrences of the checked value
				// are not null.
				for (int i = 0; i < numSlots; ++i) {
					if (vnaFrame.getValue(i).equals(checkedValue))
						result.setValue(i, IsNullValue.notDefinitelyNull());
				}
			}
		}
	}

	private ValueNumberFrame getVnaFrameAtEntry(BasicBlock block) {
		InstructionHandle first = block.getFirstInstruction();
		if (first == null)
			return null;
		return vnaDataflow.getFactAtLocation(new Location(first, block));
	}

}

// vim:ts=4
