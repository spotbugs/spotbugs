/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003-2007 University of Maryland
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

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeSet;

import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.deref.UnconditionalValueDerefAnalysis;
import edu.umd.cs.findbugs.ba.deref.UnconditionalValueDerefDataflow;
import edu.umd.cs.findbugs.ba.deref.UnconditionalValueDerefSet;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;

/**
 * Perform dataflow analysis on a method using a control flow graph. Both
 * forward and backward analyses can be performed.
 * <ul>
 * <li>The "start" point of each block is the entry (forward analyses) or the
 * exit (backward analyses).
 * <li>The "result" point of each block is the exit (forward analyses) or the
 * entry (backward analyses).
 * </ul>
 * The analysis's transfer function is applied to transform the meet of the
 * results of the block's logical predecessors (the block's start facts) into
 * the block's result facts.
 *
 * @author David Hovemeyer
 * @see CFG
 * @see DataflowAnalysis
 */
public class Dataflow<Fact, AnalysisType extends DataflowAnalysis<Fact>> {
    private final CFG cfg;

    private final AnalysisType analysis;

    private final BlockOrder blockOrder;

    private final boolean isForwards;

    private int numIterations;

    public static boolean DEBUG = SystemProperties.getBoolean("dataflow.debug");

    /**
     * Constructor.
     *
     * @param cfg
     *            the control flow graph
     * @param analysis
     *            the DataflowAnalysis to be run
     */
    public Dataflow(CFG cfg, AnalysisType analysis) {
        this.cfg = cfg;
        this.analysis = analysis;
        blockOrder = analysis.getBlockOrder(cfg);
        isForwards = analysis.isForwards();
        numIterations = 0;

        // Initialize result facts
        Iterator<BasicBlock> i = cfg.blockIterator();
        while (i.hasNext()) {
            BasicBlock block = i.next();

            Fact result = analysis.getResultFact(block);
            if (block == logicalEntryBlock()) {
                try {
                    // Entry block: set to entry fact
                    analysis.initEntryFact(result);
                } catch (DataflowAnalysisException e) {
                    analysis.makeFactTop(result);
                }
            } else {
                // Set to top
                analysis.makeFactTop(result);
            }
        }
    }

    // Maximum number of iterations before we assume there is a bug and give up.
    private static final int MAX_ITERS = SystemProperties.getInt("dataflow.maxiters", 97);

    private String getFullyQualifiedMethodName() {
        String methodName;
        MethodGen methodGen = cfg.getMethodGen();
        if (methodGen == null) {
            methodName = cfg.getMethodName();
        } else {
            methodName = SignatureConverter.convertMethodSignature(methodGen);
        }
        return methodName;
    }

    static class ForwardProgramOrder implements Comparator<BasicBlock>, Serializable {

        @Override
        public int compare(BasicBlock o1, BasicBlock o2) {
            int p1 = o1.getLabel();
            int p2 = o2.getLabel();
            return p1 - p2;
        }

    }

    static class BackwardProgramOrder extends ForwardProgramOrder {

        @Override
        public int compare(BasicBlock o1, BasicBlock o2) {
            return super.compare(o2, o1);
        }

    }

