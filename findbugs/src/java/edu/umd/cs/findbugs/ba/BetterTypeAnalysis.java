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

import edu.umd.cs.findbugs.ba.type.Type;
import edu.umd.cs.findbugs.ba.type.TypeMerger;
import edu.umd.cs.findbugs.ba.type.TypeRepository;

import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

public class BetterTypeAnalysis extends FrameDataflowAnalysis<Type, BetterTypeFrame> {
	private MethodGen methodGen;
	private CFG cfg;
	private TypeRepository typeRepository;
	private TypeMerger typeMerger;

	public BetterTypeAnalysis(MethodGen methodGen, CFG cfg, DepthFirstSearch dfs,
			TypeRepository typeRepository, TypeMerger typeMerger) {
		super(dfs);
		this.methodGen = methodGen;
		this.cfg = cfg;
		this.typeRepository = typeRepository;
		this.typeMerger = typeMerger;
	}

	public BetterTypeFrame createFact() {
		return new BetterTypeFrame(methodGen.getMaxLocals());
	}

	public void initEntryFact(BetterTypeFrame result) {
		// TODO: implement
	}

	public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, BetterTypeFrame fact) throws DataflowAnalysisException {
		// TODO: implement
	}

	public void meetInto(BetterTypeFrame fact, Edge edge, BetterTypeFrame result) throws DataflowAnalysisException {
		// TODO: implement
	}

	protected Type mergeValues(BetterTypeFrame frame, int slot, Type a, Type b)
		throws DataflowAnalysisException {
		// TODO: implement
		return typeRepository.getBottomType();
	}
}

// vim:ts=4
