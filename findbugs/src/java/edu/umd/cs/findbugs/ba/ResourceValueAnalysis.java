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

import org.apache.bcel.generic.*;

public class ResourceValueAnalysis<Resource> extends FrameDataflowAnalysis<ResourceValue, ResourceValueFrame> {

	private MethodGen methodGen;
	private ResourceTracker<Resource> resourceTracker;
	private Resource resource;
	private ResourceValueFrameModelingVisitor visitor;

	public ResourceValueAnalysis(MethodGen methodGen, ResourceTracker<Resource> resourceTracker, Resource resource) {
		this.methodGen = methodGen;
		this.resourceTracker = resourceTracker;
		this.resource = resource;
		this.visitor = resourceTracker.createVisitor(resource, methodGen.getConstantPool());
	}

	public ResourceValueFrame createFact() {
		return new ResourceValueFrame(methodGen.getMaxLocals());
	}

	public void initEntryFact(ResourceValueFrame result) {
		result.setValid();
		result.clearStack();
		final int numSlots = result.getNumSlots();
		for (int i = 0; i < numSlots; ++i)
			result.setValue(i, ResourceValue.notInstance());
	}

	public void meetInto(ResourceValueFrame fact, Edge edge, ResourceValueFrame result) throws DataflowAnalysisException {
		BasicBlock source = edge.getSource();
		BasicBlock dest = edge.getDest();

		if (dest.isExceptionHandler()) {
			ResourceValueFrame tmpFact = null;

			// Clear stack, push value for exception
			if (fact.isValid()) {
				tmpFact = createFact();
				tmpFact.copyFrom(fact);
				tmpFact.clearStack();
				tmpFact.pushValue(ResourceValue.notInstance());
			}

			// Special case: if the instruction that closes the resource
			// throws an exception, we consider the resource to be successfully
			// closed anyway.
			InstructionHandle exceptionThrower = source.getExceptionThrower();
			assert exceptionThrower != null; // is it possible to reach an exception handler by a non-exception edge?
			if (resourceTracker.isResourceClose(dest, exceptionThrower, methodGen.getConstantPool(), resource)) {
				if (tmpFact == null) {
					tmpFact = createFact();
					tmpFact.copyFrom(fact);
				}
				tmpFact.setStatus(ResourceValueFrame.CLOSED);
			}

			if (tmpFact != null)
				fact = tmpFact;
		}

		result.mergeWith(fact);
	}

	public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, ResourceValueFrame fact)
		throws DataflowAnalysisException {

		visitor.setFrame(fact);
		visitor.transferInstruction(handle, basicBlock);

	}

}

// vim:ts=4
