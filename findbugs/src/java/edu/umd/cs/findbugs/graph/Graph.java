/*
 * Generic graph library
 * Copyright (C) 2000,2003 University of Maryland
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

// $Revision: 1.4 $

package edu.umd.cs.daveho.graph;

import java.io.Serializable;

import java.util.Iterator;

import edu.umd.cs.daveho.graph.GraphVertex;
import edu.umd.cs.daveho.graph.GraphEdge;

/**
 * Graph interface; defines the operations used to access and manipulate
 * a graph.
 */
public interface Graph
	<
	EdgeType extends GraphEdge<EdgeType, VertexType>,
	VertexType extends GraphVertex<VertexType>
	> extends Serializable {

	/** Get number of edges in the graph. */
	public int getNumEdges();

	/** Get number of vertices in the graph. */
	public int getNumVertices();

	/** Get Iterator over all edges in the graph. */
	public Iterator<EdgeType> getEdgeIterator();

	/** Get Iterator over all vertices in the graph. */
	public Iterator<VertexType> getVertexIterator();

	/**
	 * Add a vertex to the graph.
	 * @return the new vertex
	 */
	public VertexType addVertex();

	/**
	 * Add an edge to the graph.
	 * Duplicate edges (with same source and target vertices) are allowed.
	 * @param source the source vertex
	 * @param target the target vertex
	 * @return the new edge
	 */
	public EdgeType addEdge(VertexType source, VertexType target);

	/**
	 * Look up an edge by source and target vertex.
	 * If multiple edges with same source and target vertex exist,
	 * one is selected arbitrarily.
	 * @param source the source vertex
	 * @param target the target vertex
	 * @return a matching edge, or null if there is no matching edge
	 */
	public EdgeType lookupEdge(VertexType source, VertexType target);

	/**
	 * Get the number of numeric (integer) labels that have been assigned to vertices
	 * in the graph.  All vertices in the graph are guaranteed to have labels in the
	 * range 0..n, where n is the value returned by this method.
	 */
	public int getNumLabels();

	/**
	 * Reset number of (integer) labels.  This might be necessary
	 * if an algorithm has assigned new labels to a graph's vertices.
	 */
	public void setNumLabels(int numLabels);

	/**
	 * Remove given edge from the graph.
	 */
	public void removeEdge(EdgeType e);

	/**
	 * Remove given vertex from the graph.
	 * Note that all edges referencing the vertex will be
	 * removed.
	 */
	public void removeVertex(VertexType v);

	/** Get an iterator over the adjacency list
		(vertices reachable through this vertex's outgoing edges). */
	public Iterator<VertexType> adjacencyListIterator(VertexType source);

	// TODO: add other methods for accessing edges and vertices

}

// vim:ts=4
