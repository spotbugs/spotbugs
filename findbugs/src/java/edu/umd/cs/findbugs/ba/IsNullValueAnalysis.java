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

import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.InstructionHandle;

public class IsNullValueAnalysis extends ForwardDataflowAnalysis<IsNullValueFrame> {

	private MethodGen methodGen;
	private ValueNumberDataflow vnaDataflow;

	public IsNullValueAnalysis(MethodGen methodGen, ValueNumberDataflow vnaDataflow) {
		this.methodGen = methodGen;
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

	public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, IsNullValueFrame fact)
		throws DataflowAnalysisException {
	}

	public void meetInto(IsNullValueFrame fact, Edge edge, IsNullValueFrame result) throws DataflowAnalysisException {
		result.mergeWith(fact);
	}

}

// vim:ts=4
