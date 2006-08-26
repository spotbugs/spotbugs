/*
 * Bytecode Analysis Framework
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
package edu.umd.cs.findbugs.ba.ca;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;

import edu.umd.cs.findbugs.ba.AbstractDataflowAnalysis;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.BlockOrder;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Dataflow;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DataflowTestDriver;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.ReversePostOrder;

public class CallListAnalysis extends AbstractDataflowAnalysis<CallList> {
	private CFG cfg;
	private DepthFirstSearch dfs;
	//private ConstantPoolGen cpg;
	private Map<InstructionHandle, Call> callMap;
	
	public CallListAnalysis(CFG cfg, DepthFirstSearch dfs, ConstantPoolGen cpg) {
		this.cfg = cfg;
		this.dfs = dfs;
		//this.cpg = cpg;
		this.callMap = buildCallMap(cfg, cpg);
	}
	
	private static Map<InstructionHandle, Call> buildCallMap(CFG cfg, ConstantPoolGen cpg) {
		Map<InstructionHandle, Call> callMap = new HashMap<InstructionHandle, Call>();
		
		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
			InstructionHandle handle = i.next().getHandle();
			Instruction ins = handle.getInstruction();
			
			if (ins instanceof InvokeInstruction) {
				InvokeInstruction inv = (InvokeInstruction) ins;
				Call call = new Call(inv.getClassName(cpg), inv.getName(cpg), inv.getSignature(cpg));
				callMap.put(handle, call);
			}
		}
		
		return callMap;
	}
	
	public void initEntryFact(CallList fact) {
		fact.clear();
	}
	
	public void initResultFact(CallList fact) {
		fact.setTop();
	}
	
	public boolean isForwards() {
		return true;
	}

	public BlockOrder getBlockOrder(CFG cfg) {
		return new ReversePostOrder(cfg, dfs);
	}
	
	public void makeFactTop(CallList fact) {
		fact.setTop();
	}
	
	public CallList createFact() {
		return new CallList();
	}
	
	public boolean same(CallList a, CallList b) {
		return a.equals(b);
	}
	
	public void meetInto(CallList start, Edge edge, CallList result)
			throws DataflowAnalysisException {
		CallList merge = CallList.merge(start, result);
		result.copyFrom(merge);
	}
	
	public void copy(CallList source, CallList dest) {
		dest.copyFrom(source);
	}

	@Override
         public void transferInstruction(
			InstructionHandle handle, BasicBlock basicBlock, CallList fact) throws DataflowAnalysisException {
		Call call = callMap.get(handle);
		if (call != null) {
			fact.add(call);
		}
	}
	
	@Override
         public boolean isFactValid(CallList fact) {
		return fact.isValid();
	}
	
	public static void main(String[] argv) throws Exception {
		if (argv.length != 1) {
			System.err.println("Usage: " + CallListAnalysis.class.getName() + " <class file>");
			System.exit(1);
		}
		
		DataflowTestDriver<CallList, CallListAnalysis> driver =
			new DataflowTestDriver<CallList, CallListAnalysis>() {
				@Override
                                 public Dataflow<CallList, CallListAnalysis> createDataflow(
						ClassContext classContext,
						Method method) throws CFGBuilderException, DataflowAnalysisException {
					CallListAnalysis analysis = new CallListAnalysis(
							classContext.getCFG(method),
							classContext.getDepthFirstSearch(method),
							classContext.getConstantPoolGen());
					Dataflow<CallList, CallListAnalysis> dataflow =
						new Dataflow<CallList, CallListAnalysis>(analysis.cfg, analysis);
						
					dataflow.execute();
					
					return dataflow;
				}
			};
	
		driver.execute(argv[0]);	
	}
}
