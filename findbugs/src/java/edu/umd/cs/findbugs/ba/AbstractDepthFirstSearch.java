/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003,2004 University of Maryland
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

import java.util.*;

/**
 * Perform a depth first search on a control flow graph.
 * Algorithm based on Cormen, et. al, <cite>Introduction to Algorithms</cite>, p. 478.
 * Currently, this class
 * <ul>
 * <li> assigns DFS edge types (see {@link DFSEdgeTypes})
 * <li> assigns discovery and finish times for each block
 * <li> produces a topological sort of the blocks,
 * <em>if and only if the CFG is acyclic</em>
 * </ul>
 * <p/>
 * <p> Concrete subclasses implement forward and reverse versions
 * of depth first search.
 *
 * @author David Hovemeyer
 * @see CFG
 * @see DepthFirstSearch
 * @see ReverseDepthFirstSearch
 */
public abstract class AbstractDepthFirstSearch implements DFSEdgeTypes {
	public final static boolean DEBUG = false;

	private CFG cfg;
	private int[] colorList;
	private int[] discoveryTimeList;
	private int[] finishTimeList;
	private int[] dfsEdgeTypeList;
	private int timestamp;
	private LinkedList<BasicBlock> topologicalSortList;

	private static final int WHITE = 0;
	private static final int GRAY = 1;
	private static final int BLACK = 2;

	/**
	 * Constructor.
	 *
	 * @param cfg the CFG to be searched
	 * @throws IllegalArgumentException if the CFG has not had edge ids assigned yet
	 */
	public AbstractDepthFirstSearch(CFG cfg) {
		this.cfg = cfg;

		int numBlocks = cfg.getNumBasicBlocks();
		colorList = new int[numBlocks]; // initially all elements are WHITE
		discoveryTimeList = new int[numBlocks];
		finishTimeList = new int[numBlocks];

		int maxEdgeId = cfg.getMaxEdgeId();
		dfsEdgeTypeList = new int[maxEdgeId];

		timestamp = 0;

		topologicalSortList = new LinkedList<BasicBlock>();
	}

	// Abstract methods allow the concrete subclass to define
	// the "polarity" of the depth first search.  That way,
	// this code can do normal DFS, or DFS of reversed CFG.

	/**
	 * Get the "logical" entry block of the CFG.
	 */
	protected abstract BasicBlock getEntry(CFG cfg);

	/**
	 * Get Iterator over "logical" outgoing edges.
	 */
	protected abstract Iterator<Edge> outgoingEdgeIterator(CFG cfg, BasicBlock basicBlock);

	/**
	 * Get "logical" target of edge.
	 */
	protected abstract BasicBlock getTarget(Edge edge);

	/**
	 * Get "logical" source of edge.
	 */
	protected abstract BasicBlock getSource(Edge edge);

	/**
	 * Perform the depth first search.
	 *
	 * @return this object
	 */
	public AbstractDepthFirstSearch search() {
		visitAll();
		classifyUnknownEdges();

		return this;
	}

	/**
	 * Get the type of edge in the depth first search.
	 *
	 * @param edge the edge
	 * @return the DFS type of edge: TREE_EDGE, FORWARD_EDGE, CROSS_EDGE, or BACK_EDGE
	 * @see DFSEdgeTypes
	 */
	public int getDFSEdgeType(Edge edge) {
		return dfsEdgeTypeList[edge.getId()];
	}

	/**
	 * Return the timestamp indicating when the given basic block
	 * was discovered.
	 *
	 * @param block the basic block
	 */
	public int getDiscoveryTime(BasicBlock block) {
		return discoveryTimeList[block.getId()];
	}

	/**
	 * Return the timestamp indicating when the given basic block
	 * was finished (meaning that all of its descendents were visited recursively).
	 *
	 * @param block the basic block
	 */
	public int getFinishTime(BasicBlock block) {
		return finishTimeList[block.getId()];
	}

