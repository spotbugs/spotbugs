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
public class CFG {
    private int nextId = 0;
    private int getNextId() {
	return nextId++;
    }
    private int firstEdgeId, maxEdgeId;
    private BasicBlock entry, exit;

    private TreeSet<BasicBlock> nodeSet; // node == BasicBlock

    // Map of Edges to themselves (so we can use temporary Edge objects as keys).
    // These are ordered by (source,dest,dupCount).
    private TreeMap<Edge, Edge> outgoingEdgeMap;

    // Comparator of edges by (dest,source,dupCount).
    private static class IncomingEdgeComparator implements Comparator<Edge> {
	public int compare(Edge a, Edge b) {
	    int cmp;
	    cmp = a.getDest().compareTo(b.getDest());
	    if (cmp != 0)
		return cmp;
	    cmp = a.getSource().compareTo(b.getSource());
	    if (cmp != 0)
		return cmp;
	    return a.getDupCount() - b.getDupCount();
	}
    }
    private static IncomingEdgeComparator incomingEdgeComparator = new IncomingEdgeComparator();

    // Map of Edges ordered by (dest,source,dupCount).
    // This map allows us to quickly find the incoming edges to a particular block.
    private TreeMap<Edge, Edge> incomingEdgeMap;

    /**
     * Constructor.
     * Creates empty control flow graph (with just entry and exit nodes).
     */
    public CFG() {
	nodeSet = new TreeSet<BasicBlock>();
	outgoingEdgeMap = new TreeMap<Edge, Edge>();
	incomingEdgeMap = new TreeMap<Edge, Edge>(incomingEdgeComparator);
	entry = allocate();
	exit = allocate();
    }

    /**
     * Make a deep copy of this CFG.
     * Blocks and edges are copied, and their ids are set to be identical.
     * The underlying InstructionHandles
     * are <em>not</em> copied, as this would make the resulting
     * duplicate CFG useless for instrumentation purposes.
     * (I.e., the same InstructionHandle references are shared by
     * the original and duplicate CFG.)
     *
     * @return a deep copy of the CFG
     */
    public CFG duplicate() {
	IdentityHashMap<BasicBlock, BasicBlock> blockMap = new IdentityHashMap<BasicBlock, BasicBlock>();

	CFG dup = new CFG();
	blockMap.put(this.getEntry(), dup.getEntry());
	blockMap.put(this.getExit(), dup.getExit());

	// Add duplicate basic blocks
	Iterator<BasicBlock> bIter = this.blockIterator();
	while (bIter.hasNext()) {
	    BasicBlock origBlock = bIter.next();
	    if (origBlock.getId() <= 1) // Skip entry and exit
		continue;
	    BasicBlock dupBlock = dup.allocate();
	    int origId = origBlock.getId();
	    int dupId = dupBlock.getId();
	    //System.out.println("origId="+origId+", dupId="+dupId);
	    if (origBlock.getId() != dupBlock.getId()) throw new IllegalStateException();
	    Iterator<InstructionHandle> i = origBlock.instructionIterator();
	    while (i.hasNext()) {
		dupBlock.addInstruction(i.next());
	    }
	    dupBlock.setExceptionGen(origBlock.getExceptionGen());
	    blockMap.put(origBlock, dupBlock);
	}

	// Add duplicate edges
	Iterator<Edge> eIter = this.edgeIterator();
	while (eIter.hasNext()) {
	    Edge origEdge = eIter.next();
	    BasicBlock origSrc = origEdge.getSource();
	    BasicBlock origDest = origEdge.getDest();
	    BasicBlock dupSrc = blockMap.get(origSrc);
	    BasicBlock dupDest = blockMap.get(origDest);
	    Edge dupEdge = dup.addEdge(dupSrc, dupDest, origEdge.getType());
	    dupEdge.setId(origEdge.getId());

	    // Annotate with source/destination bytecode and source line.
	    int sourceBytecode = origEdge.getSourceBytecode();
	    int destBytecode = origEdge.getDestinationBytecode();
	    int sourceLine = origEdge.getSourceLine();
	    int destLine = origEdge.getDestinationLine();
	    dupEdge.setSourceAndDest(sourceBytecode, destBytecode, sourceLine, destLine);
	}

	dup.maxEdgeId = this.maxEdgeId;

	return dup;
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
	Iterator<Edge> i = outgoingEdgeMap.values().iterator();
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

    /**
     * Set the value 1 greater than the maximum edge id. This should be called if,
     * after edge ids are assigned, edges are added or removed in such a way
     * that the max edge id is no longer correct.
     * This method will scan all edges, and update the max edge id appropriately.
     */
    public void setMaxEdgeId() {
	int max = -1;
	Iterator<Edge> i = edgeIterator();
	while (i.hasNext()) {
	    Edge edge = i.next();
	    int edgeId = edge.getId();
	    if (edgeId > max)
		max = edgeId;
	}
	maxEdgeId = max + 1;
    }

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
	if (!nodeSet.contains(source))
	    throw new IllegalArgumentException("source is not in the CFG");
	if (!nodeSet.contains(dest))
	    throw new IllegalArgumentException("dest is not in the CFG");

	Edge edge = new Edge(source, dest, type);
	if (outgoingEdgeMap.get(edge) != null) {
	    System.out.println("Old: " + outgoingEdgeMap.get(edge));
	    System.out.println("New: " + edge);
	    System.out.println("Source block:");
	    dumpBlock(edge.getSource());
	    System.out.println("Dest block:");
	    dumpBlock(edge.getDest());
	    throw new IllegalStateException();
	}
	storeEdge(edge);
	return edge;
    }

