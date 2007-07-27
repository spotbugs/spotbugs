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

import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.TreeSet;

import org.apache.bcel.generic.ATHROW;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.graph.AbstractGraph;
import edu.umd.cs.findbugs.util.NullIterator;

/**
 * Simple control flow graph abstraction for BCEL.
 *
 * @see BasicBlock
 * @see Edge
 */
public class CFG extends AbstractGraph<Edge, BasicBlock> implements Debug {
	/* ----------------------------------------------------------------------
	 * CFG flags
	 * ---------------------------------------------------------------------- */
	
	/** Flag set if infeasible exception edges have been pruned from the CFG. */
	public static final int PRUNED_INFEASIBLE_EXCEPTIONS = 1;
	
	/** Flag set if normal return edges from calls to methods which unconditionally
	 *  throw an exception have been removed. */
	public static final int PRUNED_UNCONDITIONAL_THROWERS = 2;
	
	/** Flag set if CFG has been "refined"; i.e., to the extent possible,
	 *  all infeasible edges have been removed. */
	public static final int REFINED = 4;
	
	/** Flag set if CFG edges corresponding to failed assertions have been removed. */
	public static final int PRUNED_FAILED_ASSERTION_EDGES = 8;
	
	/** Flag set if CFG is busy (in the process of being refined. */
	public static final int BUSY = 16;

	/* ----------------------------------------------------------------------
	 * Helper classes
	 * ---------------------------------------------------------------------- */

	/**
	 * An Iterator over the Locations in the CFG.
	 * Because of JSR subroutines, the same instruction may actually
	 * be part of multiple basic blocks (with different facts
	 * true in each, due to calling context).  Locations specify
	 * both the instruction and the basic block.
	 */
	private class LocationIterator implements Iterator<Location> {
		private Iterator<BasicBlock> blockIter;
		private BasicBlock curBlock;
		private Iterator<InstructionHandle> instructionIter;
		private Location next;

		private LocationIterator() {
			this.blockIter = blockIterator();
			findNext();
		}

		public boolean hasNext() {
			findNext();
			return next != null;
		}

		public Location next() {
			findNext();
			if (next == null) throw new NoSuchElementException();
			Location result = next;
			next = null;
			return result;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		private void findNext() {
			while (next == null) {
				// Make sure we have an instruction iterator
				if (instructionIter == null) {
					if (!blockIter.hasNext())
						return; // At end
					curBlock = blockIter.next();
					instructionIter = curBlock.instructionIterator();
				}

				if (instructionIter.hasNext())
					next = new Location(instructionIter.next(), curBlock);
				else
					instructionIter = null; // Go to next block
			}
		}
	}

	/* ----------------------------------------------------------------------
	 * Fields
	 * ---------------------------------------------------------------------- */

	private BasicBlock entry, exit;
	private int flags;
	private String methodName; // for informational purposes
	private MethodGen methodGen;
	private List<Edge> removedEdgeList;

	/* ----------------------------------------------------------------------
	 * Public methods
	 * ---------------------------------------------------------------------- */

	/**
	 * Constructor.
	 * Creates empty control flow graph (with just entry and exit nodes).
	 */
	public CFG() {
	}

	/**
	 * @param methodName The methodName to set.
	 */
	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public void setMethodGen(MethodGen methodGen) {
		this.methodGen = methodGen;
	}
	public MethodGen getMethodGen() {
		return methodGen;
	}
	/**
	 * @return Returns the methodName.
	 */
	public String getMethodName() {
		return methodName;
	}

	public void setFlags(int flags) {
		this.flags = flags;
	}

	public int getFlags() {
		return flags;
	}

	public boolean isFlagSet(int flag) {
		return (flags & flag) != 0;
	}

	/**
	 * Get the entry node.
	 */
	public BasicBlock getEntry() {
		if (entry == null) {
			entry = allocate();
		}
		return entry;
	}

	/**
	 * Get the exit node.
	 */
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
	 *
	 * @param source the source basic block
	 * @param dest   the destination basic block
	 * @param type   the type of edge; see constants in EdgeTypes interface
	 * @return the newly created Edge
	 * @throws IllegalStateException if there is already an edge in the CFG
	 *                               with the same source and destination block
	 */
	public Edge createEdge(BasicBlock source, BasicBlock dest, int type) {
		Edge edge = createEdge(source, dest);
		edge.setType(type);
		return edge;
	}

