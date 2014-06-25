/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006-2007 University of Maryland
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

import edu.umd.cs.findbugs.ba.BasicAbstractDataflowAnalysis;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.BlockOrder;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.DepthFirstSearch;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.ReverseDFSOrder;
import edu.umd.cs.findbugs.ba.ReverseDepthFirstSearch;

/**
 * A dataflow analysis to determine, at each location in a method's CFG, whether
 * or not it is possible to return normally at that location.
 *
 * @author David Hovemeyer
 */
public class ReturnPathTypeAnalysis extends BasicAbstractDataflowAnalysis<ReturnPathType> {
    private final CFG cfg;

    private final DepthFirstSearch dfs;

    private final ReverseDepthFirstSearch rdfs;

    /**
     * Constructor.
     *
     * @param cfg
     *            the method's CFG
     * @param rdfs
     *            a ReverseDepthFirstSearch on the method's CFG
     * @param dfs
     *            a DepthFirstSearch on the method's CFG
     */
    public ReturnPathTypeAnalysis(CFG cfg, ReverseDepthFirstSearch rdfs, DepthFirstSearch dfs) {
        this.cfg = cfg;
        this.dfs = dfs;
        this.rdfs = rdfs;
    }

    @Override
    public void copy(ReturnPathType source, ReturnPathType dest) {
        dest.copyFrom(source);
    }

    @Override
    public ReturnPathType createFact() {
        ReturnPathType fact = new ReturnPathType();
        fact.setTop();
        return fact;
    }

    @Override
    public void edgeTransfer(Edge edge, ReturnPathType fact) {
        // The edges leading into the exit block create the "seed" values
        // for the analysis. The exception edges create values indicating
        // that a normal (non-exception) return is not possible,
        // while the non-exception edges create values indicating that
        // a normal return is possible.
        if (edge.getTarget() == cfg.getExit()) {
            fact.setCanReturnNormally(!edge.isExceptionEdge());
        }
    }

    @Override
    public void finishIteration() {
        // nothing to do
    }

    @Override
    public BlockOrder getBlockOrder(CFG cfg) {
        return new ReverseDFSOrder(cfg, rdfs, dfs);
    }

    @Override
    public int getLastUpdateTimestamp(ReturnPathType fact) {
        return 0;
    }

    /*
     * Look up a dataflow value in given map, creating a new (top) fact if no
     * fact currently exists.
     *
     * @param map
     *            the map
     * @param block
     *            BasicBlock used as key in map
     * @return the dataflow fact for the block
     *
    private ReturnPathType getOrCreateFact(HashMap<BasicBlock, ReturnPathType> map, BasicBlock block) {
        ReturnPathType returnPathType = map.get(block);
        if (returnPathType == null) {
            returnPathType = createFact();
            map.put(block, returnPathType);
        }
        return returnPathType;
    }
     */

    @Override
    public void initEntryFact(ReturnPathType result) throws DataflowAnalysisException {
        // By default, we assume that paths reaching the exit block
        // can return normally. If we traverse exception edges which
        // lead to the exit block, then we'll mark those as "can't return
        // normally" (see edgeTransfer() method).

        result.setCanReturnNormally(true);
    }

    @Override
    public boolean isForwards() {
        return false;
    }

    @Override
    public boolean isTop(ReturnPathType fact) {
        return fact.isTop();
    }

    @Override
    public void makeFactTop(ReturnPathType fact) {
        fact.setTop();
    }

    @Override
    public void meetInto(ReturnPathType fact, Edge edge, ReturnPathType result) throws DataflowAnalysisException {
        result.mergeWith(fact);
    }

    @Override
    public boolean same(ReturnPathType fact1, ReturnPathType fact2) {
        return fact1.sameAs(fact2);
    }

    @Override
    public void setLastUpdateTimestamp(ReturnPathType fact, int timestamp) {
        // ignore
    }

    @Override
    public void startIteration() {
        // nothing to do
    }

    @Override
    public void transfer(BasicBlock basicBlock, InstructionHandle end, ReturnPathType start, ReturnPathType result)
            throws DataflowAnalysisException {
        // just copy the start fact
        result.copyFrom(start);
    }

}
