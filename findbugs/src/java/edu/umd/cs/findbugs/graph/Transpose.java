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

import java.util.*;

/**
 * Algorithm to transpose a graph.
 */
public class Transpose<
	GraphType extends Graph<EdgeInfo, VertexInfo, EdgeType, VertexType>,
	EdgeInfo,
	VertexInfo,
	EdgeType extends GraphEdge<VertexType, EdgeInfo>,
	VertexType extends GraphVertex<VertexInfo>
	> {

	/** Constructor. */
	public Transpose() { }

	/**
	 * Transpose a graph.  Note that the original graph is not modified;
	 * the new graph and its vertices and edges are new objects.
	 * @param orig the graph to transpose
	 * @param toolkit used to create the new graph, vertices, and edges
	 * @return the transposed Graph
	 */
	public GraphType transpose(GraphType orig,
		GraphToolkit<GraphType, EdgeInfo, VertexInfo, EdgeType, VertexType> toolkit) {

		GraphType trans = toolkit.createGraph();

		// For each vertex in original graph, create an equivalent
		// vertex in the transposed graph
		for (Iterator<VertexType> i = orig.getVertexIterator(); i.hasNext(); ) {
			VertexType v = (VertexType) i.next();
			trans.addVertex(toolkit.duplicateVertexInfo(v.getVertexInfo()), toolkit);
		}

		// Ensure that vertex labels in the transposed graph
		// match vertex labels in the original graph
		LabelEquivalentVertices<GraphType, EdgeInfo, VertexInfo, EdgeType, VertexType> l =
			new LabelEquivalentVertices<GraphType, EdgeInfo, VertexInfo, EdgeType, VertexType>();
		l.labelEquivalentVertices(trans, orig);

		// For each edge in the original graph, create a reversed edge
		// in the transposed graph
		for (Iterator<EdgeType> i = orig.getEdgeIterator(); i.hasNext(); ) {
			EdgeType e = i.next();
			trans.addEdge(toolkit.duplicateVertexInfo(e.getTarget().getVertexInfo()),
						   toolkit.duplicateVertexInfo(e.getSource().getVertexInfo()),
						   toolkit.duplicateEdgeInfo(e.getEdgeInfo()),
						   toolkit);
		}

		return trans;

	}

}

// vim:ts=4
