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

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.*;

/**
 * A dataflow analysis to detect potential null pointer dereferences.
 *
 * @author David Hovemeyer
 * @see IsNullValue
 * @see IsNullValueFrame
 * @see IsNullValueFrameModelingVisitor
 */
public class IsNullValueAnalysis extends FrameDataflowAnalysis<IsNullValue, IsNullValueFrame> implements EdgeTypes {
	private static final boolean DEBUG = Boolean.getBoolean("inva.debug");

	static {
		if (DEBUG) System.out.println("Debug enabled");
	}

	private static final boolean NO_SPLIT_DOWNGRADE_NSP = Boolean.getBoolean("inva.noSplitDowngradeNSP");
	private static final boolean NO_SWITCH_DEFAULT_AS_EXCEPTION = Boolean.getBoolean("inva.noSwitchDefaultAsException");

	/**
	 * If this property is true, then we assume parameters
	 * and return values can be null (but aren't definitely null).
	 */
	static final boolean UNKNOWN_VALUES_ARE_NSP =
		Boolean.getBoolean("findbugs.nullderef.assumensp");

	private MethodGen methodGen;
	private IsNullValueFrameModelingVisitor visitor;
	private ValueNumberDataflow vnaDataflow;
	private int[] numNonExceptionSuccessorMap;
	private IsNullValueFrame lastFrame;

	public IsNullValueAnalysis(MethodGen methodGen, CFG cfg, ValueNumberDataflow vnaDataflow, DepthFirstSearch dfs,
	                           AssertionMethods assertionMethods) {
		super(dfs);
		this.methodGen = methodGen;
		this.visitor = new IsNullValueFrameModelingVisitor(methodGen.getConstantPool(), assertionMethods);
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
		boolean instanceMethod = !methodGen.isStatic();

		for (int i = 0; i < numLocals; ++i) {
			IsNullValue paramValue;

			if (UNKNOWN_VALUES_ARE_NSP && !(instanceMethod && i == 0))
				paramValue = IsNullValue.nullOnSomePathValue();
			else
				paramValue = IsNullValue.nonReportingNotNullValue();

			result.setValue(i, paramValue);
		}
	}

/*
	// FIXME: because of a bug in the 2.2 generics-enabled javac,
	// we can't override this method.  Javac doesn't emit the needed
	// bridge method.
	public void transfer(BasicBlock basicBlock, InstructionHandle end, IsNullValueFrame start, IsNullValueFrame result)
		throws DataflowAnalysisException {

		lastFrame = null;
		super.transfer(basicBlock, end, start, result);

		// Determine if this basic block ends in a redundant branch.
		if (end == null) {
			if (lastFrame == null)
				result.setDecision(null);
			else {
				IsNullConditionDecision decision = getDecision(basicBlock, lastFrame);
				result.setDecision(decision);
			}
		}
	}
*/

	// FIXME: This is a workaround for the generics-java bug.
	public void startTransfer(BasicBlock basicBlock, Object start_) throws DataflowAnalysisException {
		lastFrame = null;
	}

	// FIXME: This is a workaround for the generics-java bug.
	public void endTransfer(BasicBlock basicBlock, InstructionHandle end, Object result_) throws DataflowAnalysisException {
		IsNullValueFrame result = (IsNullValueFrame) result_;

		// Determine if this basic block ends in a redundant branch.
		if (end == null) {
			if (lastFrame == null)
				result.setDecision(null);
			else {
				IsNullConditionDecision decision = getDecision(basicBlock, lastFrame);
				if (DEBUG) System.out.println("Decision=" + decision);
				result.setDecision(decision);
			}
		}
	}

