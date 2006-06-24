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

package edu.umd.cs.findbugs.ba.type2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Class to cache the result of an isSubtype() query
 * so future lookups are fast.  Also caches a complete
 * list of  supertypes in BFS order.
 *
 * @author David Hovemeyer
 * @see TypeRepository
 */
public class SubtypeQueryResult {
	private BitSet supertypeSet;
	private ArrayList<ObjectType> supertypeListInBFSOrder;
	private String[] missingClassList;

	private static final String[] emptyList = new String[0];

	/**
	 * Constructor.
	 */
	public SubtypeQueryResult() {
		this.supertypeSet = new BitSet();
		this.supertypeListInBFSOrder = new ArrayList<ObjectType>();
		this.missingClassList = emptyList;
	}

	/**
	 * Set given ObjectType as a supertype.
	 * This method should be called on supertypes in
	 * breadth-first order.  This allows first-common-superclass
	 * searches to be performed using a linear search
	 * of the  supertypes in BFS order.
	 *
	 * @param supertype the supertype
	 */
	public void addSupertype(ObjectType supertype) {
		supertypeSet.set(supertype.getLabel());
		supertypeListInBFSOrder.add(supertype);
	}

	/**
	 * Mark the object as complete.
	 *
	 * @param missingClassList the list of names of missing classes
	 */
	//@SuppressWarnings("EI2")
	public void finish(String[] missingClassList) {
		if (missingClassList.length > 0)
			this.missingClassList = missingClassList;
		supertypeListInBFSOrder.trimToSize();
	}

	/**
	 * Check to see if given ObjectType is a supertype.
	 *
	 * @param type potential supertype
	 * @return true if type is a supertype, false otherwise
	 */
	public boolean isSupertype(ObjectType type) throws ClassNotFoundException {
		boolean isSupertype = supertypeSet.get(type.getLabel());

		// NOTE: missing classes only represent lack of accuracy
		// if the query is negative (meaning that parts of the
		// class hierarchy were not explored).
		if (missingClassList.length > 0 && !isSupertype)
			throw new ClassNotFoundException("Class not found: " + Arrays.toString(missingClassList));

		return isSupertype;
	}

	/**
	 * Get set of supertypes.
	 * This is all supertypes including the object type itself.
	 *
	 * @param subtype the subtype (for which this object stores the set of supertypes)
	 * @param repos   the TypeRepository
	 * @return the set of supertypes (a new object, can be modified)
	 */
	public Set<ObjectType> getSupertypeSet(ObjectType subtype, TypeRepository repos)
	        throws ClassNotFoundException {

		// Throw ClassNotFoundException if we can't answer the
		// query authoritatively
		if (missingClassList.length > 0)
			throw new ClassNotFoundException("Class not found: " + Arrays.toString(missingClassList));

		return new HashSet<ObjectType>(supertypeListInBFSOrder);
	}

	/**
	 * Get iterator over  supertypes in BFS order.
	 */
	public Iterator<ObjectType> supertypeInBFSOrderIterator() {
		return supertypeListInBFSOrder.iterator();
	}
}

// vim:ts=4
