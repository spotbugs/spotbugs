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

package edu.umd.cs.daveho.graph;

import java.util.ArrayList;
import java.util.Iterator;

public abstract class AbstractGraph
	<
	EdgeType extends AbstractEdge<EdgeType, VertexType>,
	VertexType extends AbstractVertex<EdgeType, VertexType>
	> implements Graph<EdgeType, VertexType> {

	private ArrayList<VertexType> vertexList;
	private ArrayList<EdgeType> edgeList;
	private int maxVertexLabel;
	private int nextVertexId;
	private int maxEdgeLabel;

	protected AbstractGraph() {
		this.vertexList = new ArrayList<VertexType>();
		this.edgeList = new ArrayList<EdgeType>();
		this.maxVertexLabel = 0;
		this.nextVertexId = 0;
		this.maxEdgeLabel = 0;
	}

	public int getNumEdges() {
		return edgeList.size();
	}

	public int getNumVertices() {
		return vertexList.size();
	}

	public Iterator<EdgeType> getEdgeIterator() {
		return edgeList.iterator();
	}

	public Iterator<VertexType> getVertexIterator() {
		return vertexList.iterator();
	}

	public VertexType addVertex() {
		VertexType v = createVertex();
		int id = nextVertexId++;
		maxVertexLabel = nextVertexId;
		v.setId(id);
		v.setLabel(id);
		return v;
	}

	public EdgeType addEdge(VertexType source, VertexType target) {
		EdgeType edge = createEdge(source, target);
		edge.setLabel(maxEdgeLabel++);
		return edge;
	}

	public abstract VertexType createVertex();
	public abstract EdgeType createEdge(VertexType source, VertexType target);

}

// vim:ts=4