	/**
	 * Get an iterator over the basic blocks in topological sort order.
	 * <em>This works if and only if the CFG is acyclic.</em>
	 */
	public Iterator<BasicBlock> topologicalSortIterator() {
		return topologicalSortList.iterator();
	}

	private class Visit {
		private BasicBlock block;
		private Iterator<Edge> outgoingEdgeIterator;

		public Visit(BasicBlock block) {
			this.block = block;
			this.outgoingEdgeIterator = outgoingEdgeIterator(cfg, block);

			// Mark the block as visited, and set its timestamp
			setColor(block, GRAY);
			setDiscoveryTime(block, timestamp++);
		}

		public BasicBlock getBlock() {
			return block;
		}

		public boolean hasNextEdge() {
			return outgoingEdgeIterator.hasNext();
		}

		public Edge getNextEdge() {
			return outgoingEdgeIterator.next();
		}
	}

	private void visitAll() {
		ArrayList<Visit> stack = new ArrayList<Visit>(cfg.getNumBasicBlocks());
		stack.add(new Visit(getEntry(cfg)));

		while (!stack.isEmpty()) {
			Visit visit = stack.get(stack.size() - 1);

			if (visit.hasNextEdge()) {
				// Continue visiting successors
				Edge edge = visit.getNextEdge();
				visitSuccessor(stack, edge);
			} else {
				// Finish the block
				BasicBlock block = visit.getBlock();
				setColor(block, BLACK);
				topologicalSortList.addFirst(block);
				setFinishTime(block, timestamp++);

				stack.remove(stack.size() - 1);
			}
		}
	}

	private void visitSuccessor(ArrayList<Visit> stack, Edge edge) {
		// Get the successor
		BasicBlock succ = getTarget(edge);
		int succColor = getColor(succ);

		// Classify edge type (if possible)
		int dfsEdgeType = 0;
		switch (succColor) {
		case WHITE:
			dfsEdgeType = TREE_EDGE;
			break;
		case GRAY:
			dfsEdgeType = BACK_EDGE;
			break;
		case BLACK:
			dfsEdgeType = UNKNOWN_EDGE;
			break;// We can't distinguish between CROSS and FORWARD edges at this point
		default:
			assert false;
		}
		setDFSEdgeType(edge, dfsEdgeType);

		// If successor hasn't been visited yet, visit it
		if (succColor == WHITE) {
			stack.add(new Visit(succ));
		}
	}

	// Classify CROSS and FORWARD edges
	private void classifyUnknownEdges() {
		Iterator<Edge> edgeIter = cfg.edgeIterator();
		while (edgeIter.hasNext()) {
			Edge edge = edgeIter.next();
			int dfsEdgeType = getDFSEdgeType(edge);
			if (dfsEdgeType == UNKNOWN_EDGE) {
				int srcDiscoveryTime = getDiscoveryTime(getSource(edge));
				int destDiscoveryTime = getDiscoveryTime(getTarget(edge));

				if (srcDiscoveryTime < destDiscoveryTime) {
					// If the source was visited earlier than the
					// target, it's a forward edge.
					dfsEdgeType = FORWARD_EDGE;
				} else {
					// If the source was visited later than the 
					// target, it's a cross edge.
					dfsEdgeType = CROSS_EDGE;
				}

				setDFSEdgeType(edge, dfsEdgeType);
			}
		}
	}

	private void setColor(BasicBlock block, int color) {
		colorList[block.getId()] = color;
	}

	private int getColor(BasicBlock block) {
		return colorList[block.getId()];
	}

	private void setDiscoveryTime(BasicBlock block, int ts) {
		discoveryTimeList[block.getId()] = ts;
	}

	private void setFinishTime(BasicBlock block, int ts) {
		finishTimeList[block.getId()] = ts;
	}

	private void setDFSEdgeType(Edge edge, int dfsEdgeType) {
		dfsEdgeTypeList[edge.getId()] = dfsEdgeType;
	}
}

// vim:ts=4
