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

// $Revision: 1.3 $

package edu.umd.cs.daveho.graph;

import java.util.*;

/**
 * Algorithm to label vertices in one graph the same as equivalent 
 * vertices in another graph.
 */
public class LabelEquivalentVertices<
	GraphType extends Graph<EdgeKey, VertexKey, EdgeType, VertexType>,
	EdgeKey,
	VertexKey,
	EdgeType extends GraphEdge<VertexType, EdgeKey>,
	VertexType extends GraphVertex<VertexKey>
	> {

	/** Constructor. */
	public LabelEquivalentVertices() { }

	/**
	 * Label vertices in one graph the same as equivalent 
	 * vertices in another graph.  This algorithm should only be
	 * used if graph "b" has an equivalent vertex for every vertex
	 * in "a".
	 *
	 * @param a the GraphType to be assigned new vertex labels
	 * @param b the GraphType to use as the source of the new labels
	 */
	public void labelEquivalentVertices(GraphType a, GraphType b) {

		Iterator<VertexType> i = a.getVertexIterator();
		while (i.hasNext()) {
			VertexType origVertex = i.next();
			VertexType newVertex = b.getVertex(origVertex.getVertexKey());
			if (newVertex != null)
				origVertex.setLabel(newVertex.getLabel());
		}

		a.setNumLabels(b.getNumLabels());

	}

}

// vim:ts=4
