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

import org.apache.bcel.generic.InstructionHandle;
import java.util.*;

/**
 * Abstract base class providing functionality that will be useful
 * for most dataflow analysis implementations.  In particular, it implements
 * the meetPredecessorFacts() and transfer() functions by calling down
 * to the meetInto() and transferInstruction() functions, respectively.
 * It also maintains a map of the dataflow fact for every instruction,
 * which is useful when using the results of the analysis.
 *
 * @see Dataflow
 * @see DataflowAnalysis
 * @author David Hovemeyer
 */
public abstract class AbstractDataflowAnalysis<Fact> implements DataflowAnalysis<Fact> {
	private IdentityHashMap<InstructionHandle, Fact> factAtInstructionMap = new IdentityHashMap<InstructionHandle, Fact>();

	/* ----------------------------------------------------------------------
	 * Public methods
	 * ---------------------------------------------------------------------- */

	/**
	 * Meet one fact into another.
	 * @param fact the source fact
	 * @param edge the incoming control edge that produced the source fact
	 * @param result the result whose value should be met with the source fact
	 */
	public abstract void meetInto(Fact fact, Edge edge, Fact result) throws DataflowAnalysisException;

	/**
	 * Transfer function for a single instruction.
	 * @param handle the instruction
	 * @param fact which should be modified based on the instruction
	 */
	public abstract void transferInstruction(InstructionHandle handle, Fact fact) throws DataflowAnalysisException;

	/**
	 * Get the dataflow fact representing the point just before given instruction.
	 * Note "before" is meant in the logical sense, so for backward analyses,
	 * before means after the instruction in the control flow sense.
	 * @param handle the instruction
	 * @return the fact at the point just before the instruction
	 */
	public Fact getFactAtInstruction(InstructionHandle handle) {
		Fact fact = factAtInstructionMap.get(handle);
		if (fact == null) {
			fact = createFact();
			factAtInstructionMap.put(handle, fact);
		}
		return fact;
	}

	/* ----------------------------------------------------------------------
	 * Implementations of interface methods
	 * ---------------------------------------------------------------------- */

	public void meetPredecessorFacts(BasicBlock basicBlock, List<Edge> predEdgeList, List<Fact> predFactList, Fact start)
		throws DataflowAnalysisException {
		if (predEdgeList.size() != predFactList.size())
			throw new IllegalArgumentException("pred edge list and fact list are not the same size");

		Iterator<Edge> edgeIter = predEdgeList.iterator();
		Iterator<Fact> factIter = predFactList.iterator();

		while (edgeIter.hasNext() && factIter.hasNext()) {
			Edge edge = edgeIter.next();
			Fact fact = factIter.next();
			meetInto(fact, edge, start);
		}
	}

	public void transfer(BasicBlock basicBlock, InstructionHandle end, Fact start, Fact result) throws DataflowAnalysisException {
		copy(start, result);

		if (isFactValid(result)) {
			Iterator<InstructionHandle> i = isForwards() ? basicBlock.instructionIterator() : basicBlock.instructionReverseIterator();
			while (i.hasNext()) {
				InstructionHandle handle = i.next();
				if (handle == end)
					break;
	
				// Record the fact at this instruction
				Fact factAtInstruction  = getFactAtInstruction(handle);
				copy(result, factAtInstruction);
	
				// Transfer the dataflow value
				transferInstruction(handle, result);
			}
		}
	}

}

// vim:ts=4
