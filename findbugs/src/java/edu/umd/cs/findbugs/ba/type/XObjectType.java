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

import edu.umd.cs.findbugs.graph.AbstractVertex;

/**
 * Lightweight data structure representing an
 * <i>object type</i>: a node in the
 * class hierarchy (i.e., a class or interface).
 * Note that not all object types represent Java classes
 * or interfaces: e.g., array types.
 *
 * <p> Instances of XType participate in the flyweight pattern,
 * meaning there is at most one instance per type.
 * Instances should be created and accessed using the XTypeRepository
 * class.
 *
 * @author David Hovemeyer
 */
public abstract class XObjectType
	extends AbstractVertex<InheritanceGraphEdge, XObjectType>
	implements XReferenceType {

	public static final int UNCHECKED = 0;
	public static final int KNOWN = 1;
	public static final int UNKNOWN = 2;

	private String typeSignature;
	private int state;
	//private boolean supertypesKnown;

	protected XObjectType(String typeSignature) {
		this.typeSignature = typeSignature;
		this.state = UNCHECKED;
	}

	/**
	 * Get the state of this type: UNCHECKED, KNOWN, or UNKNOWN.
	 * This information is used by XTypeRepository to determine
	 * when it needs to perform lazy hierarchy graph construction,
	 * or to dynamically resolve a class type.
	 * <ul>
	 * <li> UNCHECKED means that the type has been created,
	 *      but we may not have seen the representation of the
	 *      type (e.g., class file), and the supertypes of
	 *      the type have not been created
	 * <li> KNOWN means that the representation of the type has
	 *      been seen, and the supertype vertices and links
	 *      have been added (although the supertypes may not
	 *      have been checked yet)
	 * <li> UNKNOWN means that an attempt to check the type
	 *      for information (e.g., class lookup) failed,
	 *      and that any query involving this type should
	 *      throw an exception to indicate missing information 
	 * </ul>
	 */
	public int getState() {
		return state;
	}

	/**
	 * Set the state: UNCHECKED, KNOWN, or UNKNOWN.
	 * @param state the state
	 * @see {@link #getState()}
	 */
	void setState(int state) {
		this.state = state;
	}

	public String getSignature() {
		return typeSignature;
	}

	public boolean isBasicType() {
		return false;
	}

	public boolean isReferenceType() {
		return true;
	}

	// All object types are valid as array element types.
	public boolean isValidArrayElementType() {
		return true;
	}

	/**
	 * Is this type an interface type (as opposed to a class or array type)?
	 */
	public abstract boolean isInterface() throws UnknownTypeException;

	/**
	 * Is this type an array type?
	 */
	public abstract boolean isArray();
}

// vim:ts=4