    /**
     * Run the algorithm. Afterwards, caller can use the getStartFact() and
     * getResultFact() methods to to get dataflow facts at start and result
     * points of each block.
     */
    public void execute() throws DataflowAnalysisException {
        boolean change;
        boolean debugWas = DEBUG;
        if (DEBUG) {
            reportAnalysis("Executing");
        }

        int timestamp = 0;
        boolean firstTime = true;
        do {
            change = false;
            boolean sawBackEdge = false;
            ++numIterations;
            if (numIterations > MAX_ITERS && !DEBUG) {
                DEBUG = true;
                reportAnalysis("Too many iterations");
                System.out.println(this.getClass().getName());
                if (this.getClass() == UnconditionalValueDerefDataflow.class || this.getClass() == LiveLocalStoreDataflow.class) {
                    try {
                        ClassContext cc = Global.getAnalysisCache().getClassAnalysis(ClassContext.class,
                                DescriptorFactory.createClassDescriptorFromDottedClassName(cfg.getMethodGen().getClassName()));
                        System.out.println("Forwards cfg");
                        CFGPrinter printer = new CFGPrinter(cfg);
                        printer.setIsForwards(true);
                        printer.print(System.out);
                        System.out.println("Backwards cfg");
                        printer = new CFGPrinter(cfg);
                        printer.setIsForwards(false);
                        printer.print(System.out);
                        cc.dumpSimpleDataflowInformation(cfg.getMethodGen().getMethod());
                    } catch (CheckedAnalysisException e) {
                        e.printStackTrace(System.out);
                    }
                }
            }

            if (DEBUG) {
                System.out.println("----------------------------------------------------------------------");
                System.out.println(this.getClass().getName() + " iteration: " + numIterations + ", timestamp: " + timestamp);
                MethodGen mg = cfg.getMethodGen();
                System.out.println(mg.getClassName() + "." + mg.getName() + mg.getSignature());
                System.out.println("----------------------------------------------------------------------");

            }

            if (numIterations >= MAX_ITERS + 9) {
                throw new DataflowAnalysisException("Too many iterations (" + numIterations + ") in dataflow when analyzing "
                        + getFullyQualifiedMethodName());
            }

            analysis.startIteration();

            if (DEBUG && firstTime && blockOrder instanceof ReverseDFSOrder) {
                ReverseDFSOrder rBlockOrder = (ReverseDFSOrder) blockOrder;
                System.out.println("Entry point is: " + logicalEntryBlock());
                System.out.println("Basic block order: ");
                Iterator<BasicBlock> i = blockOrder.blockIterator();
                while (i.hasNext()) {

                    BasicBlock block = i.next();
                    debug(block, "rBlockOrder " + rBlockOrder.rdfs.getDiscoveryTime(block) + "\n");
                }
            }
            Iterator<BasicBlock> i = blockOrder.blockIterator();
            if (numIterations > 3 && numIterations % 2 == 0 && blockOrder instanceof ReverseDFSOrder) {
                if (DEBUG) {
                    System.out.println("Trying program order");
                }
                TreeSet<BasicBlock> bb = new TreeSet<BasicBlock>(new BackwardProgramOrder());
                Iterator<BasicBlock> j = blockOrder.blockIterator();
                while (j.hasNext()) {
                    BasicBlock block = j.next();
                    bb.add(block);
                }
                if (DEBUG) {
                    for (BasicBlock block : bb) {
                        debug(block, "\n");
                    }
                }
                i = bb.iterator();
            }
            if (DEBUG) {
                dumpDataflow(analysis);
            }

            // For each block in CFG...

            while (i.hasNext()) {

                BasicBlock block = i.next();

                // Get start fact for block.
                Fact start = analysis.getStartFact(block);
                assert start != null;

                boolean needToRecompute = false;
                // Get result facts for block,
                Fact result = analysis.getResultFact(block);
                assert result != null;

                int originalResultTimestamp = analysis.getLastUpdateTimestamp(result);

                // Meet all of the logical predecessor results into this block's
                // start.
                // Special case: if the block is the logical entry, then it gets
                // the special "entry fact".
                if (block == logicalEntryBlock()) {
                    analysis.makeFactTop(start);
                    analysis.initEntryFact(start);
                    if (DEBUG) {
                        debug(block, "Init entry fact ==> " + analysis.factToString(start) + "\n");
                    }
                    needToRecompute = true;
                } else {
                    int lastCalculated = analysis.getLastUpdateTimestamp(start);
                    Iterator<Edge> predEdgeIter = logicalPredecessorEdgeIterator(block);

                    int predCount = 0;
                    int rawPredCount = 0;
                    while (predEdgeIter.hasNext()) {
                        Edge edge = predEdgeIter.next();
                        rawPredCount++;
                        if (needToRecompute) {
                            // don't need to check to see if we need to recompute.
                            if (firstTime && !sawBackEdge) {
                                // may need to se sawBackEdge
                            } else {
                                continue;
                            }

                        }
                        BasicBlock logicalPred = isForwards ? edge.getSource() : edge.getTarget();

                        int direction = blockOrder.compare(block, logicalPred);

                        if (DEBUG) {
                            debug(block, "direction " + direction + " for " + blockId(logicalPred) + "\n");
                        }
                        if (direction < 0) {
                            sawBackEdge = true;
                        }

                        // Get the predecessor result fact
                        Fact predFact = analysis.getResultFact(logicalPred);
                        int predLastUpdated = analysis.getLastUpdateTimestamp(predFact);
                        if (!analysis.isTop(predFact)) {
                            predCount++;
                            if (predLastUpdated >= lastCalculated) {

                                needToRecompute = true;
                                if (DEBUG) {
                                    debug(block, "\n Need to recompute. My timestamp = " + lastCalculated + ", pred timestamp = "
                                            + predLastUpdated + ",\n   pred fact = " + predFact + "\n");
                                }
                                // break;
                            }
                        }
                    }
                    if (predCount == 0) {
                        needToRecompute = true;
                    }

                    if (!needToRecompute) {
                        continue;
                    }

                    analysis.makeFactTop(start);
                    predEdgeIter = logicalPredecessorEdgeIterator(block);
                    while (predEdgeIter.hasNext()) {
                        Edge edge = predEdgeIter.next();
                        BasicBlock logicalPred = isForwards ? edge.getSource() : edge.getTarget();

                        // Get the predecessor result fact
                        Fact predFact = analysis.getResultFact(logicalPred);

                        // Apply the edge transfer function.
                        Fact edgeFact = analysis.createFact();
                        analysis.copy(predFact, edgeFact);
                        analysis.edgeTransfer(edge, edgeFact);

                        if (DEBUG && !analysis.same(edgeFact, predFact)) {
                            debug(block, logicalPred, edge, "Edge transfer " + analysis.factToString(predFact) + " ==> "
                                    + analysis.factToString(edgeFact));
                        }

                        // Merge the predecessor fact (possibly transformed
                        // by the edge transfer function)
                        // into the block's start fact.
                        if (DEBUG) {
                            if (analysis.isTop(start)) {
                                debug(block, logicalPred, edge, "\n  First pred is " + analysis.factToString(edgeFact)
                                        + "\n   last updated at " + analysis.getLastUpdateTimestamp(predFact) + "\n");
                            } else {
                                debug(block, logicalPred, edge, "\n  Meet " + analysis.factToString(start) + "\n   with "
                                        + analysis.factToString(edgeFact)

                                        + "\n   pred last updated at " + analysis.getLastUpdateTimestamp(predFact) + "\n");
                            }
                        }

                        if (analysis instanceof UnconditionalValueDerefAnalysis) {
                            ((UnconditionalValueDerefAnalysis) analysis).meetInto((UnconditionalValueDerefSet) edgeFact,
                                    edge, (UnconditionalValueDerefSet) start, rawPredCount == 1);
                        } else {
                            analysis.meetInto(edgeFact, edge, start);
                        }
                        analysis.setLastUpdateTimestamp(start, timestamp);

                        int pos = -1;
                        if (block.getFirstInstruction() != null) {
                            pos = block.getFirstInstruction().getPosition();
                        }
                        if (DEBUG) {
                            System.out.println(" [" + pos + "]==> " + analysis.factToString(start) + " @ " + timestamp
                                    + " \n");
                        }
                    }
                }
                if (DEBUG) {
                    debug(block, "start fact is " + analysis.factToString(start) + "\n");
                }

                // making a copy of result facts (so we can detect if it
                // changed).
                boolean resultWasTop = analysis.isTop(result);
                Fact origResult = null;
                if (!resultWasTop) {
                    origResult = analysis.createFact();
                    analysis.copy(result, origResult);
                }

                //                if (true || analysis.isTop(start)) {
                // Apply the transfer function.

                analysis.transfer(block, null, start, result);
                //                } else {
                //                    analysis.copy(start, result);
                //                }

                if (DEBUG && SystemProperties.getBoolean("dataflow.blockdebug")) {
                    debug(block, "Dumping flow values for block:\n");
                    Iterator<org.apache.bcel.generic.InstructionHandle> ii = block.instructionIterator();
                    while (ii.hasNext()) {
                        org.apache.bcel.generic.InstructionHandle handle = ii.next();
                        Fact tmpResult = analysis.createFact();
                        analysis.transfer(block, handle, start, tmpResult);
                        System.out.println("\t" + handle + " " + analysis.factToString(tmpResult));
                    }
                }

                // See if the result changed.
                if (DEBUG) {
                    debug(block, "orig result is " + (origResult == null ? "TOP" : analysis.factToString(origResult)) + "\n");
                }
                boolean thisResultChanged = false;
                if (resultWasTop) {
                    thisResultChanged = !analysis.isTop(result);
                } else {
                    thisResultChanged = !analysis.same(result, origResult);
                }
                if (thisResultChanged) {
                    timestamp++;
                    if (DEBUG) {
                        debug(block, "result changed at timestamp " + timestamp + "\n");
                    }
                    if (DEBUG && !needToRecompute) {
                        System.out.println("I thought I didn't need to recompute");
                    }
                    change = true;
                    analysis.setLastUpdateTimestamp(result, timestamp);
                } else {
                    analysis.setLastUpdateTimestamp(result, originalResultTimestamp);
                }

                if (DEBUG) {
                    debug(block,
                            "result is " + analysis.factToString(result) + " @ timestamp "
                                    + analysis.getLastUpdateTimestamp(result) + "\n");
                }
            }

            analysis.finishIteration();
            if (!sawBackEdge) {
                break;
            }

        } while (change);

        if (DEBUG) {
            System.out.println("-- Quiescence achieved-------------------------------------------------");
            System.out.println(this.getClass().getName() + " iteration: " + numIterations + ", timestamp: " + timestamp);
            MethodGen mg = cfg.getMethodGen();
            System.out.println(mg.getClassName() + "." + mg.getName() + mg.getSignature());
            new RuntimeException("Quiescence achieved----------------------------------------------------------------")
            .printStackTrace(System.out);

        }
        DEBUG = debugWas;
    }

