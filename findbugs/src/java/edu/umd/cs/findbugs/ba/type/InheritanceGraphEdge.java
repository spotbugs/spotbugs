/*
 * Bytecode Analysis Framework
 * Copyright (C) 2004, University of Maryland
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

package edu.umd.cs.findbugs.ba.type;

import edu.umd.cs.findbugs.graph.AbstractEdge;

/**
 * An edge in the graph of direct inheritance (supertype/subtype)
 * relationships.  ObjectType objects are the vertices
 * in the inheritance graph.
 *
 * @author David Hovemeyer
 */
public class InheritanceGraphEdge
        extends AbstractEdge<InheritanceGraphEdge, ObjectType> {

	private int type;

	/**
	 * Constructor.
	 *
	 * @param subtype   the subtype
	 * @param supertype the supertype
	 */
	InheritanceGraphEdge(ObjectType subtype, ObjectType supertype) {
		super(subtype, supertype);
	}

	/**
	 * Set the type of inheritance edge.
	 *
	 * @param type the type of inheritance edge
	 * @see InheritanceGraphEdgeTypes
	 */
	void setType(int type) {
		this.type = type;
	}

	/**
	 * Get the type of inheritance edge.
	 *
	 * @return type the type of inheritance edge
	 * @see InheritanceGraphEdgeTypes
	 */
	public int getType() {
		return type;
	}
}

// vim:ts=4
