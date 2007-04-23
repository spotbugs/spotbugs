/*
 * Generic graph library
 * Copyright (C) 2000,2003,2004 University of Maryland
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

// $Revision: 1.14 $

package edu.umd.cs.findbugs.graph;

import java.util.IdentityHashMap;
import java.util.Iterator;

/**
 * Algorithm to transpose a graph.
 */
public class Transpose
		<
		GraphType extends Graph<EdgeType, VertexType>,
		EdgeType extends GraphEdge<EdgeType, VertexType>,
        VertexType extends GraphVertex<VertexType>
		> {

	private IdentityHashMap<VertexType, VertexType> m_origToTransposeMap;
	private IdentityHashMap<VertexType, VertexType> m_transposeToOrigMap;

	/**
	 * Constructor.
	 */
	public Transpose() {
		m_origToTransposeMap = new IdentityHashMap<VertexType, VertexType>();
		m_transposeToOrigMap = new IdentityHashMap<VertexType, VertexType>();
	}

	/**
	 * Transpose a graph.  Note that the original graph is not modified;
	 * the new graph and its vertices and edges are new objects.
	 *
	 * @param orig    the graph to transpose
	 * @param toolkit a GraphToolkit to be used to create the transposed Graph
	 * @return the transposed Graph
	 */
	public GraphType transpose(GraphType orig, GraphToolkit<GraphType, EdgeType, VertexType> toolkit) {

		GraphType trans = toolkit.createGraph();

		// For each vertex in original graph, create an equivalent
		// vertex in the transposed graph,
		// ensuring that vertex labels in the transposed graph
		// match vertex labels in the original graph
		for (Iterator<VertexType> i = orig.vertexIterator(); i.hasNext();) {
			VertexType v = i.next();

			// Make a duplicate of original vertex
			// (Ensuring that transposed graph has same labeling as original)
			VertexType dupVertex = toolkit.duplicateVertex(v);
			dupVertex.setLabel(v.getLabel());
			trans.addVertex(v);

			// Keep track of correspondence between equivalent vertices
			m_origToTransposeMap.put(v, dupVertex);
			m_transposeToOrigMap.put(dupVertex, v);
		}
		trans.setNumVertexLabels(orig.getNumVertexLabels());

		// For each edge in the original graph, create a reversed edge
		// in the transposed graph
		for (Iterator<EdgeType> i = orig.edgeIterator(); i.hasNext();) {
			EdgeType e = i.next();

			VertexType transSource = m_origToTransposeMap.get(e.getTarget());
			VertexType transTarget = m_origToTransposeMap.get(e.getSource());

			EdgeType dupEdge = trans.createEdge(transSource, transTarget);
			dupEdge.setLabel(e.getLabel());

			// Copy auxiliary information for edge
			toolkit.copyEdge(e, dupEdge);
		}
		trans.setNumEdgeLabels(orig.getNumEdgeLabels());

		return trans;

	}

	/**
	 * Get the vertex in the transposed graph which corresponds to the
	 * given vertex in the original graph.
	 *
	 * @param v the vertex in the original graph
	 * @return the equivalent vertex in the transposed graph
	 */
	public VertexType getTransposedGraphVertex(VertexType v) {
		return m_origToTransposeMap.get(v);
	}

	/**
	 * Get the vertex in the original graph which corresponds to the
	 * given vertex in the transposed graph.
	 *
	 * @param v the vertex in the transposed graph
	 * @return the equivalent vertex in the original graph
	 */
	public VertexType getOriginalGraphVertex(VertexType v) {
		return m_transposeToOrigMap.get(v);
	}

}

// vim:ts=4
