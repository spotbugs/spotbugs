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

import java.util.*;

/**
 * Perform a depth first search on a control flow graph.
 * Algorithm based on Cormen, et. al, <cite>Introduction to Algorithms</cite>, p. 478.
 * Currently, this class
 * <ul>
 * <li> assigns DFS edge types (see {@link DFSEdgeTypes})
 * <li> assigns discovery and finish times for each block
 * <li> produces a topological sort of the blocks,
 *      <em>if and only if the CFG is acyclic</em>
 * </ul>
 * @see CFG
 * @author David Hovemeyer
 */
public class DepthFirstSearch implements DFSEdgeTypes /*, Debug*/ {
	public static boolean DEBUG = false;

	private CFG cfg;
	private int firstEdgeId;
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
	 * @param cfg the CFG to be searched
	 * @throws IllegalArgumentException if the CFG has not had edge ids assigned yet
	 */
	public DepthFirstSearch(CFG cfg) {
		if (!cfg.edgeIdsAssigned()) throw new IllegalArgumentException("CFG has not had edge ids assigned");

		this.cfg = cfg;
		firstEdgeId = cfg.getFirstEdgeId();

		int numBlocks = cfg.getNumBasicBlocks();
		colorList = new int[numBlocks]; // initially all elements are WHITE
		discoveryTimeList = new int[numBlocks];
		finishTimeList = new int[numBlocks];

		int maxEdgeId = cfg.getMaxEdgeId();
		dfsEdgeTypeList = new int[maxEdgeId];

		timestamp = 0;

		topologicalSortList = new LinkedList<BasicBlock>();
	}

	/**
	 * Perform the depth first search.
	 * @return this object
	 */
	public DepthFirstSearch search() {
		// Control flow graphs have a unique entry block
		// from which all other blocks can be reached,
		// so there's no need to iterate over start blocks
		// (as is needed in more general graph traversals).
		visit(cfg.getEntry());

		// Classify CROSS and FORWARD edges
		classifyUnknownEdges();

		return this;
	}

	/**
	 * Get the type of edge in the depth first search.
	 * @param edge the edge
	 * @return the DFS type of edge: TREE_EDGE, FORWARD_EDGE, CROSS_EDGE, or BACK_EDGE
	 * @see DFSEdgeTypes
	 */
	public int getDFSEdgeType(Edge edge) {
		return dfsEdgeTypeList[edgeIndex(edge)];
	}

	/**
	 * Return the timestamp indicating when the given basic block
	 * was discovered.
	 * @param block the basic block
	 */
	public int getDiscoveryTime(BasicBlock block) {
		return discoveryTimeList[blockIndex(block)];
	}

	/**
	 * Return the timestamp indicating when the given basic block
	 * was finished (meaning that all of its descendents were visited recursively).
	 * @param block the basic block
	 */
	public int getFinishTime(BasicBlock block) {
		return finishTimeList[blockIndex(block)];
	}

	/**
	 * Get an iterator over the basic blocks in topological sort order.
	 * <em>This works if and only if the CFG is acyclic.</em>
	 */
	public Iterator<BasicBlock> topologicalSortIterator() {
		return topologicalSortList.iterator();
	}

	private int blockIndex(BasicBlock block) {
		return block.getId();
	}

	private int indentLevel = 0;

	private void visit(BasicBlock block) {
		assert getColor(block) == WHITE;

		String indent = "";
		if (DEBUG) {
			for (int i = 0; i < indentLevel; ++i)
				indent = indent + "  ";
		}

		if (DEBUG) System.out.println(indent + "Visiting block " + block.getId());

		// Mark the block as visited, and set its timestamp
		setColor(block, GRAY);
		setDiscoveryTime(block, timestamp++);

		// For each successor of the block...
		Iterator<BasicBlock> succIter = cfg.successorIterator(block);
		while (succIter.hasNext()) {
			// Get the successor
			BasicBlock succ = succIter.next();
			int succColor = getColor(succ);

			// Get the Edge connecting the block to the successor
			Edge edge = cfg.lookupEdge(block, succ);
			assert edge != null;
			assert edge.getSource() == block;
			assert edge.getDest() == succ;

			// Classify edge type (if possible)
			int dfsEdgeType = 0;
			if (DEBUG) System.out.print(indent + "successor " + succ.getId());
			switch (succColor) {
			case WHITE:
				if (DEBUG) System.out.println(" is WHITE");
				dfsEdgeType = TREE_EDGE;
				break;
			case GRAY:
				if (DEBUG) System.out.println(" is GRAY");
				dfsEdgeType = BACK_EDGE;
				break;
			case BLACK:
				if (DEBUG) System.out.println(" is BLACK");
				dfsEdgeType = UNKNOWN_EDGE;
				break;// We can't distinguish between CROSS and FORWARD edges at this point
			default:
				assert false;
			}
			setDFSEdgeType(edge, dfsEdgeType);

			// If successor hasn't been visited yet, visit it
			if (succColor == WHITE) {
				if (DEBUG) ++indentLevel;
				visit(succ);
				if (DEBUG) --indentLevel;
			}
		}

		if (DEBUG) System.out.println(indent + "Finish block " + block.getId());
		setColor(block, BLACK);
		topologicalSortList.addFirst(block);
		setFinishTime(block, timestamp++);
	}

	private void classifyUnknownEdges() {
		Iterator<Edge> edgeIter = cfg.edgeIterator();
		while (edgeIter.hasNext()) {
			Edge edge = edgeIter.next();
			int dfsEdgeType = getDFSEdgeType(edge);
			if (dfsEdgeType == UNKNOWN_EDGE) {
				int srcDiscoveryTime = getDiscoveryTime(edge.getSource());
				int destDiscoveryTime = getDiscoveryTime(edge.getDest());

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
		discoveryTimeList[blockIndex(block)] = ts;
	}

	private void setFinishTime(BasicBlock block, int ts) {
		finishTimeList[blockIndex(block)] = ts;
	}

	private int edgeIndex(Edge edge) {
		assert edge.getId() >= 0;
		return edge.getId() - firstEdgeId;
	}

	private void setDFSEdgeType(Edge edge, int dfsEdgeType) {
		dfsEdgeTypeList[edgeIndex(edge)] = dfsEdgeType;
	}
}

// vim:ts=4
