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
public class IsNullValueAnalysis extends FrameDataflowAnalysis<IsNullValue, IsNullValueFrame> implements EdgeTypes {
	private static final boolean DEBUG = Boolean.getBoolean("inva.debug");
	private static final boolean NO_SPLIT_DOWNGRADE_NSP = Boolean.getBoolean("inva.noSplitDowngradeNSP");

	private MethodGen methodGen;
	private IsNullValueFrameModelingVisitor visitor;
	private ValueNumberDataflow vnaDataflow;
	private int[] numNonExceptionSuccessorMap;

	public IsNullValueAnalysis(MethodGen methodGen, CFG cfg, ValueNumberDataflow vnaDataflow) {
		this.methodGen = methodGen;
		this.visitor = new IsNullValueFrameModelingVisitor(methodGen.getConstantPool());
		this.vnaDataflow = vnaDataflow;
		this.numNonExceptionSuccessorMap = new int[cfg.getNumBasicBlocks()];

		// For each basic block, calculate the number of non-exception successors.
		Iterator<Edge> i = cfg.edgeIterator();
		while (i.hasNext()) {
			Edge edge = i.next();
			if (edge.isExceptionEdge())
				continue;
			int srcBlockId = edge.getSource().getId();
			numNonExceptionSuccessorMap[srcBlockId]++;
		}
	}

	public IsNullValueFrame createFact() {
		return new IsNullValueFrame(methodGen.getMaxLocals());
	}

	public void initEntryFact(IsNullValueFrame result) {
		result.setValid();
		int numLocals = methodGen.getMaxLocals();
		for (int i = 0; i < numLocals; ++i)
			result.setValue(i, IsNullValue.doNotReportValue());
	}

