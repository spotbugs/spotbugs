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

import java.util.Iterator;

import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.deref.UnconditionalValueDerefAnalysis;
import edu.umd.cs.findbugs.ba.deref.UnconditionalValueDerefSet;
import edu.umd.cs.findbugs.ba.npe.UnconditionalDerefAnalysis;

/**
 * Perform dataflow analysis on a method using a control flow graph.
 * Both forward and backward analyses can be performed.
 * <ul>
 * <li> The "start" point of each block is the entry (forward analyses)
 * or the exit (backward analyses).
 * <li> The "result" point of each block is the exit (forward analyses)
 * or the entry (backward analyses).
 * </ul>
 * The analysis's transfer function is applied to transform
 * the meet of the results of the block's logical predecessors
 * (the block's start facts) into the block's result facts.
 *
 * @author David Hovemeyer
 * @see CFG
 * @see DataflowAnalysis
 */
public class Dataflow <Fact, AnalysisType extends DataflowAnalysis<Fact>> {
	private CFG cfg;
	private AnalysisType analysis;
	private BlockOrder blockOrder;
	private boolean isForwards;
	private int numIterations;

	static final boolean DEBUG = SystemProperties.getBoolean("dataflow.debug");

	/**
	 * Constructor.
	 *
	 * @param cfg      the control flow graph
	 * @param analysis the DataflowAnalysis to be run
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
	private static final int MAX_ITERS = SystemProperties.getInteger("dataflow.maxiters", 100).intValue();

	private String getFullyQualifiedMethodName() {
		String methodName;
		MethodGen methodGen = cfg.getMethodGen();
		if (methodGen == null)
			methodName = cfg.getMethodName();
		else methodName = SignatureConverter.convertMethodSignature(methodGen);
		return methodName;
	}
	/**
	 * Run the algorithm.
	 * Afterwards, caller can use the getStartFact() and getResultFact() methods to
	 * to get dataflow facts at start and result points of each block.
	 */
	public void execute() throws DataflowAnalysisException {
		boolean change;

		if (DEBUG) {
			String shortAnalysisName = analysis.getClass().getName();
			int pkgEnd = shortAnalysisName.lastIndexOf('.');
			if (pkgEnd >= 0) {
				shortAnalysisName = shortAnalysisName.substring(pkgEnd + 1);
			}
			System.out.println("Executing " + shortAnalysisName + " on " + getFullyQualifiedMethodName());
		}

		int timestamp = 0;
		do {
			change = false;
			++numIterations;

			if (DEBUG) {
				System.out.println("----------------------------------------------------------------------");
				System.out.println(this.getClass().getName() + " iteration: " + numIterations + ", timestamp: " + timestamp);
				System.out.println("----------------------------------------------------------------------");
			}

			if (numIterations >= MAX_ITERS) {
				assert false : "Too many iterations (" + numIterations + ") in dataflow when analyzing " + getFullyQualifiedMethodName();
				break;
			}

			analysis.startIteration();

			if (DEBUG) {
				if (blockOrder instanceof ReverseDFSOrder) {
					ReverseDFSOrder rBlockOrder = (ReverseDFSOrder) blockOrder;
					System.out.println("Entry point is: " + logicalEntryBlock());
					System.out.println("Basic block order: ");
					Iterator<BasicBlock> i = blockOrder.blockIterator();
					while (i.hasNext()) {

						BasicBlock block = i.next();
						if (DEBUG) debug(block, "rBlockOrder " + rBlockOrder.rdfs.getDiscoveryTime(block) + "\n");
					}
				}
			}

			// For each block in CFG...
			Iterator<BasicBlock> i = blockOrder.blockIterator();
			while (i.hasNext()) {

				BasicBlock block = i.next();
				if (DEBUG) debug(block, "start\n");

				// Get start fact for block.
				Fact start = analysis.getStartFact(block);
				boolean needToRecompute = false;
				//				 Get result facts for block,
				Fact result = analysis.getResultFact(block);
				int originalResultTimestamp = analysis.getLastUpdateTimestamp(result);

				// Meet all of the logical predecessor results into this block's start.
				// Special case: if the block is the logical entry, then it gets
				// the special "entry fact".
				if (block == logicalEntryBlock()) {
					analysis.makeFactTop(start);
					analysis.initEntryFact(start);
					if (DEBUG) debug(block, "Init entry fact ==> " + start + "\n");
					needToRecompute = true;
				} else {
					int lastCalculated = analysis.getLastUpdateTimestamp(start);
					Iterator<Edge> predEdgeIter = logicalPredecessorEdgeIterator(block);

					int predCount = 0;
					int rawPredCount = 0;
					while (predEdgeIter.hasNext()) {
						Edge edge = predEdgeIter.next();
						rawPredCount++;
						if (needToRecompute) continue;
						BasicBlock logicalPred = isForwards ? edge.getSource() : edge.getTarget();

						// Get the predecessor result fact
						Fact predFact = analysis.getResultFact(logicalPred);
						int predLastUpdated = analysis.getLastUpdateTimestamp(predFact);
						if (!analysis.isTop(predFact)) {
							predCount++;
							if (predLastUpdated >= lastCalculated) {

							needToRecompute = true;
							if (DEBUG) {
							System.out.println("Need to recompute. My timestamp = " + lastCalculated + ", pred timestamp = " + predLastUpdated + ", pred fact = " + predFact);
							}
							// break;
							}
						}
					}
					if (predCount == 0) needToRecompute = true;

					if (!needToRecompute) {
						if (DEBUG) {
							debug(block, "Skipping: predecessors haven't changed");
							System.out.println(" curr timestamp: " + timestamp);
							System.out.println(" last timestamp: " + lastCalculated);
							predEdgeIter = logicalPredecessorEdgeIterator(block);

							while (predEdgeIter.hasNext()) {
								Edge edge = predEdgeIter.next();
								BasicBlock logicalPred = isForwards ? edge.getSource() : edge.getTarget();

								// Get the predecessor result fact
								Fact predFact = analysis.getResultFact(logicalPred);
								int predLastUpdated = analysis.getLastUpdateTimestamp(predFact);
								System.out.println(" pred timestamp: " + predLastUpdated);
								}
							System.out.println("Fact: " + start);
						}
						continue;
					}

					if (needToRecompute) {

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
								debug(block, logicalPred, edge,
										"Edge transfer " + predFact + " ==> " + edgeFact);
							}

							// Merge the predecessor fact (possibly transformed by the edge transfer function)
							// into the block's start fact.
							if (DEBUG) debug(block, logicalPred, edge, "\n  Meet " + start + "\n   with " + edgeFact 
									+ "\n   pred last updated at " +  analysis.getLastUpdateTimestamp(predFact) +"\n");


							if (analysis instanceof UnconditionalValueDerefAnalysis) {
								((UnconditionalValueDerefAnalysis)analysis).meetInto((UnconditionalValueDerefSet)edgeFact, edge, (UnconditionalValueDerefSet) start, rawPredCount==1);
							}
							else analysis.meetInto(edgeFact, edge, start);
							analysis.setLastUpdateTimestamp(start, timestamp);

							int pos = -1;
							if (block.getFirstInstruction() != null)
								pos = block.getFirstInstruction().getPosition();
							if (DEBUG) System.out.println(" [" + pos +"]==> " + start +" @ " + timestamp + " \n");
						}
					}
				}
				if (DEBUG) debug(block, "start fact is " + start + "\n");

				// making a copy of result facts (so we can detect if it changed).
				boolean resultWasTop = analysis.isTop(result);
				Fact origResult = null;
				if (!resultWasTop) {
					origResult = analysis.createFact();
					analysis.copy(result, origResult);
				}

				if (true || analysis.isTop(start)) {
					// Apply the transfer function.

					analysis.transfer(block, null, start, result);
				} else {
					analysis.copy(start, result);
				}

				if (DEBUG && SystemProperties.getBoolean("dataflow.blockdebug")) {
					debug(block, "Dumping flow values for block:\n");
					Iterator<org.apache.bcel.generic.InstructionHandle> ii = block.instructionIterator();
					while (ii.hasNext()) {
						org.apache.bcel.generic.InstructionHandle handle = ii.next();
						Fact tmpResult = analysis.createFact();
						analysis.transfer(block, handle, start, tmpResult);
						System.out.println("\t" + handle + " " + tmpResult);
					}
				}

				// See if the result changed.
				if (DEBUG) debug(block, "orig result is " + origResult + "\n");
				boolean thisResultChanged = false;
				if (resultWasTop)
					thisResultChanged = !analysis.isTop(result);
				else thisResultChanged = !analysis.same(result, origResult);
				if (thisResultChanged) {
					timestamp++;
					if (DEBUG) debug(block, "result changed at timestamp " + timestamp + "\n");
					if (DEBUG && !needToRecompute) {
						System.out.println("I thought I didn't need to recompute");
					}
					change = true;
					analysis.setLastUpdateTimestamp(result, timestamp);
				} else
					analysis.setLastUpdateTimestamp(result, originalResultTimestamp);

				if (DEBUG) debug(block, "result is " + result + " @ timestamp " 
						+ analysis.getLastUpdateTimestamp(result) + "\n");
			}

