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

public class ResourceValueAnalysis extends FrameDataflowAnalysis<ResourceValue, ResourceValueFrame> {

	private MethodGen methodGen;
	private CFG cfg;

	public ResourceValueAnalysis(MethodGen methodGen, CFG cfg) {
		this.methodGen = methodGen;
		this.cfg = cfg;
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
		// TODO: implement
	}

	public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, ResourceValueFrame fact)
		throws DataflowAnalysisException {
		// TODO: implement
	}

}

// vim:ts=4