	public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, IsNullValueFrame fact)
		throws DataflowAnalysisException {

		visitor.setFrame(fact);
		handle.getInstruction().accept(visitor);

	}

	public void meetInto(IsNullValueFrame fact, Edge edge, IsNullValueFrame result)
		throws DataflowAnalysisException {

		if (fact.isValid()) {
			final int numSlots = fact.getNumSlots();

			if (!NO_SPLIT_DOWNGRADE_NSP) {
				// Downgrade NSP to DNR on non-exception control splits
				if (!edge.isExceptionEdge() && numNonExceptionSuccessorMap[edge.getSource().getId()] > 1) {
					IsNullValueFrame tmpFact = createFact();
					tmpFact.copyFrom(fact);
					for (int i = 0; i < numSlots; ++i) {
						IsNullValue value = tmpFact.getValue(i);
						if (value.equals(IsNullValue.nullOnSomePathValue()))
							tmpFact.setValue(i, IsNullValue.doNotReportValue());
					}
					fact = tmpFact;
				}
			}

			final BasicBlock destBlock = edge.getDest();

			if (destBlock.isExceptionHandler()) {
				// Exception handler - clear stack and push a non-null value
				// to represent the exception.
				IsNullValueFrame tmpFrame = createFact();
				tmpFrame.copyFrom(fact);
				tmpFrame.clearStack();

				// Mark all values as having occurred on an exception path
				for (int i = 0; i < tmpFrame.getNumSlots(); ++i)
					tmpFrame.setValue(i, tmpFrame.getValue(i).toExceptionValue());

				// Push the exception value
				tmpFrame.pushValue(IsNullValue.nonNullValue());
				fact = tmpFrame;
			} else {
				// Determine if the edge conveys any information about the
				// null/non-null status of operands in the incoming frame.
	
				int nullInfo = getNullInfoFromEdge(edge);

				switch (nullInfo) {
				case TOS_NULL:
				case TOS_NON_NULL:
				case NEXT_TO_TOS_NULL:
				case NEXT_TO_TOS_NON_NULL:
					{
						// What we know here is that the value that was
						// just popped off the stack is either null or non-null.
						// We can use this info to increase the precision of
						// any stack slots containing the same value.

						// Get ValueNumberFrame and IsNullValueFrame at location
						// just before the IF instruction in the source block.
						BasicBlock sourceBlock = edge.getSource();
						Location atIf = new Location(sourceBlock.getLastInstruction(), sourceBlock);
						IsNullValueFrame prevIsNullValueFrame = getFactAtLocation(atIf);
						ValueNumberFrame prevVnaFrame = vnaDataflow.getFactAtLocation(atIf);

						int prevNumSlots = prevIsNullValueFrame.getNumSlots();
						assert prevNumSlots == prevVnaFrame.getNumSlots();

						// Figure out which slot contains the value we have information about
						int slotToReplace = (nullInfo == TOS_NULL || nullInfo == TOS_NON_NULL)
							? prevNumSlots - 1
							: prevNumSlots - 2;

						// Get the value we have information about, as well as the
						// condition the IF statement is controlled by.
						ValueNumber replaceMe = prevVnaFrame.getValue(slotToReplace);
						IsNullValue origIsNullValue = prevIsNullValueFrame.getValue(slotToReplace);

						// Update all slots containing the value which was used
						// in the IF statement.
						fact = replaceValues(fact, replaceMe, prevVnaFrame,
							(nullInfo == TOS_NULL || nullInfo == NEXT_TO_TOS_NULL)
								? IsNullValue.flowSensitiveNullValue(origIsNullValue)
								: IsNullValue.flowSensitiveNonNullValue(origIsNullValue));
					}
					break;
				case REF_OPERAND_NON_NULL:
					{
						ValueNumberFrame vnaFrame = vnaDataflow.getStartFact(destBlock);
						if (vnaFrame == null)
							throw new IllegalStateException("no vna frame at block entry?");

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
				case NO_INFO:
					break;
				default:
					assert false;
				}
			}
		}

		// Normal dataflow merge
		result.mergeWith(fact);
	}

	private static final int TOS_NULL = 0;
	private static final int TOS_NON_NULL = 1;
	private static final int NEXT_TO_TOS_NULL = 2;
	private static final int NEXT_TO_TOS_NON_NULL = 3;
	private static final int REF_OPERAND_NON_NULL = 4;
	private static final int NO_INFO = -1;

	/**
	 * Return a value indicating what information about the null/non-null
	 * status of values in the stack frame is conveyed by the
	 * given edge.  Note that when we talk about top of stack here,
	 * we really mean top of stack at the time of the last IF comparison
	 * (which is the source of the edge).
	 *
	 * @param edge the edge
	 * @return TOS_NULL if the value on top of the stack is null,
	 *   TOS_NON_NULL if the value on top of the stack is non-null,
	 *   NEXT_TO_TOS_NULL if the value next to TOS is null,
	 *   NEXT_TO_TOS_NON_NULL if the value next to TOS is non-null,
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
			else if (opcode == Constants.IF_ACMPEQ || opcode == Constants.IF_ACMPNE) {
				Location atIf = new Location(lastInSourceHandle, edge.getSource());
				IsNullValueFrame frame = getFactAtLocation(atIf);

				int numSlots = frame.getNumSlots();
				IsNullValue tos = frame.getValue(numSlots - 1); // top of stack
				IsNullValue nextToTos = frame.getValue(numSlots - 2); // next to top of stack

				// If one of the values being compared is null, then
				// we learn something about the other one.

				boolean tosNull = tos.isDefinitelyNull();
				boolean nextToTosNull = nextToTos.isDefinitelyNull();

				if (tosNull || nextToTosNull) {
					int[] info = new int[]{
						tosNull ? NEXT_TO_TOS_NULL : TOS_NULL,
						tosNull ? NEXT_TO_TOS_NON_NULL : TOS_NON_NULL
					};

					if (edgeType == IFCMP_EDGE)
						return info[opcode == Constants.IF_ACMPEQ ? 0 : 1];
					else
						return info[opcode == Constants.IF_ACMPEQ ? 1 : 0];
				}
			}
		} else if (edge.getSource().isNullCheck() && edge.getType() == FALL_THROUGH_EDGE) {
			return REF_OPERAND_NON_NULL;
		}

		return NO_INFO;
	}

	private IsNullValueFrame replaceValues(IsNullValueFrame frame, ValueNumber replaceMe, ValueNumberFrame vnaFrame,
		IsNullValue replacementValue) {

		// The VNA frame may have more slots than the IsNullValueFrame
		// if it was produced by an IF comparison (whose operand or operands
		// are subsequently popped off the stack).

		final int numSlots = Math.min(frame.getNumSlots(), vnaFrame.getNumSlots());

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
