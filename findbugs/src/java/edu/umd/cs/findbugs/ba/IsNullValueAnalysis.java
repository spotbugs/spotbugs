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
import org.apache.bcel.classfile.Method;
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

	public IsNullValueAnalysis(MethodGen methodGen, CFG cfg, ValueNumberDataflow vnaDataflow, DepthFirstSearch dfs) {
		super(dfs);
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

	private static final BitSet nullComparisonInstructionSet = new BitSet();
	static {
		nullComparisonInstructionSet.set(Constants.IFNULL);
		nullComparisonInstructionSet.set(Constants.IFNONNULL);
		nullComparisonInstructionSet.set(Constants.IF_ACMPEQ);
		nullComparisonInstructionSet.set(Constants.IF_ACMPNE);
	}

	private static final IsNullValue ifNullComparison(short opcode, int edgeType, IsNullValue conditionValue) {
		if (opcode == Constants.IFNULL || opcode == Constants.IF_ACMPEQ) {
			// Null on IFCMP_EDGE, non-null on FALL_THROUGH_EDGE
			return edgeType == IFCMP_EDGE
				? IsNullValue.flowSensitiveNullValue(conditionValue)
				: IsNullValue.flowSensitiveNonNullValue(conditionValue);
		} else /*if (opcode == Constants.IFNONNULL || opcode == Constants.IF_ACMPNE)*/  {
			// Non-null on IFCMP_EDGE, null on FALL_THROUGH_EDGE
			return edgeType == IFCMP_EDGE
				? IsNullValue.flowSensitiveNonNullValue(conditionValue)
				: IsNullValue.flowSensitiveNullValue(conditionValue);
		}
	}

	public void meetInto(IsNullValueFrame fact, Edge edge, IsNullValueFrame result)
		throws DataflowAnalysisException {

		if (fact.isValid()) {
			IsNullValueFrame tmpFact = null;

			final int numSlots = fact.getNumSlots();

			if (!NO_SPLIT_DOWNGRADE_NSP) {
				// Downgrade NSP to DNR on non-exception control splits
				if (!edge.isExceptionEdge() && numNonExceptionSuccessorMap[edge.getSource().getId()] > 1) {
					tmpFact = modifyFrame(fact, tmpFact);

					for (int i = 0; i < numSlots; ++i) {
						IsNullValue value = tmpFact.getValue(i);
						if (value.equals(IsNullValue.nullOnSomePathValue()))
							tmpFact.setValue(i, IsNullValue.doNotReportValue());
					}
				}
			}

			final BasicBlock destBlock = edge.getDest();

			if (destBlock.isExceptionHandler()) {
				// Exception handler - clear stack and push a non-null value
				// to represent the exception.
				tmpFact = modifyFrame(fact, tmpFact);
				tmpFact.clearStack();

				if (!ClassContext.PRUNE_INFEASIBLE_EXCEPTION_EDGES) {
					// Downgrade to DNR if the handler is for CloneNotSupportedException
					CodeExceptionGen handler = destBlock.getExceptionGen();
					ObjectType catchType = handler.getCatchType();
					if (catchType != null) {
						String catchClass = catchType.getClassName();
						if (catchClass.equals("java.lang.CloneNotSupportedException") ||
							catchClass.equals("java.lang.InterruptedException")) {
							for (int i = 0; i < tmpFact.getNumSlots(); ++i)
								if (tmpFact.getValue(i).isDefinitelyNull())
									tmpFact.setValue(i, IsNullValue.doNotReportValue());
						}
					}
				}

				// Mark all values as having occurred on an exception path
				for (int i = 0; i < tmpFact.getNumSlots(); ++i)
					tmpFact.setValue(i, tmpFact.getValue(i).toExceptionValue());

				// Push the exception value
				tmpFact.pushValue(IsNullValue.nonNullValue());
			} else {
				// Determine if the edge conveys any information about the
				// null/non-null status of operands in the incoming frame.

				final BasicBlock sourceBlock = edge.getSource();
				final InstructionHandle lastInSourceHandle = sourceBlock.getLastInstruction();
				final int edgeType = edge.getType();

				// Handle IFNULL, IFNONNULL, IF_ACMPEQ, and IF_ACMPNE to
				// produce flow-sensitive information about whether or not the
				// compared value or values were null.
				if (lastInSourceHandle != null) {
					short lastInSourceOpcode = lastInSourceHandle.getInstruction().getOpcode();
					if (nullComparisonInstructionSet.get(lastInSourceOpcode)) {
						// Get ValueNumberFrame and IsNullValueFrame at location
						// just before the IF instruction in the source block.
						final Location atIf = new Location(lastInSourceHandle, sourceBlock);
						final IsNullValueFrame prevIsNullValueFrame = getFactAtLocation(atIf);
						final ValueNumberFrame prevVnaFrame = vnaDataflow.getFactAtLocation(atIf);
						final int prevNumSlots = prevIsNullValueFrame.getNumSlots();
						final IsNullValue conditionValue = prevIsNullValueFrame.getTopValue();

						switch (lastInSourceOpcode) {
						case Constants.IFNULL:
						case Constants.IFNONNULL:
							{
								tmpFact = replaceValues(fact, tmpFact, prevVnaFrame.getTopValue(), prevVnaFrame,
									ifNullComparison(lastInSourceOpcode, edgeType, conditionValue));
							}
							break;
						case Constants.IF_ACMPEQ:
						case Constants.IF_ACMPNE:
							{
								IsNullValue tos = prevIsNullValueFrame.getValue(prevNumSlots - 1);
								IsNullValue nextToTOS = prevIsNullValueFrame.getValue(prevNumSlots - 2);

								if (tos.isDefinitelyNull()) {
									// TOS is null, so next-to-TOS is flow-sensitively null
									tmpFact = replaceValues(fact, tmpFact, prevVnaFrame.getValue(prevNumSlots-2), prevVnaFrame,
										ifNullComparison(lastInSourceOpcode, edgeType, conditionValue));
								}

								if (nextToTOS.isDefinitelyNull()) {
									// Next-to-TOS is null, so TOS is flow-sensitively null
									tmpFact = replaceValues(fact, tmpFact, prevVnaFrame.getTopValue(), prevVnaFrame,
										ifNullComparison(lastInSourceOpcode, edgeType, conditionValue));
								}
							}
							break;
						}
					}
				}

				// If this is a fall-through edge from a null check,
				// then we know the value checked is not null.
				if (sourceBlock.isNullCheck() && edgeType == FALL_THROUGH_EDGE) {
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

					tmpFact = replaceValues(fact, tmpFact, replaceMe, vnaFrame, IsNullValue.nonNullValue());
				}
			}

			if (tmpFact != null)
				fact = tmpFact;
		}

		// Normal dataflow merge
		result.mergeWith(fact);
	}

	private IsNullValueFrame replaceValues(IsNullValueFrame origFrame, IsNullValueFrame frame,
		ValueNumber replaceMe, ValueNumberFrame vnaFrame, IsNullValue replacementValue) {

		// If required, make a copy of the frame
		frame = modifyFrame(origFrame, frame);

		// The VNA frame may have more slots than the IsNullValueFrame
		// if it was produced by an IF comparison (whose operand or operands
		// are subsequently popped off the stack).

		final int numSlots = Math.min(frame.getNumSlots(), vnaFrame.getNumSlots());

		for (int i = 0; i < numSlots; ++i) {
			if (vnaFrame.getValue(i).equals(replaceMe))
				frame.setValue(i, replacementValue);
		}

		return frame;

	}

	/**
	 * Test driver.
	 */
	public static void main(String[] argv) throws Exception {
		if (argv.length != 1) {
			System.err.println("Usage: " + IsNullValueAnalysis.class.getName() + " <class file>");
			System.exit(1);
		}

		DataflowTestDriver<IsNullValueFrame, IsNullValueAnalysis> driver = new DataflowTestDriver<IsNullValueFrame, IsNullValueAnalysis>() {
			public Dataflow<IsNullValueFrame, IsNullValueAnalysis> createDataflow(ClassContext classContext, Method method)
				throws CFGBuilderException, DataflowAnalysisException {

				return classContext.getIsNullValueDataflow(method);
			}
		};

		driver.execute(argv[0]);
	}

}

// vim:ts=4
