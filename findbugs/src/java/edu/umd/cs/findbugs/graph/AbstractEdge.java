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

package edu.umd.cs.findbugs.graph;

/**
 * GraphEdge implementation for use with AbstractGraph.
 *
 * @see GraphEdge
 * @see AbstractGraph
 * @see AbstractVertex
 * @author David Hovemeyer
 */
public class AbstractEdge
		<
		ActualEdgeType extends AbstractEdge<ActualEdgeType, VertexType>,
		VertexType extends AbstractVertex<ActualEdgeType, VertexType>
		> implements GraphEdge<ActualEdgeType, VertexType> {

	private VertexType source;
	private VertexType target;
	private int label;
	private ActualEdgeType nextOutgoingEdge;
	private ActualEdgeType nextIncomingEdge;

	/**
	 * Constructor.
	 * @param source the source vertex of the edge
	 * @param target the target vertex of the edge
	 */
	public AbstractEdge(VertexType source, VertexType target) {
		this.source = source;
		this.target = target;
	}

	public VertexType getSource() {
		return source;
	}

	public VertexType getTarget() {
		return target;
	}

	public int getLabel() {
		return label;
	}

	public void setLabel(int label) {
		this.label = label;
	}

	@Override
	public int hashCode() {
		return source.hashCode() + target.hashCode()*3;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof AbstractEdge)) return false;
		AbstractEdge other = (AbstractEdge) o;
		return source.equals(other.source) && target.equals(other.target);
	}

	public int compareTo(ActualEdgeType other) {
		int cmp = source.compareTo(other.source);
		if (cmp != 0)
			return cmp;
		return target.compareTo(other.target);
	}

	void setNextOutgoingEdge(ActualEdgeType edge) {
		nextOutgoingEdge = edge;
	}

	ActualEdgeType getNextOutgoingEdge() {
		return nextOutgoingEdge;
	}

	void setNextIncomingEdge(ActualEdgeType edge) {
		nextIncomingEdge = edge;
	}

	ActualEdgeType getNextIncomingEdge() {
		return nextIncomingEdge;
	}

}

// vim:ts=4