    private static void dumpBlock(BasicBlock block) {
	Iterator<InstructionHandle> i = block.instructionIterator();
	while (i.hasNext()) {
	    System.out.println(i.next());
	}
    }

    /**
     * Add a (potentially) duplicate edge to the CFG.
     * @param source the source basic block
     * @param dest the destination basic block
     * @param type the edge type
     * @return the newly created Edge
     */
    public Edge addDuplicateEdge(BasicBlock source, BasicBlock dest, int type) {
	SortedMap<Edge, Edge> dupEdgeMap = getDuplicateEdgeMap(source, dest);
	int nextDupCount = dupEdgeMap.size();
	Edge edge = new Edge(source, dest, type, nextDupCount);
	storeEdge(edge);
	return edge;
    }

    /**
     * Store given edge in edge maps.
     */
    private void storeEdge(Edge edge) {
	outgoingEdgeMap.put(edge, edge);
	incomingEdgeMap.put(edge, edge);

	// Update incoming and outgoing edge counts
	BasicBlock source = edge.getSource();
	BasicBlock dest = edge.getDest();
	source.setNumOutgoingEdges(source.getNumOutgoingEdges() + 1);
	dest.setNumIncomingEdges(dest.getNumIncomingEdges() + 1);
    }

    /**
     * Remove an edge from the graph.
     * @param edge the Edge to remove from the graph
     */
    public void removeEdge(Edge edge) {
	if (!outgoingEdgeMap.containsKey(edge))
	    throw new IllegalArgumentException("edge is not part of this CFG");
	outgoingEdgeMap.remove(edge);
	incomingEdgeMap.remove(edge);

	// Update incoming and outgoing edge counts
	BasicBlock source = edge.getSource();
	BasicBlock dest = edge.getDest();
	source.setNumOutgoingEdges(source.getNumOutgoingEdges() - 1);
	dest.setNumIncomingEdges(dest.getNumIncomingEdges() - 1);
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
	Edge key = new Edge(source, dest, 0);
	return outgoingEdgeMap.get(key);
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
     * Look up duplicate edges matching given source and destination blocks.
     * @param source the source BasicBlock
     * @param dest the destination BasicBlock
     * @return an Iterator over all Edges which match the source and destination
     */
    public Iterator<Edge> lookupDuplicateEdges(BasicBlock source, BasicBlock dest) {
	return getDuplicateEdgeMap(source, dest).values().iterator();
    }

    /**
     * Get an Iterator over the nodes (BasicBlocks) of the control flow graph.
     */
    public Iterator<BasicBlock> blockIterator() {
	return nodeSet.iterator();
    }

    /**
     * Get an Iterator over the edges in the control flow graph.
     */
    public Iterator<Edge> edgeIterator() {
	return outgoingEdgeMap.values().iterator();
    }

    /**
     * Iterator for finding a range of edge with the same source or destination
     * block in a submap.  Because the incomingEdgeMap and outgoingEdgeMap group
     * edges by destination and source blocks (respectively), a submap can
     * be used to iterate through all of the edges with the same destination
     * or source block.
     */
    private static abstract class EdgeIterator implements Iterator<Edge> {
	private Iterator<Edge> realIter;
	private Edge next;

	public EdgeIterator(Iterator<Edge> realIter) {
	    this.realIter = realIter;
	}

	public boolean hasNext() {
	    if (next != null)
		return true;
	    else if (!realIter.hasNext())
		return false;
	    Edge candidate = realIter.next();
	    if (!isMatch(candidate))
		return false;
	    next = candidate;
	    return true;
	}

	public Edge next() {
	    if (next == null) throw new NoSuchElementException();
	    Edge result = next;
	    next = null;
	    return result;
	}

	public void remove() {
	    realIter.remove();
	}

	protected abstract boolean isMatch(Edge edge);
    }

    /**
     * Iterator for a sequence of edges with the same source block.
     */
    private static class OutgoingEdgeIterator extends EdgeIterator {
	private BasicBlock sourceBlock;

	public OutgoingEdgeIterator(Iterator<Edge> realIter, BasicBlock sourceBlock) {
	    super(realIter);
	    this.sourceBlock = sourceBlock;
	}

	protected boolean isMatch(Edge edge) {
	    return edge.getSource() == sourceBlock;
	}
    }

    /**
     * Iterator for a sequence of edges with the same destination block.
     */
    private static class IncomingEdgeIterator extends EdgeIterator {
	private BasicBlock destBlock;

	public IncomingEdgeIterator(Iterator<Edge> realIter, BasicBlock destBlock) {
	    super(realIter);
	    this.destBlock = destBlock;
	}

	protected boolean isMatch(Edge edge) {
	    return edge.getDest() == destBlock;
	}
    }

    /**
     * Get an Iterator over the outgoing edges from given basic block.
     * @param source the source basic block
     */
    public Iterator<Edge> outgoingEdgeIterator(BasicBlock source) {
	BasicBlock fakeDest = new BasicBlock(-1);
	Edge fakeEdge = new Edge(source, fakeDest, 0, -1);
	SortedMap<Edge, Edge> tailMap = outgoingEdgeMap.tailMap(fakeEdge);
	return new OutgoingEdgeIterator(tailMap.values().iterator(), source);
    }

    /**
     * Get an Iterator over the incoming edges to given destination block.
     * @param dest the destination basic block
     */
    public Iterator<Edge> incomingEdgeIterator(BasicBlock dest) {
	BasicBlock fakeSource = new BasicBlock(-1);
	Edge fakeEdge = new Edge(fakeSource, dest, 0, -1);
	SortedMap<Edge, Edge> tailMap = incomingEdgeMap.tailMap(fakeEdge);
	return new IncomingEdgeIterator(tailMap.values().iterator(), dest);
    }

    /**
     * Get Iterator over successors of given basic block.
     */
    public Iterator<BasicBlock> successorIterator(BasicBlock block) {
	TreeSet<BasicBlock> successorSet = new TreeSet<BasicBlock>();
	Iterator<Edge> ei = outgoingEdgeIterator(block);
	while (ei.hasNext()) {
	    successorSet.add(ei.next().getDest());
	}
	return successorSet.iterator();
    }

    /**
     * Get Iterator over predecessors of given basic block.
     */
    public Iterator<BasicBlock> predecessorIterator(BasicBlock block) {
	TreeSet<BasicBlock> predecessorSet = new TreeSet<BasicBlock>();
	Iterator<Edge> ei = incomingEdgeIterator(block);
	while (ei.hasNext()) {
	    predecessorSet.add(ei.next().getSource());
	}
	return predecessorSet.iterator();
    }

    /**
     * Allocate a new BasicBlock.  The block won't be connected to
     * any node in the graph.
     */
    public BasicBlock allocate() {
	BasicBlock bb = new BasicBlock(getNextId());
	nodeSet.add(bb);
	return bb;
    }

    /** Get number of basic blocks. */
    public int getNumBasicBlocks() {
	return nodeSet.size();
    }

    /** Get number of edges. */
    public int getNumEdges() {
	return outgoingEdgeMap.size();
    }

    /**
     * Get the set of duplicate edges with the same source and destination blocks.
     * This relies on the fact that the edges in the edge map are sorted
     * by (source,dest) pair and then dupCount as a tie breaker;
     * therefore, all duplicate edges are adjacent in the sort order,
     * so we can use a submap to get them all.
     * @param source the source block
     * @param dest the destination block
     * @return map containing all matching edges (mapped to self)
     */
    private SortedMap<Edge, Edge> getDuplicateEdgeMap(BasicBlock source, BasicBlock dest) {
	Edge minKey = new Edge(source, dest, 0), maxKey = new Edge(source, dest, 0, Integer.MAX_VALUE);
	return outgoingEdgeMap.subMap(minKey, maxKey);
    }

    /**
     * Perform integrity checks on the CFG.
     * One of the main ideas here is that we verify that the results of
     * incoming and outgoing edge iterators are complete,
     * since the implementation of those iterators is somewhat complicated.
     * @throws IllegalStateException if the CFG is not valid
     */
    public void checkIntegrity() {
	TreeSet<Edge> allIncomingEdges = new TreeSet<Edge>(), allOutgoingEdges = new TreeSet<Edge>();

	Iterator<BasicBlock> i = nodeSet.iterator();
	while (i.hasNext()) {
	    BasicBlock block = i.next();

	    for (Iterator<Edge> j = incomingEdgeIterator(block); j.hasNext(); ) {
		Edge incoming = j.next();
		allIncomingEdges.add(incoming);
		if (incoming.getDest() != block) throw new IllegalStateException("wrong target for incoming edge");
	    }

	    for (Iterator<Edge> j = outgoingEdgeIterator(block); j.hasNext(); ) {
		Edge outgoing = j.next();
		allOutgoingEdges.add(outgoing);
		if (outgoing.getSource() != block) throw new IllegalStateException("wrong source for outgoing edge");
	    }
	}

	if (!allIncomingEdges.equals(incomingEdgeMap.keySet()))
	    throw new IllegalStateException("allIncomingEdges != incomingEdgeMap.keySet()");
	if (!allOutgoingEdges.equals(incomingEdgeMap.keySet()))
	    throw new IllegalStateException("allOutgoingEdges != incomingEdgeMap.keySet()");

	if (!allIncomingEdges.equals(outgoingEdgeMap.keySet()))
	    throw new IllegalStateException("allIncomingEdges != outgoingEdgeMap.keySet()");
	if (!allOutgoingEdges.equals(outgoingEdgeMap.keySet()))
	    throw new IllegalStateException("allOutgoingEdges != outgoingEdgeMap.keySet()");

/*
	// Verify that maxEdgeId is accurate
	int max = -1;
	for (Iterator<Edge> j = edgeIterator(); j.hasNext(); ) {
	    Edge edge = j.next();
	    if (edge.getId() > max)
		max = edge.getId();
	}
	if (max != maxEdgeId) throw new IllegalStateException("maxEdgeId is wrong: set to " + maxEdgeId + ", is actually " + max);
*/
    }
}
