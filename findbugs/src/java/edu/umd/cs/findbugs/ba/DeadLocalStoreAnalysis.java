/*
 * Bytecode Analysis Framework
 * Copyright (C) 2004, University of Maryland
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

import java.util.BitSet;

import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.LoadInstruction;
import org.apache.bcel.generic.MethodGen;

/**
 * Dataflow analysis to find dead stores of locals.
 * This is just a backward analysis to see which loads
 * reach stores of the same local.
 *
 * @author David Hovemeyer
 */
public class DeadLocalStoreAnalysis extends BackwardDataflowAnalysis<BitSet> {
	private int topBit ;

	public DeadLocalStoreAnalysis(MethodGen methodGen, ReverseDepthFirstSearch rdfs) {
		super(rdfs);
		this.topBit = methodGen.getMaxLocals();
	}

	public BitSet createFact() {
		return new BitSet();
	}

	public void copy(BitSet source, BitSet dest) {
		dest.clear();
		dest.or(source);
	}

	public void initEntryFact(BitSet result) throws DataflowAnalysisException {
		result.clear();
	}

	public void initResultFact(BitSet result) {
		makeFactTop(result);
	}

	public void makeFactTop(BitSet fact) {
		fact.clear();
		fact.set(topBit);
	}

	public boolean same(BitSet fact1, BitSet fact2) {
		return fact1.equals(fact2);
	}

	public void meetInto(BitSet fact, Edge edge, BitSet result) throws DataflowAnalysisException {
		if (isTop(fact)) {
			// Nothing to do, result stays the same
		} if (isTop(result)) {
			// Result is top, so it takes the value of fact
			copy(fact, result);
		} else {
			// Meet is union
			result.or(fact);
		}
	}

	public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, BitSet fact)
		throws DataflowAnalysisException {
		Instruction ins = handle.getInstruction();
		if (ins instanceof LoadInstruction) {
			LoadInstruction load = (LoadInstruction) ins;
			int local = load.getIndex();
			fact.set(local);
		}
	}

	public boolean isFactValid(BitSet fact) {
		return !isTop(fact);
	}

	private boolean isTop(BitSet fact) {
		return fact.get(topBit);
	}
}

// vim:ts=4
