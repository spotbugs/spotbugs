/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003-2005 University of Maryland
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

import org.apache.bcel.Constants;
import org.apache.bcel.generic.IFNONNULL;
import org.apache.bcel.generic.IFNULL;
import org.apache.bcel.generic.IF_ACMPEQ;
import org.apache.bcel.generic.IF_ACMPNE;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.DefaultAnnotationForParameters;
import edu.umd.cs.findbugs.annotations.NonNull;

@javax.annotation.ParametersAreNonnullByDefault
@DefaultAnnotationForParameters(NonNull.class)
public class ResourceValueAnalysis <Resource> extends FrameDataflowAnalysis<ResourceValue, ResourceValueFrame>
		implements EdgeTypes {

	private static final boolean DEBUG = SystemProperties.getBoolean("dataflow.debug");

	private MethodGen methodGen;
	private CFG cfg;
	private ResourceTracker<Resource> resourceTracker;
	private Resource resource;
	private ResourceValueFrameModelingVisitor visitor;
	private boolean ignoreImplicitExceptions;

	public ResourceValueAnalysis(MethodGen methodGen, CFG cfg, DepthFirstSearch dfs,
								 ResourceTracker<Resource> resourceTracker, Resource resource) {

		super(dfs);
		this.methodGen = methodGen;
		this.cfg = cfg;
		this.resourceTracker = resourceTracker;
		this.resource = resource;
		this.visitor = resourceTracker.createVisitor(resource, methodGen.getConstantPool());

		this.ignoreImplicitExceptions = resourceTracker.ignoreImplicitExceptions(resource);
	}

	public ResourceValueFrame createFact() {
		ResourceValueFrame fact = new ResourceValueFrame(methodGen.getMaxLocals());
		fact.setTop();
		return fact;
	}

	public void initEntryFact(ResourceValueFrame result) {
		result.setValid();
		result.clearStack();
		final int numSlots = result.getNumSlots();
		for (int i = 0; i < numSlots; ++i) {
			boolean slotContainsInstance = resourceTracker.isParamInstance(resource, i);
			result.setValue(i, slotContainsInstance ? ResourceValue.instance() : ResourceValue.notInstance());
		}
	}

	public void meetInto(ResourceValueFrame fact, Edge edge, ResourceValueFrame result) throws DataflowAnalysisException {
		BasicBlock source = edge.getSource();
		BasicBlock dest = edge.getTarget();

		ResourceValueFrame tmpFact = null;

		if (edge.isExceptionEdge()) {
			// If this edge throws only implicit exceptions
			// (as determined by TypeAnalysis and PruneInfeasibleExceptionEdges),
			// and the resource tracker says to ignore implicit exceptions
			// for this resource, ignore it.
			if (AnalysisContext.currentAnalysisContext().getBoolProperty(AnalysisFeatures.ACCURATE_EXCEPTIONS) &&
					ignoreImplicitExceptions &&
					!edge.isFlagSet(EXPLICIT_EXCEPTIONS_FLAG))
				return;

			// The ResourceTracker may veto the exception edge
			if (resourceTracker.ignoreExceptionEdge(edge, resource, methodGen.getConstantPool()))
				return;

			if (fact.getStatus() == ResourceValueFrame.OPEN) {
				// If status is OPEN, downgrade to OPEN_ON_EXCEPTION_PATH
				tmpFact = modifyFrame(fact, null);
				tmpFact.setStatus(ResourceValueFrame.OPEN_ON_EXCEPTION_PATH);
			}

			if (fact.isValid()) {
				// Special case: if the instruction that closes the resource
				// throws an exception, we consider the resource to be successfully
				// closed anyway.
				InstructionHandle exceptionThrower = source.getExceptionThrower();
				BasicBlock fallThroughSuccessor = cfg.getSuccessorWithEdgeType(source, FALL_THROUGH_EDGE);
				if (DEBUG && fallThroughSuccessor == null) System.out.println("Null fall through successor!");
				if (fallThroughSuccessor != null &&
						resourceTracker.isResourceClose(fallThroughSuccessor, exceptionThrower, methodGen.getConstantPool(), resource, fact)) {
					tmpFact = modifyFrame(fact, tmpFact);
					tmpFact.setStatus(ResourceValueFrame.CLOSED);
					if (DEBUG) System.out.print("(failed attempt to close)");
				}
			}

			if (dest.isExceptionHandler()) {
				// Clear stack, push value for exception
				if (fact.isValid()) {
					tmpFact = modifyFrame(fact, tmpFact);
					tmpFact.clearStack();
					tmpFact.pushValue(ResourceValue.notInstance());
				}
			}
		}

		// Make the resource nonexistent if it is compared against null
		int edgeType = edge.getType();
		if (edgeType == IFCMP_EDGE || edgeType == FALL_THROUGH_EDGE) {
			InstructionHandle lastInSourceHandle = source.getLastInstruction();
			if (lastInSourceHandle != null) {
				Instruction lastInSource = lastInSourceHandle.getInstruction();
				boolean isNullCheck = false;
				boolean isNonNullCheck = false;
				// This check catches null == X, null != X
				if (lastInSource instanceof IF_ACMPEQ || lastInSource instanceof IF_ACMPNE) {
					Location l = new Location(lastInSourceHandle, source);
					InstructionHandle ih         = l.getHandle();
					// Get instruction that pushed topmost
					InstructionHandle ihPrev     = ih == null     ? null : ih.getPrev();
					// Get next-topmost that pushed next-topmost
					InstructionHandle ihPrevPrev = ihPrev == null ? null : ihPrev.getPrev();
					int prevPush = 0;
					if(ihPrev != null) {
						try {
							prevPush = ihPrev.getInstruction().produceStack(null);
						} catch(NullPointerException e) { /* do nothing */ }
					}
					int prevPrevPush = 0;
					if(ihPrevPrev != null) {
						try {
							prevPrevPush = ihPrevPrev.getInstruction().produceStack(null);
						} catch(NullPointerException e) { /* do nothing */ }
					}
					// If instructions exist and both push one word onto the
					// stack and the next-topmost pushes null...
					if(ihPrev != null && ihPrevPrev != null &&
					   prevPush == 1 && prevPrevPush == 1 &&
					   ihPrevPrev.getInstruction().getOpcode() == Constants.ACONST_NULL)
					{
						// Topmost item on stack is being compared with null
						// (the null itself is next-topmost on the stack)
						isNullCheck = lastInSource instanceof IF_ACMPEQ;
						isNonNullCheck = lastInSource instanceof IF_ACMPNE;
					}
				}
				// This check catches X == null, X != null
				else if (lastInSource instanceof IFNULL || lastInSource instanceof IFNONNULL) {
					isNullCheck = lastInSource instanceof IFNULL;
					isNonNullCheck = lastInSource instanceof IFNONNULL;
				}
				if(isNullCheck || isNonNullCheck) {
					// Get the frame at the if statement
					ResourceValueFrame startFrame = getStartFact(source);
					if (startFrame.isValid()) {
						// The source block has a valid start fact.
						// That means it is safe to inspect the frame at the If instruction.
						ResourceValueFrame frameAtIf = getFactAtLocation(new Location(lastInSourceHandle, source));
						ResourceValue topValue = frameAtIf.getValue(frameAtIf.getNumSlots() - 1);

						if (topValue.isInstance()) {
							if ((isNullCheck && edgeType == IFCMP_EDGE) ||
								(isNonNullCheck && edgeType == FALL_THROUGH_EDGE)) {
								//System.out.println("**** making resource nonexistent on edge "+edge.getId());
								tmpFact = modifyFrame(fact, tmpFact);
								tmpFact.setStatus(ResourceValueFrame.NONEXISTENT);
							}
						}
					}
				}
			}
		}

		if (tmpFact != null)
			fact = tmpFact;

		mergeInto(fact, result);
	}

	@Override
		 protected void mergeInto(ResourceValueFrame frame, ResourceValueFrame result)
			throws DataflowAnalysisException {
		// Merge slots
		super.mergeInto(frame, result);

		// Merge status
		result.setStatus(Math.min(result.getStatus(), frame.getStatus()));
	}


	@Override
		 protected void mergeValues(ResourceValueFrame otherFrame, ResourceValueFrame resultFrame, int slot) throws DataflowAnalysisException {
		ResourceValue value = ResourceValue.merge(resultFrame.getValue(slot), otherFrame.getValue(slot));
		resultFrame.setValue(slot, value);
	}

	@Override
		 public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, ResourceValueFrame fact)
			throws DataflowAnalysisException {

		visitor.setFrameAndLocation(fact, new Location(handle, basicBlock));
		visitor.transferInstruction(handle, basicBlock);

	}

}

// vim:ts=4
