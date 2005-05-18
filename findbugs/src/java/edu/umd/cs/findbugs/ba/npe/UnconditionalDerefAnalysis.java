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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.ba.BackwardDataflowAnalysis;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.ExceptionSet;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.ReverseDepthFirstSearch;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.TypeAnalysis;
import edu.umd.cs.findbugs.ba.TypeDataflow;
import edu.umd.cs.findbugs.ba.vna.MergeTree;
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
public class UnconditionalDerefAnalysis extends BackwardDataflowAnalysis<BitSet> {
	
	private final CFG cfg;
	private final MethodGen methodGen;
	private final TypeDataflow typeDataflow;
	private final ValueNumberDataflow vnaDataflow;
	private final HashMap<ValueNumber, Integer> valueNumberToParamMap;
	private final BitSet paramValueNumberSet;
	private final int numParams;
	private final int topBit;
	private final int bottomBit;
	
	public UnconditionalDerefAnalysis(
			ReverseDepthFirstSearch rdfs,
			CFG cfg,
			MethodGen methodGen,
			ValueNumberDataflow vnaDataflow,
			TypeDataflow typeDataflow) {
		super(rdfs);
		this.cfg = cfg;
		this.methodGen = methodGen;
		this.typeDataflow = typeDataflow;
		this.vnaDataflow = vnaDataflow;
		this.valueNumberToParamMap = new HashMap<ValueNumber, Integer>();
		this.paramValueNumberSet = new BitSet();
		this.numParams = new SignatureParser(methodGen.getSignature()).getNumParameters();
		this.topBit = numParams;
		this.bottomBit = numParams + 1;
		
		buildValueNumberToParamMap();
	}
	
	private void buildValueNumberToParamMap() {
		ValueNumberFrame vnaFrameAtEntry = vnaDataflow.getStartFact(cfg.getEntry());
		
		Map<ValueNumber, Integer> valueNumberToParamMap = new HashMap<ValueNumber, Integer>();

		int paramOffset = methodGen.isStatic() ? 0 : 1;

		for (int paramIndex = 0; paramIndex < numParams; ++paramIndex) {
			int paramLocal = paramIndex + paramOffset;
			
			ValueNumber valueNumber = vnaFrameAtEntry.getValue(paramLocal);
			valueNumberToParamMap.put(valueNumber, new Integer(paramIndex));
			paramValueNumberSet.set(valueNumber.getNumber());
		}
	}

	public void copy(BitSet source, BitSet dest) {
		dest.clear();
		dest.or(source);
	}
	
	public BitSet createFact() {
		return new BitSet();
	}
	
	public void initEntryFact(BitSet result) throws DataflowAnalysisException {
		// At entry (really the CFG exit, since this is a backwards analysis)
		// no dereferences have been seen
		result.clear();
	}
	
	public void initResultFact(BitSet result) {
		makeFactTop(result);
	}
	
	public void makeFactTop(BitSet fact) {
		fact.clear();
		fact.set(topBit);
	}
	
	public void meetInto(BitSet fact, Edge edge, BitSet result) throws DataflowAnalysisException {
		// Ignore implicit exceptions
		if (TypeAnalysis.ACCURATE_EXCEPTIONS) {
			// Ignore "implicit" exceptions.  These are any runtime
			// exceptions not explicitly declared by a called method,
			// or thrown by an ATHROW instruction.
			ExceptionSet exceptionSet = typeDataflow.getAnalysis().getEdgeExceptionSet(edge); 
			if (!exceptionSet.containsExplicitExceptions()) {
				return;
			}
		}
		
		if (isTop(result) || isBottom(fact)) {
			copy(fact, result);
		} else if (isBottom(result) || isTop(fact)) {
			// Nothing to do
		} else {
			// Meet is intersection
			result.and(fact);
		}
	}
	
	public boolean same(BitSet fact1, BitSet fact2) {
		return fact1.equals(fact2);
	}
	
	//@Override
	public boolean isFactValid(BitSet fact) {
		return !isTop(fact) && !isBottom(fact);
	}
	
	public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, BitSet fact)
		throws DataflowAnalysisException {

		if (!basicBlock.isNullCheck())
			return;
		
		ValueNumberFrame vnaFrame = vnaDataflow.getFactAtLocation(new Location(handle, basicBlock));
		if (!vnaFrame.isValid()) {
			// Probably dead code.
			// Assume this location can't be reached.
			makeFactTop(fact);
			return;
		}
		
		ValueNumber instance = vnaFrame.getInstance(handle.getInstruction(), methodGen.getConstantPool());
		Integer param = valueNumberToParamMap.get(instance);
		if (param == null)
			return;
		
		boolean isParam;
		MergeTree mergeTree = vnaDataflow.getAnalysis().getMergeTree();
		if (mergeTree != null) {
			// Check to see what parameters might have flowed into the
			// checked value.
			BitSet inputSet = mergeTree.getTransitiveInputSet(instance);
			BitSet valueNumbersCheckedHere = new BitSet();
			valueNumbersCheckedHere.or(inputSet);
			valueNumbersCheckedHere.and(paramValueNumberSet);
			
			if (!valueNumbersCheckedHere.isEmpty()) {
				for (Iterator<Map.Entry<ValueNumber, Integer>> i = valueNumberToParamMap.entrySet().iterator();
						i.hasNext();) {
					Map.Entry<ValueNumber, Integer> entry = i.next();
					// If the value number of this parameter is one of those
					// which flow into the checked value...
					if (valueNumbersCheckedHere.get(entry.getKey().getNumber())) {
						// Add the corresponding parameter index to the dataflow fact
						fact.set(entry.getValue().intValue());
					}
				}
			}
		} else {
			// No merge tree.  Just look for parameters checked explicitly
			if (paramValueNumberSet.get(instance.getNumber())) {
				fact.set(valueNumberToParamMap.get(instance).intValue());
			}
		}
	}
	
	private boolean isTop(BitSet fact) {
		return fact.get(topBit);
	}
	
	private boolean isBottom(BitSet fact) {
		return fact.get(bottomBit);
	}
}