	/**
	 * Look up an Edge by its id.
	 *
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
	 * Get an Iterator over the Locations in the control flow graph.
	 */
	public Iterator<Location> locationIterator() {
		return new LocationIterator();
	}
	
	/**
	 * Returns a collection of locations, ordered according to the compareTo ordering over locations.
	 * If you want to list all the locations in a CFG for debugging purposes, this is a good order to do so in.
	 * 
	 * @return collection of locations
	 */
	public Collection<Location> orderedLocations() {
		TreeSet<Location> tree = new TreeSet<Location>();
		for(Iterator<Location> locs = locationIterator(); locs.hasNext(); ) {
			Location loc = locs.next();
			tree.add(loc);
		}
		return tree;
	}

	/**
	 * Get Collection of basic blocks whose IDs are specified by
	 * given BitSet.
	 *
	 * @param labelSet BitSet of block labels
	 * @return a Collection containing the blocks whose IDs are given
	 */
	public Collection<BasicBlock> getBlocks(BitSet labelSet) {
		LinkedList<BasicBlock> result = new LinkedList<BasicBlock>();
		for (Iterator<BasicBlock> i = blockIterator(); i.hasNext();) {
			BasicBlock block = i.next();
			if (labelSet.get(block.getLabel()))
				result.add(block);
		}
		return result;
	}

	/**
	 * Get a Collection of basic blocks which contain the bytecode
	 * instruction with given offset.
	 *
	 * @param offset the bytecode offset of an instruction
	 * @return Collection of BasicBlock objects which contain the instruction
	 *         with that offset
	 */
	public Collection<BasicBlock> getBlocksContainingInstructionWithOffset(int offset) {
		LinkedList<BasicBlock> result = new LinkedList<BasicBlock>();
		for (Iterator<BasicBlock> i = blockIterator(); i.hasNext(); ) {
			BasicBlock block = i.next();
			if (block.containsInstructionWithOffset(offset))
				result.add(block);
		}
		return result;
	}

	/**
	 * Get a Collection of Locations which specify the instruction
	 * at given bytecode offset.
	 * 
	 * @param offset the bytecode offset
	 * @return all Locations referring to the instruction at that offset
	 */
	public Collection<Location> getLocationsContainingInstructionWithOffset(int offset) {
		LinkedList<Location> result = new LinkedList<Location>();
		for (Iterator<Location> i = locationIterator(); i.hasNext(); ) {
			Location location = i.next();
			if (location.getHandle().getPosition() == offset) {
				result.add(location);
			}
		}
		return result;
	}

	/**
	 * Get the first predecessor reachable from given edge type.
	 *
	 * @param target   the target block
	 * @param edgeType the edge type leading from the predecessor
	 * @return the predecessor, or null if there is no incoming edge with
	 *         the specified edge type
	 */
	public BasicBlock getPredecessorWithEdgeType(BasicBlock target, int edgeType) {
		Edge edge = getIncomingEdgeWithType(target, edgeType);
		return edge != null ? edge.getSource() : null;
	}

	/**
	 * Get the first successor reachable from given edge type.
	 *
	 * @param source   the source block
	 * @param edgeType the edge type leading to the successor
	 * @return the successor, or null if there is no outgoing edge with
	 *         the specified edge type
	 */
	public BasicBlock getSuccessorWithEdgeType(BasicBlock source, int edgeType) {
		Edge edge = getOutgoingEdgeWithType(source, edgeType);
		return edge != null ? edge.getTarget() : null;
	}

	/**
	 * Get the Location where exception(s) thrown on given exception edge
	 * are thrown.
	 * 
	 * @param exceptionEdge the exception Edge
	 * @return Location where exception(s) are thrown from
	 */
	public Location getExceptionThrowerLocation(Edge exceptionEdge) {
		if (!exceptionEdge.isExceptionEdge())
			throw new IllegalArgumentException();

		InstructionHandle handle = exceptionEdge.getSource().getExceptionThrower();
		if (handle == null)
			throw new IllegalStateException();

		BasicBlock basicBlock = (handle.getInstruction() instanceof ATHROW)
			? exceptionEdge.getSource()
			: getSuccessorWithEdgeType(exceptionEdge.getSource(), EdgeTypes.FALL_THROUGH_EDGE);

		if (basicBlock == null) {
			if (removedEdgeList != null) {
				// The fall-through edge might have been removed during
				// CFG pruning.  Look for it in the removed edge list.
				for (Edge removedEdge : removedEdgeList) {
					if (removedEdge.getType() == EdgeTypes.FALL_THROUGH_EDGE
							&& removedEdge.getSource() == exceptionEdge.getSource()) {
						basicBlock = removedEdge.getTarget();
						break;
					}
				}
			}
		}
			
		if (basicBlock == null) {
			throw new IllegalStateException("No basic block for thrower " + handle + " in " + this.methodGen.getClassName() + "." + 
					 this.methodName + " : " + this.methodGen.getSignature());
		}

		return new Location(handle, basicBlock);
	}
	