    private void reportAnalysis(String msg) {
        String shortAnalysisName = analysis.getClass().getName();
        int pkgEnd = shortAnalysisName.lastIndexOf('.');
        if (pkgEnd >= 0) {
            shortAnalysisName = shortAnalysisName.substring(pkgEnd + 1);
        }
        System.out.println(msg + " " + shortAnalysisName + " on " + getFullyQualifiedMethodName());
    }

    private static String blockId(BasicBlock bb) {
        InstructionHandle handle = bb.getFirstInstruction();
        if (handle == null) {
            return "" + bb.getLabel();
        }
        return bb.getLabel() + ":" + handle.getPosition() + " " + handle.getInstruction();
    }

    private static void debug(BasicBlock bb, String msg) {

        System.out.print("Dataflow (block " + blockId(bb) + "): " + msg);
    }

    private static void debug(BasicBlock bb, BasicBlock pred, Edge edge, String msg) {
        System.out.print("Dataflow (block " + blockId(bb) + ", predecessor " + blockId(pred) + " ["
                + Edge.edgeTypeToString(edge.getType()) + "]): " + msg);
    }

    /**
     * Return the number of iterations of the main execution loop.
     */
    public int getNumIterations() {
        return numIterations;
    }

    /**
     * Get dataflow facts for start of given block.
     */
    public Fact getStartFact(BasicBlock block) {
        return analysis.getStartFact(block);
    }

