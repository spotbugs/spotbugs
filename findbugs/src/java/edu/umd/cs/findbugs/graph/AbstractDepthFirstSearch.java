/*
 * Generic graph library
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

package edu.umd.cs.findbugs.graph;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;

/**
 * Perform a depth first search on a graph.
 * Algorithm based on Cormen, et. al, <cite>Introduction to Algorithms</cite>, p. 478.
 * Currently, this class
 * <ul>
 * <li> assigns DFS edge types (see {@link DFSEdgeTypes})
 * <li> assigns discovery and finish times for each vertex
 * <li> produces a topological sort of the vertices,
 * <em>if and only if the graph is acyclic</em>
 * </ul>
 *
 * <p> Concrete subclasses implement forward and reverse versions
 * of depth first search.
 *
 * @author David Hovemeyer
 * @see Graph
 * @see DepthFirstSearch
 * @see ReverseDepthFirstSearch
 */
public abstract class AbstractDepthFirstSearch
		<
		GraphType extends Graph<EdgeType, VertexType>,
		EdgeType extends GraphEdge<EdgeType, VertexType>,
		VertexType extends GraphVertex<VertexType>
		>
	implements DFSEdgeTypes {

	public final static boolean DEBUG = false;

	private GraphType graph;
	private int[] colorList;
	private int[] discoveryTimeList;
	private int[] finishTimeList;
	private int[] dfsEdgeTypeList;
	private int timestamp;
	private LinkedList<VertexType> topologicalSortList;
	private VertexChooser<VertexType> vertexChooser;
	private SearchTreeCallback<VertexType> searchTreeCallback;

	/**
	 * Color of a vertex which hasn't been visited yet.
	 */
	protected static final int WHITE = 0;

	/**
	 * Color of a vertex which has been visited, but
	 * not all of whose descendents have been visited.
	 */
	protected static final int GRAY = 1;

	/**
	 * Color of a vertex whose entire search tree has
	 * been visited.
	 */
	protected static final int BLACK = 2;

	/**
	 * Constructor.
	 *
	 * @param graph the graph to be searched
	 * @throws IllegalArgumentException if the graph has not had edge ids assigned yet
	 */
	public AbstractDepthFirstSearch(GraphType graph) {
		this.graph = graph;

		int numBlocks = graph.getNumVertexLabels();
		colorList = new int[numBlocks]; // initially all elements are WHITE
		discoveryTimeList = new int[numBlocks];
		finishTimeList = new int[numBlocks];

		int maxEdgeId = graph.getNumEdgeLabels();
		dfsEdgeTypeList = new int[maxEdgeId];

		timestamp = 0;

		topologicalSortList = new LinkedList<VertexType>();
	}

	// Abstract methods allow the concrete subclass to define
	// the "polarity" of the depth first search.  That way,
	// this code can do normal DFS, or DFS of reversed GraphType.

	/**
	 * Get Iterator over "logical" outgoing edges.
	 */
	protected abstract Iterator<EdgeType> outgoingEdgeIterator(GraphType graph, VertexType vertex);

	/**
	 * Get "logical" target of edge.
	 */
	protected abstract VertexType getTarget(EdgeType edge);

	/**
	 * Get "logical" source of edge.
	 */
	protected abstract VertexType getSource(EdgeType edge);

	/**
	 * Choose the next search tree root.
	 * By default, this method just scans for a WHITE vertex.
	 * Subclasses may override this method in order to choose
	 * which vertices are used as search tree roots.
	 *
	 * @return the next search tree root
	 */
	protected VertexType getNextSearchTreeRoot() {
		// FIXME: slow linear search, should improve
		for (Iterator<VertexType> i = graph.vertexIterator(); i.hasNext(); ) {
			VertexType vertex = i.next();
			if (visitMe(vertex))
				return vertex;
		}
		return null;
	}

	/**
	 * Specify a VertexChooser object to be used to selected
	 * which vertices are visited by the search.
	 * This is useful if you only want to search a subset
	 * of the vertices in the graph.
	 *
	 * @param vertexChooser the VertexChooser to use
	 */
	public void setVertexChooser(VertexChooser<VertexType> vertexChooser) {
		this.vertexChooser = vertexChooser;
	}

	/**
	 * Set a search tree callback.
	 *
	 * @param searchTreeCallback the search tree callback
	 */
	public void setSearchTreeCallback(SearchTreeCallback<VertexType> searchTreeCallback) {
		this.searchTreeCallback = searchTreeCallback;
	}

	/**
	 * Perform the depth first search.
	 *
	 * @return this object
	 */
	public AbstractDepthFirstSearch<GraphType, EdgeType, VertexType> search() {
		visitAll();
		classifyUnknownEdges();

		return this;
	}

	/**
	 * Return whether or not the graph contains a cycle:
	 * i.e., whether it contains any back edges.
	 * This should only be called after search() has been called
	 * (since that method actually executes the search).
	 *
	 * @return true if the graph contains a cycle, false otherwise
	 */
	public boolean containsCycle() {
		for (Iterator<EdgeType> i = graph.edgeIterator(); i.hasNext(); ) {
			EdgeType edge = i.next();
			if (getDFSEdgeType(edge) == BACK_EDGE)
				return true;
		}
		return false;
	}

	/**
	 * Get the type of edge in the depth first search.
	 *
	 * @param edge the edge
	 * @return the DFS type of edge: TREE_EDGE, FORWARD_EDGE, CROSS_EDGE, or BACK_EDGE
	 * @see DFSEdgeTypes
	 */
	public int getDFSEdgeType(EdgeType edge) {
		return dfsEdgeTypeList[edge.getLabel()];
	}

	/**
	 * Return the timestamp indicating when the given vertex
	 * was discovered.
	 *
	 * @param vertex the vertex
	 */
	public int getDiscoveryTime(VertexType vertex) {
		return discoveryTimeList[vertex.getLabel()];
	}

	/**
	 * Return the timestamp indicating when the given vertex
	 * was finished (meaning that all of its descendents were visited recursively).
	 *
	 * @param vertex the vertex
	 */
	public int getFinishTime(VertexType vertex) {
		return finishTimeList[vertex.getLabel()];
	}

	/**
	 * Get array of finish times, indexed by vertex label.
	 *
	 * @return the array of finish times
	 */
	@SuppressWarnings("EI")
	public int[] getFinishTimeList() {
		return finishTimeList;
	}

	/**
	 * Get an iterator over the vertexs in topological sort order.
	 * <em>This works if and only if the graph is acyclic.</em>
	 */
	public Iterator<VertexType> topologicalSortIterator() {
		return topologicalSortList.iterator();
	}

	private class Visit {
		private VertexType vertex;
		private Iterator<EdgeType> outgoingEdgeIterator;

		public Visit(VertexType vertex) {
			if (vertex == null) throw new IllegalStateException();
			this.vertex = vertex;
			this.outgoingEdgeIterator = outgoingEdgeIterator(graph, vertex);

			// Mark the vertex as visited, and set its timestamp
			setColor(vertex, GRAY);
			setDiscoveryTime(vertex, timestamp++);
		}

		public VertexType getVertex() {
			return vertex;
		}

		public boolean hasNextEdge() {
			return outgoingEdgeIterator.hasNext();
		}

		public EdgeType getNextEdge() {
			return outgoingEdgeIterator.next();
		}
	}

	private void visitAll() {
		for (;;) {
			VertexType searchTreeRoot = getNextSearchTreeRoot();
			if (searchTreeRoot == null)
				// No more work to do
				break;

			if (searchTreeCallback != null)
				searchTreeCallback.startSearchTree(searchTreeRoot);

			ArrayList<Visit> stack = new ArrayList<Visit>(graph.getNumVertexLabels());
			stack.add(new Visit(searchTreeRoot));

			while (!stack.isEmpty()) {
				Visit visit = stack.get(stack.size() - 1);

				if (visit.hasNextEdge()) {
					// Continue visiting successors
					EdgeType edge = visit.getNextEdge();
					visitSuccessor(stack, edge);
				} else {
					// Finish the vertex
					VertexType vertex = visit.getVertex();
					setColor(vertex, BLACK);
					topologicalSortList.addFirst(vertex);
					setFinishTime(vertex, timestamp++);

					stack.remove(stack.size() - 1);
				}
			}
		}
	}

	private void visitSuccessor(ArrayList<Visit> stack, EdgeType edge) {
		// Get the successor
		VertexType succ = getTarget(edge);
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
		if (visitMe(succ)) {
			// Add to search tree (if a search tree callback exists)
			if (searchTreeCallback != null)
				searchTreeCallback.addToSearchTree(getSource(edge), getTarget(edge));

			// Add to visitation stack
			stack.add(new Visit(succ));
		}
	}

	// Classify CROSS and FORWARD edges
	private void classifyUnknownEdges() {
		Iterator<EdgeType> edgeIter = graph.edgeIterator();
		while (edgeIter.hasNext()) {
			EdgeType edge = edgeIter.next();
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

	private void setColor(VertexType vertex, int color) {
		colorList[vertex.getLabel()] = color;
	}

	/**
	 * Get the current color of given vertex.
	 *
	 * @param vertex the vertex
	 * @return the color (WHITE, BLACK, or GRAY)
	 */
	protected int getColor(VertexType vertex) {
		return colorList[vertex.getLabel()];
	}

	/**
	 * Predicate to determine which vertices should be visited
	 * as the search progresses.  Takes both vertex color
	 * and the vertex chooser (if any) into account.
	 *
	 * @param vertex the vertex to possibly be visited
	 * @return true if the vertex should be visited, false if not
	 */
	protected boolean visitMe(VertexType vertex) {
		return (getColor(vertex) == WHITE)
			&& (vertexChooser == null || vertexChooser.isChosen(vertex));
	}

	private void setDiscoveryTime(VertexType vertex, int ts) {
		discoveryTimeList[vertex.getLabel()] = ts;
	}

	private void setFinishTime(VertexType vertex, int ts) {
		finishTimeList[vertex.getLabel()] = ts;
	}

	private void setDFSEdgeType(EdgeType edge, int dfsEdgeType) {
		dfsEdgeTypeList[edge.getLabel()] = dfsEdgeType;
	}
}

// vim:ts=4
