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

	public IsNullValueAnalysis(MethodGen methodGen, CFG cfg, ValueNumberDataflow vnaDataflow) {
		this.methodGen = methodGen;
		this.cfg = cfg;
		this.vnaDataflow = vnaDataflow;
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
			result.setValue(i, IsNullValue.doNotReportValue());
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

	public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, IsNullValueFrame fact)
		throws DataflowAnalysisException {

		IsNullValueFrameModelingVisitor visitor =
			new IsNullValueFrameModelingVisitor(fact, methodGen.getConstantPool());
		handle.getInstruction().accept(visitor);

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

	public void meetInto(IsNullValueFrame fact, Edge edge, IsNullValueFrame result)
		throws DataflowAnalysisException {

		if (fact.isValid() && result.isValid()) {
			// Determine if the edge conveys any information about the
			// null/non-null status of operands in the incoming frame.
			final int numSlots = result.getNumSlots();
			final BasicBlock destBlock = edge.getDest();

			int nullInfo = getNullInfoFromEdge(edge);
			switch (nullInfo) {
			case TOS_NULL:
				fact = replaceValues(fact, numSlots - 1, destBlock, IsNullValue.nullValue());
				break;
			case TOS_NON_NULL:
				fact = replaceValues(fact, numSlots - 1, destBlock, IsNullValue.nonNullValue());
				break;
			case REF_OPERAND_NON_NULL:
				{
					// For all of the instructions which have a null-checked
					// reference operand, it is pushed onto the stack before
					// all of the other operands to the instruction.
					Instruction firstInDest = edge.getDest().getFirstInstruction().getInstruction();
					int numSlotsConsumed = firstInDest.consumeStack(methodGen.getConstantPool());
					if (numSlotsConsumed == Constants.UNPREDICTABLE)
						throw new DataflowAnalysisException("Unpredictable stack consumption for " + firstInDest);
					fact = replaceValues(fact, numSlots - numSlotsConsumed, destBlock, IsNullValue.nonNullValue());
				}
				break;
			case NO_INFO:
				break;
			default:
				assert false;
			}
		}

		// Normal dataflow merge
		result.mergeWith(fact);
	}

	private static final int TOS_NULL = 0;
	private static final int TOS_NON_NULL = 1;
	private static final int REF_OPERAND_NON_NULL = 2;
	private static final int NO_INFO = -1;

	/**
	 * Return a value indicating what information about the null/non-null
	 * status of values in the stack frame is conveyed by the
	 * given edge.
	 * @param edge the edge
	 * @return TOS_NULL if the value on top of the stack is null,
	 *   TOS_NON_NULL if the value on top of the stack is non-null,
	 *   REF_OPERAND_NON_NULL if the reference operand to the
	 *   first instruction in the destination block is non-null,
	 *   or NO_INFO if the edge conveys no extra information
	 */
	private int getNullInfoFromEdge(Edge edge) {
		final InstructionHandle lastInSourceHandle = edge.getSource().getLastInstruction();
		final int edgeType = edge.getType();

		if (lastInSourceHandle != null) {
			Instruction lastInSource = lastInSourceHandle.getInstruction();
			short opcode = lastInSource.getOpcode();
			if (opcode == Constants.IFNULL)
				return edgeType == IFCMP_EDGE ? TOS_NULL : TOS_NON_NULL;
			else if (opcode == Constants.IFNONNULL)
				return edgeType == IFCMP_EDGE ? TOS_NON_NULL : TOS_NULL;
		} else {
			InstructionHandle firstInDestHandle = edge.getDest().getFirstInstruction();
			if (firstInDestHandle != null) {
				Instruction firstInDest = firstInDestHandle.getInstruction();
				short opcode = firstInDest.getOpcode();
				if (nullCheckInstructionSet.get(opcode))
					return REF_OPERAND_NON_NULL;
			}
		}

		return NO_INFO;
	}

	/**
	 * Replace all values in the frame matching the value in given stack
	 * slot with the given value.
	 * @param frame the original frame
	 * @param stackSlot the stack slot in the frame whose value should be replaced
	 * @param block the basic block whose entry value is represented
	 *   by the frame
	 * @param value the new value
	 * @return the new frame
	 */
	private IsNullValueFrame replaceValues(IsNullValueFrame frame, int stackSlot, BasicBlock block,
		IsNullValue replacementValue) throws DataflowAnalysisException {

		ValueNumberFrame vnaFrame = getVnaFrameAtEntry(block);
		final int numSlots = frame.getNumSlots();

		// Create a new frame for the result,
		// since we don't want to modify the original.
		IsNullValueFrame result = new IsNullValueFrame(numSlots);
		result.copyFrom(frame);

		if (vnaFrame != null) {
			assert numSlots == vnaFrame.getNumSlots();

			// Replace all values which are the same as the one in the slot.
			ValueNumber origValue = vnaFrame.getValue(stackSlot);
			for (int i = 0; i < numSlots; ++i) {
				ValueNumber valueNum = vnaFrame.getValue(i);
				if (valueNum.equals(origValue))
					result.setValue(i, replacementValue);
			}
		} else {
			// Just replace the value in the specified slot, since we don't
			// know the value numbers.
			result.setValue(stackSlot, replacementValue);
		}

		return result;
	}

	/**
	 * Get the ValueNumberFrame at entry to given block.
	 * @param block the block
	 * @return the ValueNumberFrame at entry to the block,
	 *   or null if we have no information at that location
	 */
	private ValueNumberFrame getVnaFrameAtEntry(BasicBlock block) {
		InstructionHandle first = block.getFirstInstruction();
		if (first == null)
			return null;
		return vnaDataflow.getFactAtLocation(new Location(first, block));
	}

	/**
	 * Test driver.
	 */
	public static void main(String[] argv) throws Exception {
		if (argv.length != 1) {
			System.err.println("Usage: " + IsNullValueAnalysis.class.getName() + " <class file>");
			System.exit(1);
		}

		DataflowTestDriver<IsNullValueFrame> driver = new DataflowTestDriver<IsNullValueFrame>() {
			public AbstractDataflowAnalysis<IsNullValueFrame> createAnalysis(MethodGen methodGen, CFG cfg)
				throws DataflowAnalysisException {

				// Create the ValueNumberAnalysis
				ValueNumberAnalysis vna = new ValueNumberAnalysis(methodGen);
				ValueNumberDataflow vnaDataflow = new ValueNumberDataflow(cfg, vna);
				vnaDataflow.execute();

				IsNullValueAnalysis analysis = new IsNullValueAnalysis(methodGen, cfg, vnaDataflow);
				return analysis;
			}
		};

		driver.execute(argv[0]);
	}

}

// vim:ts=4