	/**
	 * Get an Iterator over Edges removed from this CFG.
	 * 
	 * @return Iterator over Edges removed from this CFG
	 */
	public Iterator<Edge> removedEdgeIterator() {
		return removedEdgeList != null ? removedEdgeList.iterator() : new NullIterator<Edge>();
	}

	/**
	 * Get the first incoming edge in basic block with given type.
	 *
	 * @param basicBlock the basic block
	 * @param edgeType   the edge type
	 * @return the Edge, or null if there is no edge with that edge type
	 */
	public Edge getIncomingEdgeWithType(BasicBlock basicBlock, int edgeType) {
		return getEdgeWithType(incomingEdgeIterator(basicBlock), edgeType);
	}

	/**
	 * Get the first outgoing edge in basic block with given type.
	 *
	 * @param basicBlock the basic block
	 * @param edgeType   the edge type
	 * @return the Edge, or null if there is no edge with that edge type
	 */
	public Edge getOutgoingEdgeWithType(BasicBlock basicBlock, int edgeType) {
		return getEdgeWithType(outgoingEdgeIterator(basicBlock), edgeType);
	}

	private Edge getEdgeWithType(Iterator<Edge> iter, int edgeType) {
		while (iter.hasNext()) {
			Edge edge = iter.next();
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
		BasicBlock b = new BasicBlock();
		addVertex(b);
		return b;
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
		for (Iterator<BasicBlock> i = blockIterator(); i.hasNext();) {
			BasicBlock basicBlock = i.next();
			InstructionHandle prev = null;
			for (Iterator<InstructionHandle> j = basicBlock.instructionIterator(); j.hasNext();) {
				InstructionHandle handle = j.next();
				if (prev != null && prev.getNext() != handle)
					throw new IllegalStateException("Non-consecutive instructions in block " + basicBlock.getLabel() +
							": prev=" + prev + ", handle=" + handle);
				prev = handle;
			}
		}
	}

	@Override
	protected Edge allocateEdge(BasicBlock source, BasicBlock target) {
		return new Edge(source, target);
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.graph.AbstractGraph#removeEdge(edu.umd.cs.findbugs.graph.AbstractEdge)
	 */
	@Override
	public void removeEdge(Edge edge) {
		super.removeEdge(edge);
		
		// Keep track of removed edges.
		if (removedEdgeList == null) {
			removedEdgeList = new LinkedList<Edge>();
		}
		removedEdgeList.add(edge);
	}

	/**
	 * Get number of non-exception control successors of given basic block.
	 * 
	 * @param block a BasicBlock
	 * @return number of non-exception control successors of the basic block
	 */
	public int getNumNonExceptionSucessors(BasicBlock block) {
		int numNonExceptionSuccessors = block.getNumNonExceptionSuccessors();
		if (numNonExceptionSuccessors < 0) {
			numNonExceptionSuccessors = 0;
			for (Iterator<Edge> i = outgoingEdgeIterator(block); i.hasNext();) {
				Edge edge = i.next();
				if (!edge.isExceptionEdge()) {
					numNonExceptionSuccessors++;
				}
			}
			block.setNumNonExceptionSuccessors(numNonExceptionSuccessors);
		}
		return numNonExceptionSuccessors;
	}

	/**
	 * Get the Location representing the entry to the CFG.
	 * Note that this is a "fake" Location, and shouldn't
	 * be relied on to yield source line information.
	 * 
	 * @return Location at entry to CFG
	 */
	public Location getLocationAtEntry() {
		InstructionHandle handle = getEntry().getFirstInstruction();
		assert handle != null;
		return new Location(handle, getEntry());
	}
}

// vim:ts=4