			analysis.finishIteration();
		} while (change);
	}

	private static String blockId(BasicBlock bb) {
		InstructionHandle handle = bb.getFirstInstruction();
		if (handle == null) return ""+ bb.getLabel();
		return bb.getLabel()+":"+ handle.getPosition() + " " + handle.getInstruction();
	}
	private static void debug(BasicBlock bb, String msg) {


		System.out.print("Dataflow (block " + blockId(bb) + "): " + msg);
	}

	private static void debug(BasicBlock bb, BasicBlock pred, Edge edge, String msg) {
		System.out.print("Dataflow (block " + blockId(bb) + ", predecessor " + blockId(pred) +
				" [" + Edge.edgeTypeToString(edge.getType()) + "]): " + msg);
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
	 * Get dataflow fact at (just before) given Location.
	 * Note "before" is meant in the logical sense, so for backward analyses,
	 * before means after the location in the control flow sense.
	 * 
	 * @param location the Location
	 * @return the dataflow value at given Location
	 * @throws DataflowAnalysisException
	 */
	public /*final*/ Fact getFactAtLocation(Location location) throws DataflowAnalysisException {
		return analysis.getFactAtLocation(location);
	}
	
	/**
	 * Get the dataflow fact representing the point just after given Location.
	 * Note "after" is meant in the logical sense, so for backward analyses,
	 * after means before the location in the control flow sense.
	 * 
	 * @param location the Location
	 * @return the dataflow value after given Location
	 * @throws DataflowAnalysisException
	 */
	public /*final*/ Fact getFactAfterLocation(Location location) throws DataflowAnalysisException {
		return analysis.getFactAfterLocation(location);
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
	 * Return an Iterator over edges that connect given block to its
	 * logical predecessors.  For forward analyses, this is the incoming edges.
	 * For backward analyses, this is the outgoing edges.
	 */
	private Iterator<Edge> logicalPredecessorEdgeIterator(BasicBlock block) {
		return isForwards ? cfg.incomingEdgeIterator(block) : cfg.outgoingEdgeIterator(block);
	}

	/**
	 * Get the "logical" entry block of the CFG.
	 * For forward analyses, this is the entry block.
	 * For backward analyses, this is the exit block.
	 */
	private BasicBlock logicalEntryBlock() {
		return isForwards ? cfg.getEntry() : cfg.getExit();
	}
}

// vim:ts=4
