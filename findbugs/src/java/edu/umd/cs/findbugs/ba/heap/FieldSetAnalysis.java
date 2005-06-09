/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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

package edu.umd.cs.findbugs.ba.heap;

import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.ForwardDataflowAnalysis;

/**
 * @author David Hovemeyer
 */
public class FieldSetAnalysis extends ForwardDataflowAnalysis<FieldSet> {
	public FieldSetAnalysis(DepthFirstSearch dfs) {
		super(dfs);
	}
	
	public void makeFactTop(FieldSet fact) {
		fact.setTop();
	}
	
	public void initEntryFact(FieldSet result) throws DataflowAnalysisException {
		result.clear();
	}
	
	public void initResultFact(FieldSet result) {
		makeFactTop(result);
	}
	
	public void meetInto(FieldSet fact, Edge edge, FieldSet result) throws DataflowAnalysisException {
		result.mergeWith(fact);
	}
	
	public boolean same(FieldSet fact1, FieldSet fact2) {
		return fact1.sameAs(fact2);
	}
	
	public FieldSet createFact() {
		return new FieldSet();
	}
	
	//@Override
	public boolean isFactValid(FieldSet fact) {
		return !(fact.isTop() || fact.isBottom());
	}
	
	public void copy(FieldSet source, FieldSet dest) {
		dest.copyFrom(source);
	}
	
	public void transferInstruction(
			InstructionHandle handle,
			BasicBlock basicBlock,
			FieldSet fact) throws DataflowAnalysisException {
		// TODO: implement
	}
}
