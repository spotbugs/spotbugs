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
	private static final boolean DEBUG = Boolean.getBoolean("inva.debug");

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

	public void meetInto(IsNullValueFrame fact, Edge edge, IsNullValueFrame result)
		throws DataflowAnalysisException {

		if (fact.isValid()) {

			final BasicBlock destBlock = edge.getDest();

			if (destBlock.isExceptionHandler()) {
				// Exception handler - clear stack and push a non-null value
				// to represent the exception.
				IsNullValueFrame tmpFrame = createFact();
				tmpFrame.copyFrom(fact);
				tmpFrame.clearStack();
				tmpFrame.pushValue(IsNullValue.nonNullValue());
				fact = tmpFrame;
			} else {
				// Determine if the edge conveys any information about the
				// null/non-null status of operands in the incoming frame.
				final int numSlots = fact.getNumSlots();
	
				int nullInfo = getNullInfoFromEdge(edge);
				if (nullInfo != NO_INFO) {
					// Value numbers in this frame
					ValueNumberFrame vnaFrame = vnaDataflow.getStartFact(destBlock);
					if (vnaFrame == null)
						throw new IllegalStateException("no vna frame at block entry?");

					if (vnaFrame != null) {
						switch (nullInfo) {
						case TOS_NULL:
						case TOS_NON_NULL:
							{
								// What we know here is that the value that was
								// just popped off the stack is either null or non-null.
								// We can use this info to increase the precision of
								// any stack slots containing the same value.

								BasicBlock sourceBlock = edge.getSource();
								Location atIf = new Location(sourceBlock.getLastInstruction(), sourceBlock);
								ValueNumberFrame prevVnaFrame = vnaDataflow.getFactAtLocation(atIf);

								if (prevVnaFrame != null) {
									ValueNumber replaceMe = prevVnaFrame.getTopValue();
									fact = replaceValues(fact, replaceMe, vnaFrame,
										nullInfo == TOS_NULL ? IsNullValue.nullValue() : IsNullValue.nonNullValue());
								} else
									throw new IllegalStateException("No value number frame?");
							}
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
								ValueNumber replaceMe = vnaFrame.getValue(numSlots - numSlotsConsumed);

								fact = replaceValues(fact, replaceMe, vnaFrame, IsNullValue.nonNullValue());
							}
							break;
						default:
							assert false;
						}
					}
				}
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
		} else if (edge.getSource().isNullCheck() && edge.getType() == FALL_THROUGH_EDGE) {
			return REF_OPERAND_NON_NULL;
		}

		return NO_INFO;
	}

	private IsNullValueFrame replaceValues(IsNullValueFrame frame, ValueNumber replaceMe, ValueNumberFrame vnaFrame,
		IsNullValue replacementValue) {

		final int numSlots = frame.getNumSlots();
		assert numSlots == vnaFrame.getNumSlots();

		final IsNullValueFrame result = createFact();
		result.copyFrom(frame);

		for (int i = 0; i < numSlots; ++i) {
			if (vnaFrame.getValue(i).equals(replaceMe))
				result.setValue(i, replacementValue);
		}

		return result;

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
