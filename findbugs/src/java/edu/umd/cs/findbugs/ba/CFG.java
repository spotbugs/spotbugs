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

// We require BCEL 5.0 or later.
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * Simple control flow graph abstraction for BCEL.
 * @see BasicBlock
 * @see Edge
 */
public class CFG implements Debug {
	private ArrayList<BasicBlock> blockList;
	private ArrayList<Edge> edgeList;
	private BasicBlock entry, exit;
	private int firstEdgeId, maxEdgeId;

	/**
	 * Constructor.
	 * Creates empty control flow graph (with just entry and exit nodes).
	 */
	public CFG() {
		blockList = new ArrayList<BasicBlock>();
		edgeList = new ArrayList<Edge>();
		entry = allocate();
		exit = allocate();
		firstEdgeId = maxEdgeId = 0;
	}

	/**
	 * Assign unique numbers to the edges, starting from the given number.
	 * This is done as a post-processing step by the instrumenter
	 * so that each Edge has a unique id within the overall class.
	 * @return the first id to assign
	 * @return the next unique id available to be assigned
	 */
	public int assignEdgeIds(int start) {
		firstEdgeId = start;
		Iterator<Edge> i = edgeIterator();
		while (i.hasNext()) {
			Edge edge = i.next();
			edge.setId(start++);
		}
		maxEdgeId = start;
		return start;
	}

	/** Have edge ids be assigned? */
	public boolean edgeIdsAssigned() {
		return maxEdgeId > 0;
	}

	/** Get the first edge id. Assumes assignEdgeIds() has been called. */
	public int getFirstEdgeId() { return firstEdgeId; }

	/** Get the value 1 greater than the maximum edge id. Assumes assignEdgeIds() has been called. */
	public int getMaxEdgeId() { return maxEdgeId; }

	/** Get the entry node. */
	public BasicBlock getEntry() {
		return entry;
	}

	/** Get the exit node. */
	public BasicBlock getExit() {
		return exit;
	}

	/**
	 * Add a unique edge to the graph.
	 * There must be no other edge already in the CFG with
	 * the same source and destination blocks.
	 * @param source the source basic block
	 * @param dest the destination basic block
	 * @param type  the type of edge; see constants in EdgeTypes interface
	 * @return the newly created Edge
	 * @throws IllegalStateException if there is already an edge in the CFG
	 *   with the same source and destination block
	 */
	public Edge addEdge(BasicBlock source, BasicBlock dest, int type) {
		if (VERIFY_INTEGRITY) {
			if (!blockList.contains(source))
				throw new IllegalArgumentException("source is not in the CFG");
			if (!blockList.contains(dest))
				throw new IllegalArgumentException("dest is not in the CFG");
		}

		Edge edge = new Edge(source, dest, type);

		edgeList.add(edge);
		source.addOutgoingEdge(edge);
		dest.addIncomingEdge(edge);

		return edge;
	}

	/**
	 * Remove given Edge from the graph.
	 * @param edge the edge
	 */
	public void removeEdge(Edge edge) {
		// FIXME: implement this
		throw new UnsupportedOperationException();
	}

	/**
	 * Look up the Edge object connecting given BasicBlocks.
	 * If there are multiple edges matching the given source and destination
	 * blocks, the first one is returned.
	 * @param source the source BasicBlock
	 * @param dest the destination BasicBlock
	 * @return the Edge, or null if there is no such edge in the graph
	 */
	public Edge lookupEdge(BasicBlock source, BasicBlock dest) {
		Iterator<Edge> i = outgoingEdgeIterator(source);
		while (i.hasNext()) {
			Edge edge = i.next();
			if (edge.getDest() == dest)
				return edge;
		}
		return null;
	}

	/**
	 * Look up an Edge by its id.
	 * @param id the id of the edge to look up
	 * @return the Edge, or null if no matching Edge was found
	 */
	public Edge lookupEdgeById(int id) {
		Iterator<Edge> i = edgeIterator();
		while (i.hasNext()) {
			Edge edge = i.next();
			if (edge.getId() == id)
				return edge;
		}
		return null;
	}

	/**
	 * Get an Iterator over the nodes (BasicBlocks) of the control flow graph.
	 */
	public Iterator<BasicBlock> blockIterator() {
		return blockList.iterator();
	}

	/**
	 * Get an Iterator over the edges in the control flow graph.
	 */
	public Iterator<Edge> edgeIterator() {
		return edgeList.iterator();
	}

	private static class OutgoingEdgeIterator implements Iterator<Edge> {
		private Edge edge;

