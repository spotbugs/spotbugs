/*
 * Generic graph library
 * Copyright (C) 2000,2003,2004,2007 University of Maryland
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
 * GraphVertex implementation for use with AbstractGraph.
 *
 * @see GraphVertex
 * @see AbstractGraph
 * @see AbstractEdge
 * @author David Hovemeyer
 */
public class AbstractVertex <
		EdgeType extends AbstractEdge<EdgeType, ActualVertexType>,
		ActualVertexType extends AbstractVertex<EdgeType, ActualVertexType>
		> implements GraphVertex<ActualVertexType> {

	private int label;
	EdgeType firstIncomingEdge, lastIncomingEdge;
	EdgeType firstOutgoingEdge, lastOutgoingEdge;

	public int getLabel() {
		return label;
	}

	public void setLabel(int label) {
		this.label = label;
	}

	@Override
	public int hashCode() {
		return label;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null || o.getClass() != this.getClass()) {
			return false;
		}
		AbstractVertex other = (AbstractVertex) o;
		return other.getLabel() == this.getLabel();
	}
	
	public int compareTo(ActualVertexType other) {
		if (this.getLabel() < other.getLabel()) {
			return -1;
		} else if (this.getLabel() > other.getLabel()) {
			return 1;
		} else {
			return 0;
		}
	}

	void addOutgoingEdge(EdgeType edge) {
		if (firstOutgoingEdge == null) {
			firstOutgoingEdge = lastOutgoingEdge = edge;
		} else {
			lastOutgoingEdge.setNextOutgoingEdge(edge);
			lastOutgoingEdge = edge;
		}
	}

	EdgeType getFirstOutgoingEdge() {
		return firstOutgoingEdge;
	}

	void addIncomingEdge(EdgeType edge) {
		if (firstIncomingEdge == null) {
			firstIncomingEdge = lastIncomingEdge = edge;
		} else {
			lastIncomingEdge.setNextIncomingEdge(edge);
			lastIncomingEdge = edge;
		}
	}

	EdgeType getFirstIncomingEdge() {
		return firstIncomingEdge;
	}

	void removeIncomingEdge(EdgeType edge) {
		EdgeType prev = null, cur = firstIncomingEdge;
		while (cur != null) {
			EdgeType next = cur.getNextIncomingEdge();
			if (cur.equals(edge)) {
				if (prev != null)
					prev.setNextIncomingEdge(next);
				else
					firstIncomingEdge = next;
				return;
			}
			prev = cur;
			cur = next;
		}
		throw new IllegalArgumentException("removing nonexistent edge!");
	}

	void removeOutgoingEdge(EdgeType edge) {
		EdgeType prev = null, cur = firstOutgoingEdge;
		while (cur != null) {
			EdgeType next = cur.getNextOutgoingEdge();
			if (cur.equals(edge)) {
				if (prev != null)
					prev.setNextOutgoingEdge(next);
				else
					firstOutgoingEdge = next;
				return;
			}
			prev = cur;
			cur = cur.getNextOutgoingEdge();
		}
		throw new IllegalArgumentException("removing nonexistent edge!");
	}


}

// vim:ts=4
