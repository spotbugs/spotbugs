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

import java.util.Comparator;

/**
 * GraphToolkit defines the interface used by graph algorithms to
 * create graph components, and to compare and access information
 * about them.
 */
public interface GraphToolkit<
	GraphType extends Graph<EdgeKey, VertexKey, EdgeType, VertexType>,
	EdgeKey,
	VertexKey,
	EdgeType extends GraphEdge<VertexType, EdgeKey>,
	VertexType extends GraphVertex<VertexKey>
	> {

	/** Create a Graph object. */
	public GraphType createGraph();

	/** Create a GraphVertex object. */
	public VertexType createVertex(VertexKey vertexKey, int label);

	/** Create an GraphEdge object. */
	public EdgeType createEdge(VertexType source, VertexType target, EdgeKey edgeKey);

	/** Duplicate an EdgeKey object. */
	public EdgeKey duplicateEdgeKey(EdgeKey edgeKey);

	/** Duplicate a VertexKey object. */
	public VertexKey duplicateVertexKey(VertexKey vertexKey);

}

// vim:ts=4