    /**
     * Get dataflow facts for end of given block.
     */
    public Fact getResultFact(BasicBlock block) {
        return analysis.getResultFact(block);
    }

    /**
     * Get dataflow fact at (just before) given Location. Note "before" is meant
     * in the logical sense, so for backward analyses, before means after the
     * location in the control flow sense.
     *
     * @param location
     *            the Location
     * @return the dataflow value at given Location
     * @throws DataflowAnalysisException
     */
    public/* final */Fact getFactAtLocation(Location location) throws DataflowAnalysisException {
        return analysis.getFactAtLocation(location);
    }

    /**
     * Get the dataflow fact representing the point just after given Location.
     * Note "after" is meant in the logical sense, so for backward analyses,
     * after means before the location in the control flow sense.
     *
     * @param location
     *            the Location
     * @return the dataflow value after given Location
     * @throws DataflowAnalysisException
     */
    public/* final */Fact getFactAfterLocation(Location location) throws DataflowAnalysisException {
        return analysis.getFactAfterLocation(location);
    }

    /**
     * Get the fact that is true on the given control edge.
     *
     * @param edge
     *            the edge
     * @return the fact that is true on the edge
     * @throws DataflowAnalysisException
     */
    public Fact getFactOnEdge(Edge edge) throws DataflowAnalysisException {
        return analysis.getFactOnEdge(edge);
    }

    /**
     * Get the analysis object.
     */
    public AnalysisType getAnalysis() {
        return analysis;
    }

    /**
     * Get the CFG object.
     */
    public CFG getCFG() {
        return cfg;
    }

    /**
     * Return an Iterator over edges that connect given block to its logical
     * predecessors. For forward analyses, this is the incoming edges. For
     * backward analyses, this is the outgoing edges.
     */
    private Iterator<Edge> logicalPredecessorEdgeIterator(BasicBlock block) {
        return isForwards ? cfg.incomingEdgeIterator(block) : cfg.outgoingEdgeIterator(block);
    }

    /**
     * Get the "logical" entry block of the CFG. For forward analyses, this is
     * the entry block. For backward analyses, this is the exit block.
     */
    private BasicBlock logicalEntryBlock() {
        return isForwards ? cfg.getEntry() : cfg.getExit();
    }

    public void dumpDataflow(AnalysisType analysis) {
        System.out.println(this.getClass().getName() + " analysis for " + getCFG().getMethodName() + getCFG().getMethodSig()
                + " { ");
        try {

            for (Location loc : getCFG().orderedLocations()) {
                System.out.println("\nBefore: " + analysis.factToString(getFactAtLocation(loc)));
                System.out.println("Location: " + loc);
                System.out.println("After: " + analysis.factToString(getFactAfterLocation(loc)));
            }
        } catch (DataflowAnalysisException e) {
            AnalysisContext.logError("error dumping dataflow analysis", e);
            System.out.println(e);
        }
        System.out.println("}");
    }
}