	public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, IsNullValueFrame fact)
	        throws DataflowAnalysisException {

		// If this is the last instruction in the block,
		// save the result immediately before the instruction.
		if (handle == basicBlock.getLastInstruction()) {
			lastFrame = createFact();
			lastFrame.copyFrom(fact);
		}

		// Model the instruction
		visitor.setFrame(fact);
		Instruction ins = handle.getInstruction();
		ins.accept(visitor);

		// Special case:
		// The instruction may have produced previously seen values
		// about which new is-null information is known.
		// If any other instances of the produced values exist,
		// update their is-null information.
		int numProduced = ins.produceStack(methodGen.getConstantPool());
		if (numProduced == Constants.UNPREDICTABLE)
			throw new AnalysisException("Unpredictable stack production", methodGen, handle);

		int start = fact.getNumSlots() - numProduced;
		ValueNumberFrame vnaFrameAfter = vnaDataflow.getFactAfterLocation(new Location(handle, basicBlock));

		for (int i = start; i < fact.getNumSlots(); ++i) {
			ValueNumber value = vnaFrameAfter.getValue(i);
			IsNullValue isNullValue = fact.getValue(i);

			for (int j = 0; j < start; ++j) {
				ValueNumber otherValue = vnaFrameAfter.getValue(j);
				if (value.equals(otherValue)) {
					// Same value is in both slots.
					// Update the is-null information to match
					// the new information.
					fact.setValue(j, isNullValue);
				}
			}
		}

	}

	private static final BitSet nullComparisonInstructionSet = new BitSet();

	static {
		nullComparisonInstructionSet.set(Constants.IFNULL);
		nullComparisonInstructionSet.set(Constants.IFNONNULL);
		nullComparisonInstructionSet.set(Constants.IF_ACMPEQ);
		nullComparisonInstructionSet.set(Constants.IF_ACMPNE);
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
							tmpFact.setValue(i, IsNullValue.nonReportingNullOnSomePathValue());
					}
				}
			}

			if (!NO_SWITCH_DEFAULT_AS_EXCEPTION) {
				if (edge.getType() == SWITCH_DEFAULT_EDGE) {
					tmpFact = modifyFrame(fact, tmpFact);
					tmpFact.toExceptionValues();
				}
			}

			final BasicBlock destBlock = edge.getTarget();

			if (destBlock.isExceptionHandler()) {
				// Exception handler - clear stack and push a non-null value
				// to represent the exception.
				tmpFact = modifyFrame(fact, tmpFact);
				tmpFact.clearStack();

				// Downgrade NULL and NSP to DNR if the handler is for
				// CloneNotSupportedException or InterruptedException
				CodeExceptionGen handler = destBlock.getExceptionGen();
				ObjectType catchType = handler.getCatchType();
				if (catchType != null) {
					String catchClass = catchType.getClassName();
					if (catchClass.equals("java.lang.CloneNotSupportedException") ||
					        catchClass.equals("java.lang.InterruptedException")) {
						for (int i = 0; i < tmpFact.getNumSlots(); ++i) {
							IsNullValue value = tmpFact.getValue(i);
							if (value.isDefinitelyNull() || value.isNullOnSomePath())
								tmpFact.setValue(i, IsNullValue.nonReportingNullOnSomePathValue());
						}
					}
				}

				// Mark all values as having occurred on an exception path
				tmpFact.toExceptionValues();

				// Push the exception value
				tmpFact.pushValue(IsNullValue.nonNullValue());
			} else {
				final int edgeType = edge.getType();
				final BasicBlock sourceBlock = edge.getSource();
				final ValueNumberFrame targetVnaFrame = vnaDataflow.getStartFact(destBlock);
				assert targetVnaFrame != null;

				// Determine if the edge conveys any information about the
				// null/non-null status of operands in the incoming frame.
				if (edgeType == IFCMP_EDGE || edgeType == FALL_THROUGH_EDGE) {
					IsNullConditionDecision decision = getResultFact(edge.getSource()).getDecision();
					if (decision != null) {
						if (!decision.isEdgeFeasible(edgeType)) {
							// The incoming edge is infeasible; just use TOP
							// as the start fact for this block.
							tmpFact = createFact();
							tmpFact.setTop();
						} else if (decision.getValue() != null) {
							// A value has been determined for this edge.
							// Use the value to update the is-null information in
							// the start fact for this block.

							final Location atIf = new Location(sourceBlock.getLastInstruction(), sourceBlock);
							final IsNullValueFrame prevIsNullValueFrame = getFactAtLocation(atIf);
							final ValueNumberFrame prevVnaFrame = vnaDataflow.getFactAtLocation(atIf);

							tmpFact = replaceValues(fact, tmpFact, decision.getValue(), prevVnaFrame,
							        targetVnaFrame, decision.getDecision(edgeType));
						}
					}
				}

				// If this is a fall-through edge from a null check,
				// then we know the value checked is not null.
				if (sourceBlock.isNullCheck() && edgeType == FALL_THROUGH_EDGE) {
					ValueNumberFrame vnaFrame = vnaDataflow.getStartFact(destBlock);
					if (vnaFrame == null)
						throw new IllegalStateException("no vna frame at block entry?");

					Instruction firstInDest = edge.getTarget().getFirstInstruction().getInstruction();
					// If we're not sure that the instance is definitely non-null,
					// update the is-null information for the dereferenced value.
					IsNullValue instance = fact.getInstance(firstInDest, methodGen.getConstantPool());
					if (!instance.isDefinitelyNotNull()) {
						ValueNumber replaceMe = vnaFrame.getInstance(firstInDest, methodGen.getConstantPool());
						tmpFact = replaceValues(fact, tmpFact, replaceMe, vnaFrame, targetVnaFrame, IsNullValue.nonNullValue());
					}
				}
			}

			if (tmpFact != null)
				fact = tmpFact;
		}

		// Normal dataflow merge
		mergeInto(fact, result);
	}

	protected IsNullValue mergeValues(IsNullValueFrame frame, int slot, IsNullValue a, IsNullValue b)
	        throws DataflowAnalysisException {
		return IsNullValue.merge(a, b);
	}

	/**
	 * Determine if the given basic block ends in a redundant
	 * null comparison.
	 *
	 * @param basicBlock the basic block
	 * @param lastFrame  the IsNullValueFrame representing values at the final instruction
	 *                   of the block
	 * @return an IsNullConditionDecision object representing the
	 *         is-null information gained about the compared value,
	 *         or null if no information is gained
	 */
	private IsNullConditionDecision getDecision(BasicBlock basicBlock, IsNullValueFrame lastFrame)
	        throws DataflowAnalysisException {

		assert lastFrame != null;

		final InstructionHandle lastInSourceHandle = basicBlock.getLastInstruction();
		if (lastInSourceHandle == null)
			return null; // doesn't end in null comparison

		final short lastInSourceOpcode = lastInSourceHandle.getInstruction().getOpcode();
		if (!nullComparisonInstructionSet.get(lastInSourceOpcode))
			return null; // doesn't end in null comparison

		Location atIf = new Location(lastInSourceHandle, basicBlock);
		ValueNumberFrame prevVnaFrame = vnaDataflow.getFactAtLocation(atIf);

		switch (lastInSourceOpcode) {
		case Constants.IFNULL:
		case Constants.IFNONNULL:
			{
				IsNullValue tos = lastFrame.getTopValue();
				boolean ifnull = (lastInSourceOpcode == Constants.IFNULL);

				// Initially, assume neither branch is feasible.
				IsNullValue ifcmpDecision = null;
				IsNullValue fallThroughDecision = null;

				if (tos.isDefinitelyNull()) {
					// Predetermined comparison - one branch is infeasible
					if (ifnull)
						ifcmpDecision = IsNullValue.flowSensitiveNullValue();
					else // ifnonnull
						fallThroughDecision = IsNullValue.flowSensitiveNullValue();
				} else if (tos.isDefinitelyNotNull()) {
					// Predetermined comparison - one branch is infeasible
					if (ifnull)
						fallThroughDecision = IsNullValue.flowSensitiveNonNullValue();
					else // ifnonnull
						ifcmpDecision = IsNullValue.flowSensitiveNonNullValue();
				} else {
					// As far as we know, both branches feasible
					ifcmpDecision = ifnull ? IsNullValue.flowSensitiveNullValue() : IsNullValue.flowSensitiveNonNullValue();
					fallThroughDecision = ifnull ? IsNullValue.flowSensitiveNonNullValue() : IsNullValue.flowSensitiveNullValue();
				}
				return new IsNullConditionDecision(prevVnaFrame.getTopValue(), ifcmpDecision, fallThroughDecision);
			}
		case Constants.IF_ACMPEQ:
		case Constants.IF_ACMPNE:
			{
				IsNullValue tos = lastFrame.getStackValue(0);
				IsNullValue nextToTOS = lastFrame.getStackValue(1);

				boolean tosNull = tos.isDefinitelyNull();
				boolean nextToTOSNull = nextToTOS.isDefinitelyNull();

				boolean cmpeq = (lastInSourceOpcode == Constants.IF_ACMPEQ);

				// Initially, assume neither branch is feasible.
				IsNullValue ifcmpDecision = null;
				IsNullValue fallThroughDecision = null;
				ValueNumber value;

				if (tosNull && nextToTOSNull) {
					// Redundant comparision: both values are null, only one branch is feasible
					value = null; // no value will be replaced - just want to indicate that one of the branches is infeasible
					if (cmpeq)
						ifcmpDecision = IsNullValue.flowSensitiveNullValue();
					else // cmpne
						fallThroughDecision = IsNullValue.flowSensitiveNullValue();
				} else if (tosNull || nextToTOSNull) {
					// We have updated information about whichever value is not null;
					// both branches are feasible
					value = prevVnaFrame.getStackValue(tosNull ? 1 : 0);
					ifcmpDecision = cmpeq ? IsNullValue.flowSensitiveNullValue() : IsNullValue.flowSensitiveNonNullValue();
					fallThroughDecision = cmpeq ? IsNullValue.flowSensitiveNonNullValue() : IsNullValue.flowSensitiveNullValue();
				} else {
					// No information gained
					break;
				}

				return new IsNullConditionDecision(value, ifcmpDecision, fallThroughDecision);
			}
		default:
			throw new IllegalStateException();
		}

		return null; // no information gained
	}

	/**
	 * Update is-null information at a branch target based on information gained at a
	 * null comparison branch.
	 *
	 * @param origFrame        the original is-null frame at entry to basic block
	 * @param frame            the modified version of the is-null entry frame;
	 *                         null if the entry frame has not been modified yet
	 * @param replaceMe        the ValueNumber in the value number frame at the if comparison
	 *                         whose is-null information will be updated
	 * @param prevVnaFrame     the ValueNumberFrame at the if comparison
	 * @param targetVnaFrame   the ValueNumberFrame at entry to the basic block
	 * @param replacementValue the IsNullValue representing the updated
	 *                         is-null information
	 * @return a modified IsNullValueFrame with updated is-null information
	 */
	private IsNullValueFrame replaceValues(IsNullValueFrame origFrame, IsNullValueFrame frame,
	                                       ValueNumber replaceMe, ValueNumberFrame prevVnaFrame, ValueNumberFrame targetVnaFrame, IsNullValue replacementValue) {

		// If required, make a copy of the frame
		frame = modifyFrame(origFrame, frame);

		assert frame.getNumSlots() == targetVnaFrame.getNumSlots();

		// The VNA frame may have more slots than the IsNullValueFrame
		// if it was produced by an IF comparison (whose operand or operands
		// are subsequently popped off the stack).

		final int targetNumSlots = targetVnaFrame.getNumSlots();
		final int prefixNumSlots = Math.min(frame.getNumSlots(), prevVnaFrame.getNumSlots());

		// Here's the deal:
		// - "replaceMe" is the value number from the previous frame (at the if branch)
		//   which indicates a value that we have updated is-null information about
		// - in the target value number frame (at entry to the target block),
		//   we find the value number in the stack slot corresponding to the "replaceMe"
		//   value; this is the "corresponding" value
		// - all instances of the "corresponding" value in the target frame have
		//   their is-null information updated to "replacementValue"
		// This should thoroughly make use of the updated information.

		for (int i = 0; i < prefixNumSlots; ++i) {
			if (prevVnaFrame.getValue(i).equals(replaceMe)) {
				ValueNumber corresponding = targetVnaFrame.getValue(i);
				for (int j = 0; j < targetNumSlots; ++j) {
					if (targetVnaFrame.getValue(j).equals(corresponding))
						frame.setValue(j, replacementValue);
				}
			}
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
