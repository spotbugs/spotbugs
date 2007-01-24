/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

import java.util.HashMap;

import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.BlockOrder;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.DataflowAnalysis;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.ReverseDFSOrder;
import edu.umd.cs.findbugs.ba.ReverseDepthFirstSearch;

/**
 * @author David Hovemeyer
 */
public class ReturnPathTypeAnalysis implements DataflowAnalysis<ReturnPathType> {
	private CFG cfg;
	private DepthFirstSearch dfs;
	private ReverseDepthFirstSearch rdfs;
	private HashMap<BasicBlock, ReturnPathType> startFactMap;
	private HashMap<BasicBlock, ReturnPathType> resultFactMap;

	/**
	 * Constructor.
	 * 
	 * @param cfg  the method's CFG
	 * @param rdfs a ReverseDepthFirstSearch on the method's CFG
	 * @param dfs  a DepthFirstSearch on the method's CFG
	 */
	public ReturnPathTypeAnalysis(CFG cfg, ReverseDepthFirstSearch rdfs, DepthFirstSearch dfs) {
		this.cfg = cfg;
		this.dfs = dfs;
		this.rdfs = rdfs;
		this.startFactMap = new HashMap<BasicBlock, ReturnPathType>();
		this.resultFactMap = new HashMap<BasicBlock, ReturnPathType>();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#copy(java.lang.Object, java.lang.Object)
	 */
	public void copy(ReturnPathType source, ReturnPathType dest) {
		dest.copyFrom(source);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#createFact()
	 */
	public ReturnPathType createFact() {
		ReturnPathType fact = new ReturnPathType();
		fact.setTop();
		return fact;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#edgeTransfer(edu.umd.cs.findbugs.ba.Edge, java.lang.Object)
	 */
	public void edgeTransfer(Edge edge, ReturnPathType fact) {
		/**
		 * Exception edges targeting the CFG exit block
		 * indicate an abnormal return.
		 */
		if (edge.isExceptionEdge() && edge.getTarget() == cfg.getExit()) {
			fact.setCanReturnNormally(false);
		}
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#finishIteration()
	 */
	public void finishIteration() {
		// nothing to do
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#getBlockOrder(edu.umd.cs.findbugs.ba.CFG)
	 */
	public BlockOrder getBlockOrder(CFG cfg) {
		return new ReverseDFSOrder(cfg, rdfs, dfs);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#getLastUpdateTimestamp(java.lang.Object)
	 */
	public int getLastUpdateTimestamp(ReturnPathType fact) {
		return fact.getTimestamp();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#getResultFact(edu.umd.cs.findbugs.ba.BasicBlock)
	 */
	public ReturnPathType getResultFact(BasicBlock block) {
		return getOrCreateFact(resultFactMap, block);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#getStartFact(edu.umd.cs.findbugs.ba.BasicBlock)
	 */
	public ReturnPathType getStartFact(BasicBlock block) {
		return getOrCreateFact(startFactMap, block);
	}

	/**
	 * Look up a dataflow value in given map,
	 * creating a new (top) fact if no fact currently exists.
	 * 
	 * @param map   the map
	 * @param block BasicBlock used as key in map
	 * @return the dataflow fact for the block
	 */
	private ReturnPathType getOrCreateFact(HashMap<BasicBlock, ReturnPathType> map, BasicBlock block) {
		ReturnPathType returnPathType = map.get(block);
		if (returnPathType == null) {
			returnPathType = createFact();
			map.put(block, returnPathType);
		}
		return returnPathType;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#initEntryFact(java.lang.Object)
	 */
	public void initEntryFact(ReturnPathType result)
			throws DataflowAnalysisException {
		// By default, we assume that paths reaching the exit block
		// can return normally.  If we traverse exception edges which
		// lead to the exit block, then we'll mark those as "can't return
		// normally" (see edgeTransfer() method).
		
		result.setCanReturnNormally(true);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#initResultFact(java.lang.Object)
	 */
	public void initResultFact(ReturnPathType result) {
		makeFactTop(result);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#isForwards()
	 */
	public boolean isForwards() {
		return false;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#isTop(java.lang.Object)
	 */
	public boolean isTop(ReturnPathType fact) {
		return fact.isTop();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#makeFactTop(java.lang.Object)
	 */
	public void makeFactTop(ReturnPathType fact) {
		fact.setTop();
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#meetInto(java.lang.Object, edu.umd.cs.findbugs.ba.Edge, java.lang.Object)
	 */
	public void meetInto(ReturnPathType fact, Edge edge, ReturnPathType result)
			throws DataflowAnalysisException {
		result.mergeWith(fact);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#same(java.lang.Object, java.lang.Object)
	 */
	public boolean same(ReturnPathType fact1, ReturnPathType fact2) {
		return fact1.sameAs(fact2);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#setLastUpdateTimestamp(java.lang.Object, int)
	 */
	public void setLastUpdateTimestamp(ReturnPathType fact, int timestamp) {
		fact.setTimestamp(timestamp);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#startIteration()
	 */
	public void startIteration() {
		// nothing to do
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.DataflowAnalysis#transfer(edu.umd.cs.findbugs.ba.BasicBlock, org.apache.bcel.generic.InstructionHandle, java.lang.Object, java.lang.Object)
	 */
	public void transfer(BasicBlock basicBlock, InstructionHandle end,
			ReturnPathType start, ReturnPathType result)
			throws DataflowAnalysisException {
		// nothing to do
	}

}
