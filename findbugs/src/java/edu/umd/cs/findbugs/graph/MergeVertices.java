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

import java.util.*;

/**
 * Algorithm to merge a set of vertices into a single vertex.
 * Note that the graph is modified as part of this process.
 */
public class MergeVertices <
	GraphType extends Graph<EdgeInfo, VertexInfo, EdgeType, VertexType>,
	EdgeInfo,
	VertexInfo,
	EdgeType extends GraphEdge<VertexType, EdgeInfo>,
	VertexType extends GraphVertex<VertexInfo>
	> {

	/** Constructor. */
	public MergeVertices() { }

	/**
	 * Merge the specified set of vertices into a single vertex.
	 * @param vertexSet the set of vertices to be merged
	 * @param g the graph to be modified
	 * @param combinator object used to combine VertexInfo objects
	 * @param toolkit a GraphToolkit to be used to create new 
	 *	graph components
	 */
	public void mergeVertices(Set<VertexType> vertexSet, GraphType g, VertexInfoCombinator<VertexInfo> combinator,
							   GraphToolkit<GraphType, EdgeInfo, VertexInfo, EdgeType, VertexType> toolkit) {

		// Special case: if the vertex set contains a single vertex
		// or is empty, there is nothing to do
		if (vertexSet.size() <= 1)
			return;

		// Get all vertices to which we have outgoing edges
		// or from which we have incoming edges, since they'll need
		// to be fixed
		TreeSet<EdgeType> edgeSet = new TreeSet<EdgeType>();
		for (Iterator<EdgeType> i = g.getEdgeSet().iterator(); i.hasNext(); ) {
			EdgeType e = i.next();
			if (vertexSet.contains(e.getSource()) || vertexSet.contains(e.getTarget()))
				edgeSet.add(e);
		}

		// Combine all of the VertexInfo objects into a single composite object
		VertexInfo compositeVertexInfo = null;
		for (Iterator<VertexType> i = vertexSet.iterator(); i.hasNext(); ) {
			VertexType v = i.next();
			if (compositeVertexInfo == null)
				compositeVertexInfo = toolkit.duplicateVertexInfo(v.getVertexInfo());
			else
				compositeVertexInfo = combinator.combineVertexInfo(compositeVertexInfo, v.getVertexInfo());
		}

		// Create the new composite vertex
		VertexType compositeVertex = g.addVertex(compositeVertexInfo, toolkit);

		// For each original edge into or out of the vertex set,
		// create an equivalent edge referencing the composite
		// vertex
		for (Iterator<EdgeType> i = edgeSet.iterator(); i.hasNext(); ) {
			EdgeType e = i.next();
			VertexType source = vertexSet.contains(e.getSource()) ? compositeVertex : e.getSource();
			VertexType target = vertexSet.contains(e.getTarget()) ? compositeVertex : e.getTarget();

//			  if (source != compositeVertex && target != compositeVertex)
//				  System.out.println("BIG OOPS!");

			// Don't create a self edge for the composite vertex
			// unless one of the vertices in the vertex set
			// had a self edge
			if (source == compositeVertex && target == compositeVertex &&
				 e.getSource() != e.getTarget())
				continue;

			g.addEdge(toolkit.duplicateVertexInfo(source.getVertexInfo()),
					   toolkit.duplicateVertexInfo(target.getVertexInfo()),
					   toolkit.duplicateEdgeInfo(e.getEdgeInfo()),
					   toolkit);
		}

		// Remove all of the vertices in the vertex set; this will
		// automatically remove the edges into and out of those
		// vertices
		for (Iterator<VertexType> i = vertexSet.iterator(); i.hasNext(); ) {
			g.removeVertex(i.next());
		}

	}

}

// vim:ts=4
