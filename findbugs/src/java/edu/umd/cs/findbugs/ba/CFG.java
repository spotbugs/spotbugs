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

import edu.umd.cs.daveho.graph.AbstractGraph;

import java.util.*;

import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * Simple control flow graph abstraction for BCEL.
 * @see BasicBlock
 * @see Edge
 */
public class CFG extends AbstractGraph<Edge, BasicBlock> implements Debug {

	/* ----------------------------------------------------------------------
	 * Fields
	 * ---------------------------------------------------------------------- */

	private BasicBlock entry, exit;
	private int flags;

	/* ----------------------------------------------------------------------
	 * Public methods
	 * ---------------------------------------------------------------------- */

	/**
	 * Constructor.
	 * Creates empty control flow graph (with just entry and exit nodes).
	 */
	public CFG() {
	}

	void setFlags(int flags) {
		this.flags = flags;
	}

	int getFlags() {
		return flags;
	}

	boolean isFlagSet(int flag) {
		return (flags & flag) != 0;
	}

	/** Get the entry node. */
	public BasicBlock getEntry() {
		if (entry == null) {
			entry = allocate();
		}
		return entry;
	}

	/** Get the exit node. */
	public BasicBlock getExit() {
		if (exit == null) {
			exit = allocate();
		}
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
		Edge edge = addEdge(source, dest);
		edge.setType(type);
		return edge;
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
		return vertexIterator();
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
		return edge != null ? edge.getTarget() : null;
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
	 * Allocate a new BasicBlock.  The block won't be connected to
	 * any node in the graph.
	 */
	public BasicBlock allocate() {
		return addVertex();
	}

	/**
	 * Get number of basic blocks.
	 * This is just here for compatibility with the old CFG
	 * method names.
	 */
	public int getNumBasicBlocks() {
		return getNumVertices();
	}

	/**
	 * Get the number of edge labels allocated.
	 * This is just here for compatibility with the old CFG
	 * method names.
	 */
	public int getMaxEdgeId() {
		return getNumEdgeLabels();
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

	protected BasicBlock createVertex() {
		return new BasicBlock();
	}

	protected Edge createEdge(BasicBlock source, BasicBlock target) {
		return new Edge(source, target);
	}
}

// vim:ts=4
