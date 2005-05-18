/*
 * Bytecode analysis framework
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

package edu.umd.cs.findbugs.ba.npe;

import java.util.BitSet;

import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.ba.BackwardDataflowAnalysis;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.ReverseDepthFirstSearch;

/**
 * Dataflow analysis to look for parameters dereferenced unconditionally.
 * Flow values are sets of parameter value numbers which are dereferenced
 * on every path past the current location.
 * 
 * @author David Hovemeyer
 */
public class UnconditionalDerefAnalysis extends BackwardDataflowAnalysis<BitSet> {
	
	public UnconditionalDerefAnalysis(ReverseDepthFirstSearch rdfs) {
		super(rdfs);
	}
	
	public void copy(BitSet source, BitSet dest) {
		// TODO Auto-generated method stub
		
	}
	
	public BitSet createFact() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public void initEntryFact(BitSet result) throws DataflowAnalysisException {
		// TODO Auto-generated method stub
		
	}
	
	public void initResultFact(BitSet result) {
		// TODO Auto-generated method stub
		
	}
	
	public void makeFactTop(BitSet fact) {
		// TODO Auto-generated method stub
		
	}
	
	public void meetInto(BitSet fact, Edge edge, BitSet result) throws DataflowAnalysisException {
		// TODO Auto-generated method stub
		
	}
	
	public boolean same(BitSet fact1, BitSet fact2) {
		// TODO Auto-generated method stub
		return false;
	}
	
	//@Override
	public boolean isFactValid(BitSet fact) {
		// TODO Auto-generated method stub
		return false;
	}
	
	public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, BitSet fact)
		throws DataflowAnalysisException {
		
	}
}
