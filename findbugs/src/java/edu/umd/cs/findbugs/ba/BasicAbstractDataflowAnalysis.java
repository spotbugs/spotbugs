/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2006, University of Maryland
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

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.bcel.generic.InstructionHandle;

/**
 * A useful starting point for defining a dataflow analysis.
 * Handles access and caching of start and result facts for
 * basic blocks.
 * 
 * <p>
 * Subclasses that model instructions within basic blocks
 * should extend AbstractDataflowAnalysis.
 * </p>
 * 
 * @author David Hovemeyer
 */
public abstract class BasicAbstractDataflowAnalysis<Fact> implements DataflowAnalysis<Fact> {
	IdentityHashMap<BasicBlock, Fact> startFactMap;
	IdentityHashMap<BasicBlock, Fact> resultFactMap;

	/**
	 * Constructor.
	 */
	public BasicAbstractDataflowAnalysis() {
		this.startFactMap = new IdentityHashMap<BasicBlock, Fact>();
		this.resultFactMap = new IdentityHashMap<BasicBlock, Fact>();
	}

	/**
	 * Get an iterator over the result facts.
	 */
	public Iterator<Fact> resultFactIterator() {
		return resultFactMap.values().iterator();
	}

	/**
	 * Call this to get a dataflow value as a String.
	 * By default, we just call toString().
	 * Subclasses may override to get different behavior.
	 */
	public String factToString(Fact fact) {
		return fact.toString();
	}

	public Fact getStartFact(BasicBlock block) {
		return lookupOrCreateFact(startFactMap, block);
	}

	public Fact getResultFact(BasicBlock block) {
		return lookupOrCreateFact(resultFactMap, block);
	}

	/**
	 * Get dataflow fact at (just before) given Location.
	 * Note "before" is meant in the logical sense, so for backward analyses,
	 * before means after the location in the control flow sense.
	 * 
	 * <p>
	 * The default implementation ignores instructions within basic blocks.
	 * Subclasses that model individual instructions must override this method.
	 * </p> 
	 * 
	 * @param location the Location
	 * @return the dataflow value at given Location
	 * @throws DataflowAnalysisException
	 */
	public Fact getFactAtLocation(Location location) throws DataflowAnalysisException {
		return getStartFact(location.getBasicBlock());
	}

	/**
	 * Get the dataflow fact representing the point just after given Location.
	 * Note "after" is meant in the logical sense, so for backward analyses,
	 * after means before the location in the control flow sense.
	 * 
	 * <p>
	 * The default implementation ignores instructions within basic blocks.
	 * Subclasses that model individual instructions must override this method.
	 * </p> 
	 * 
	 * @param location the Location
	 * @return the dataflow value after given Location
	 * @throws DataflowAnalysisException
	 */
	public Fact getFactAfterLocation(Location location) throws DataflowAnalysisException {
		if (location.getBasicBlock().isEmpty()) {
			return getResultFact(location.getBasicBlock());
		}
		
		InstructionHandle logicalLastInstruction = isForwards()
			? location.getBasicBlock().getLastInstruction()
			: location.getBasicBlock().getFirstInstruction();
			
		if (location.getHandle() == logicalLastInstruction) {
			return getResultFact(location.getBasicBlock());
		} else {
			return getStartFact(location.getBasicBlock());
		}
	}
	
	/**
	 * Get the fact that is true on the given control edge.
	 * 
	 * @param edge the edge
	 * @return the fact that is true on the edge
	 * @throws DataflowAnalysisException 
	 */
	public Fact getFactOnEdge(Edge edge) throws DataflowAnalysisException {
		BasicBlock block = isForwards() ? edge.getSource() : edge.getTarget();
		Fact fact = createFact();
		makeFactTop(fact);
		meetInto(getResultFact(block), edge, fact);
		return fact;
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#startIteration()
	 */
	public void startIteration() {
		// Do nothing - subclass may override
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#finishIteration()
	 */
	public void finishIteration() {
		// Do nothing - subclass may override
	}

	private Fact lookupOrCreateFact(Map<BasicBlock, Fact> map, BasicBlock block) {
		Fact fact = map.get(block);
		if (fact == null) {
			fact = createFact();
			map.put(block, fact);
		}
		return fact;
	}
}