		public OutgoingEdgeIterator(BasicBlock source) {
			this.edge = source.getFirstOutgoingEdge();
		}

		public boolean hasNext() { return edge != null; }

		public Edge next() {
			if (!hasNext())
				throw new NoSuchElementException();
			Edge result = edge;
			edge = edge.getNextOutgoingEdge();
			return result;
		}

		public void remove() { throw new UnsupportedOperationException(); }
	}

	/**
	 * Get an Iterator over the outgoing edges from given basic block.
	 * @param source the source basic block
	 */
	public Iterator<Edge> outgoingEdgeIterator(BasicBlock source) {
		return new OutgoingEdgeIterator(source);
	}

	private static class IncomingEdgeIterator implements Iterator<Edge> {
		private Edge edge;

		public IncomingEdgeIterator(BasicBlock dest) {
			this.edge = dest.getFirstIncomingEdge();
		}

		public boolean hasNext() { return edge != null; }

		public Edge next() {
			if (!hasNext())
				throw new NoSuchElementException();
			Edge result = edge;
			edge = edge.getNextIncomingEdge();
			return result;
		}

		public void remove() { throw new UnsupportedOperationException(); }
	}

	/**
	 * Get an Iterator over the incoming edges to given destination block.
	 * @param dest the destination basic block
	 */
	public Iterator<Edge> incomingEdgeIterator(BasicBlock dest) {
		return new IncomingEdgeIterator(dest);
	}

	/**
	 * Get Iterator over successors of given basic block.
	 */
	public Iterator<BasicBlock> successorIterator(final BasicBlock block) {
		return new Iterator<BasicBlock>() {
			private Iterator<Edge> iter = outgoingEdgeIterator(block);
			public boolean hasNext() { return iter.hasNext(); }
			public BasicBlock next() { return iter.next().getDest(); }
			public void remove() { throw new UnsupportedOperationException(); }
		};
	}

	/**
	 * Get the first successor reachable from given edge type.
	 * @param source the source block
	 * @param edgeType the edge type leading to the successor
	 * @return the successor, or null if there is no outgoing edge with
	 *   the specified edge type
	 */
	public BasicBlock getSuccessorWithEdgeType(BasicBlock source, int edgeType) {
		Edge edge = getOutgoingEdgeWithType(source, edgeType);
		return edge != null ? edge.getDest() : null;
	}

	/**
	 * Get the first outgoing edge in basic block with given type.
	 * @param basicBlock the basic block
	 * @param edgeType the edge type
	 * @return the Edge, or null if there is no edge with that edge type
	 */
	public Edge getOutgoingEdgeWithType(BasicBlock basicBlock, int edgeType) {
		Iterator<Edge> i = outgoingEdgeIterator(basicBlock);
		while (i.hasNext()) {
			Edge edge = i.next();
			if (edge.getType() == edgeType)
				return edge;
		}
		return null;
	}

	/**
	 * Get Iterator over predecessors of given basic block.
	 */
	public Iterator<BasicBlock> predecessorIterator(final BasicBlock block) {
		return new Iterator<BasicBlock>() {
			private Iterator<Edge> iter = incomingEdgeIterator(block);
			public boolean hasNext() { return iter.hasNext(); }
			public BasicBlock next() { return iter.next().getSource(); }
			public void remove() { throw new UnsupportedOperationException(); }
		};
	}

	/**
	 * Get the next block id to be assigned.
	 * This will need to change if we want to allow blocks to be removed.
	 */
	private int getNextId() {
		return blockList.size();
	}

	/**
	 * Allocate a new BasicBlock.  The block won't be connected to
	 * any node in the graph.
	 */
	public BasicBlock allocate() {
		BasicBlock bb = new BasicBlock(getNextId());
		blockList.add(bb);
		return bb;
	}

	/** Get number of basic blocks. */
	public int getNumBasicBlocks() {
		return blockList.size();
	}

	/** Get number of edges. */
	public int getNumEdges() {
		return edgeList.size();
	}

	public void checkIntegrity() {
		// Ensure that basic blocks have only consecutive instructions
		for (Iterator<BasicBlock> i = blockIterator(); i.hasNext(); ) {
			BasicBlock basicBlock = i.next();
			InstructionHandle prev = null;
			for (Iterator<InstructionHandle> j = basicBlock.instructionIterator(); j.hasNext(); ) {
				InstructionHandle handle = j.next();
				if (prev != null && prev.getNext() != handle)
					throw new IllegalStateException("Non-consecutive instructions in block " + basicBlock.getId() +
						": prev=" + prev + ", handle=" + handle);
				prev = handle;
			}
		}
	}
}

// vim:ts=4
