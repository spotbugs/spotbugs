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

// $Revision: 1.1 $

package edu.umd.cs.daveho.graph;

import java.util.Comparator;

/**
 * GraphToolkit defines the interface used by graph algorithms to
 * create graph components, and to compare and access information
 * about them.
 */
public interface GraphToolkit<
	GraphType extends Graph<EdgeInfo, VertexInfo, EdgeType, VertexType>,
	EdgeInfo,
	VertexInfo,
	EdgeType extends GraphEdge<VertexType, EdgeInfo>,
	VertexType extends GraphVertex<VertexInfo>
	> {

	/** Create a Graph object. */
	public GraphType createGraph();

	/** Create a GraphVertex object. */
	public VertexType createVertex(VertexInfo vertexInfo, int label);

	/** Create an GraphEdge object. */
	public EdgeType createEdge(VertexType source, VertexType target, EdgeInfo edgeInfo);

	/** Duplicate an EdgeInfo object. */
	public EdgeInfo duplicateEdgeInfo(EdgeInfo edgeInfo);

	/** Duplicate a VertexInfo object. */
	public VertexInfo duplicateVertexInfo(VertexInfo vertexInfo);

}

// vim:ts=4
