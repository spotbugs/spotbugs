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

// $Revision: 1.2 $

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
	EdgeInfo,
	VertexInfo,
	EdgeType extends GraphEdge<VertexType, EdgeInfo>,
	VertexType extends GraphVertex<VertexInfo>
	> extends Serializable {

	/** Get number of edges in the graph. */
	public int getNumEdges();

	/** Get number of vertices in the graph. */
	public int getNumVertices();

	/** Get Iterator over all edges in the graph. */
	public Iterator<EdgeType> getEdgeIterator();

	/** Get Iterator over all vertices in the graph. */
	public Iterator<VertexType> getVertexIterator();

	/** Add a vertex to the graph.
	 * @param vertexInfo a VertexInfo object uniquely identifying the vertex
	 * @param toolkit the GraphToolkit used to construct graph components
	 * @return the new vertex, or an equivalent existing vertex
	 */
	public VertexType addVertex(VertexInfo vertexInfo, GraphToolkit toolkit);

	/** Add an edge to the graph.
	 * @param sourceInfo a VertexInfo object uniquely identifying the source vertex
	 * @param targetInfo a VertexInfo object uniquely identifying the target vertex
	 * @param edgeInfo an EdgeInfo object providing auxiliary information about
	 *	 the edge
	 * @param toolkit the GraphToolkit used to construct graph components
	 * @return the new edge, or an equivalent existing edge
	 */
	public EdgeType addEdge(VertexInfo sourceInfo, VertexInfo targetInfo, EdgeInfo edgeInfo,
						 GraphToolkit toolkit);

	/**
	 * Get the vertex identified by given VertexInfo object, or null
	 * if no such vertex exists.
	 */
	public VertexType getVertex(VertexInfo vertexInfo);

	/**
	 * Get the edge identifier whose source and target match given VertexInfo objects
	 * and whose auxiliary information matches given EdgeInfo object, or null
	 * if no such edge exists.
	 */
	public EdgeType getEdge(VertexInfo sourceInfo, VertexInfo targetInfo, EdgeInfo edgeInfo);

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
