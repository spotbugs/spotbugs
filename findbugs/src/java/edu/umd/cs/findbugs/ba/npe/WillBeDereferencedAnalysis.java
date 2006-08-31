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

import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.AnalysisFeatures;
import edu.umd.cs.findbugs.ba.BackwardDataflowAnalysis;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.EdgeTypes;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.ReverseDepthFirstSearch;
import edu.umd.cs.findbugs.ba.type.TypeDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

/**
 * Dataflow analysis to look for parameters dereferenced unconditionally.
 * Flow values are sets of parameters (indexed starting from 0) which are
 * dereferenced on every path past the current location.
 * 
 * @author David Hovemeyer
 */
public @Deprecated class WillBeDereferencedAnalysis extends BackwardDataflowAnalysis<WillBeDereferencedInfo> {
	 

	private static final boolean DEBUG = SystemProperties.getBoolean("npe.deref.debug");
	
	private final CFG cfg;
	private final MethodGen methodGen;
	//private final TypeDataflow typeDataflow;
	private final ValueNumberDataflow vnaDataflow;
	//private final int maxBit;
	

	
	public WillBeDereferencedAnalysis(
			ReverseDepthFirstSearch rdfs,
			CFG cfg,
			MethodGen methodGen,
			ValueNumberDataflow vnaDataflow,
			TypeDataflow typeDataflow) {
		super(rdfs, null);
		this.cfg = cfg;
		this.methodGen = methodGen;
		//this.typeDataflow = typeDataflow;
		this.vnaDataflow = vnaDataflow;
		//this.maxBit = methodGen.getMaxLocals();

	}

	public void copy(WillBeDereferencedInfo source, WillBeDereferencedInfo dest) {
		dest.copyFrom(source);
	}
	
	public WillBeDereferencedInfo createFact() {
		return new WillBeDereferencedInfo();
	}
	
	public void initEntryFact(WillBeDereferencedInfo result) throws DataflowAnalysisException {
		// At entry (really the CFG exit, since this is a backwards analysis)
		// no dereferences have been seen
		result.value.clear();
		result.isTop = false;
	}
	
	public void initResultFact(WillBeDereferencedInfo result) {
		makeFactTop(result);
	}
	
	public void makeFactTop(WillBeDereferencedInfo fact) {
		fact.isTop = true;
		fact.value.clear();
	}

	public boolean isTop(WillBeDereferencedInfo fact) {
		return fact.isTop;
	}
	public void meetInto(WillBeDereferencedInfo fact, Edge edge, WillBeDereferencedInfo result) throws DataflowAnalysisException {
		// Ignore implicit exceptions
		if (AnalysisContext.currentAnalysisContext().getBoolProperty(AnalysisFeatures.ACCURATE_EXCEPTIONS)
				&& edge.isExceptionEdge()
				&& !edge.isFlagSet(EdgeTypes.EXPLICIT_EXCEPTIONS_FLAG)) {
			return;
		}
		result.meet(fact);
	}
	
	public boolean same(WillBeDereferencedInfo fact1, WillBeDereferencedInfo fact2) {
		return fact1.equals(fact2);
	}
	
	@Override
	public boolean isFactValid(WillBeDereferencedInfo fact) {
		return !fact.isTop;
	}
	
	@Override
         public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, WillBeDereferencedInfo fact)
		throws DataflowAnalysisException {
		

		if (!isFactValid(fact))
			throw new IllegalStateException();

		
		// See if this instruction has a null check.
		if (handle != basicBlock.getFirstInstruction())
			return;
		BasicBlock fallThroughPredecessor = cfg.getPredecessorWithEdgeType(basicBlock, EdgeTypes.FALL_THROUGH_EDGE);
		if (fallThroughPredecessor == null || !fallThroughPredecessor.isNullCheck())
			return;

		// Get value number of the checked value
		ValueNumberFrame vnaFrame = vnaDataflow.getFactAtLocation(new Location(handle, basicBlock));
		if (!vnaFrame.isValid()) {
			// Probably dead code.
			// Assume this location can't be reached.
			makeFactTop(fact);
			return;
		}
		ValueNumber instance = vnaFrame.getInstance(handle.getInstruction(), methodGen.getConstantPool());
		if (DEBUG) {
			System.out.println("[Null check of value " + instance.getNumber() + "]");
		}
		fact.value.add(instance);
		
		
	}

	
}
