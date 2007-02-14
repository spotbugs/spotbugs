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

import java.util.HashMap;
import java.util.Map;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.ForwardDataflowAnalysis;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.XField;

/**
 * @author David Hovemeyer
 */
public abstract class FieldSetAnalysis extends ForwardDataflowAnalysis<FieldSet> {
	private ConstantPoolGen cpg;
	
	private Map<InstructionHandle, XField> instructionToFieldMap;
	
	public FieldSetAnalysis(DepthFirstSearch dfs, ConstantPoolGen cpg) {
		super(dfs);
		this.cpg = cpg;
		this.instructionToFieldMap = new HashMap<InstructionHandle, XField>();
	}
	
	public ConstantPoolGen getCPG() {
		return cpg;
	}
	
	public void makeFactTop(FieldSet fact) {
		fact.setTop();
	}
	public boolean isTop(FieldSet fact) {
		return fact.isTop();
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
	
	
	@Override
         public boolean isFactValid(FieldSet fact) {
		return fact.isValid();
	}
	
	public void copy(FieldSet source, FieldSet dest) {
		dest.copyFrom(source);
	}
	
	@Override
         public void transferInstruction(
			InstructionHandle handle,
			BasicBlock basicBlock,
			FieldSet fact) throws DataflowAnalysisException {
		if (!isFactValid(fact))
			return;
		
		try {
			handleInstruction(handle, basicBlock, fact);
		} catch (ClassNotFoundException e) {
			AnalysisContext.reportMissingClass(e);
			fact.setBottom();
		}
	}
	
	private void handleInstruction(
			InstructionHandle handle,
			BasicBlock basicBlock,
			FieldSet fact) throws DataflowAnalysisException, ClassNotFoundException {
		Instruction ins = handle.getInstruction();
		short opcode = ins.getOpcode();
		XField field;
		
		switch (opcode) {
		case Constants.GETFIELD:
		case Constants.GETSTATIC:
			field = lookupField(handle, (FieldInstruction) ins);
			if (field != null) {
				sawLoad(fact, field);
			}
			break;
		
		case Constants.PUTFIELD:
		case Constants.PUTSTATIC:
			field = lookupField(handle, (FieldInstruction) ins);
			if (field != null) {
				sawStore(fact, field);
			}
			break;
		
		case Constants.INVOKEINTERFACE:
		case Constants.INVOKESPECIAL:
		case Constants.INVOKESTATIC:
		case Constants.INVOKEVIRTUAL:
			// Assume that the called method assigns loads and stores all possible fields
			fact.setBottom();
			break;
		}
	}
	
	private XField lookupField(InstructionHandle handle, FieldInstruction fins) throws ClassNotFoundException {
		XField field = instructionToFieldMap.get(handle);
		if (field == null) {
			field = Hierarchy.findXField(fins, getCPG());
			instructionToFieldMap.put(handle, field);
		}
		return field;
	}
	
	protected abstract void sawLoad(FieldSet fact, XField field);
	protected abstract void sawStore(FieldSet fact, XField field);
}
