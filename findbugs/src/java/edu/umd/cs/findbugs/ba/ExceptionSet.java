/*
 * Bytecode Analysis Framework
 * Copyright (C) 2004 University of Maryland
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

package edu.umd.cs.findbugs.ba;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

/**
 * Class for keeping track of exceptions that can be
 * thrown by an instruction.  We distinguish <em>explicit</em>
 * and <em>implicit</em> exceptions.  Explicit exceptions
 * are explicitly declared, thrown, or caught.  Implicit exceptions
 * are runtime faults (NPE, array out of bounds)
 * not explicitly handled by the user code.
 *
 * @see ThrownException
 * @see TypeAnalysis
 * @author David Hovemeyer
 */
public class ExceptionSet {
	private Map<ObjectType, ThrownException> map;
	private boolean universalHandler;
	private Type commonSupertype;

	/**
	 * Constructor.
	 * Creates an empty set.
	 */
	public ExceptionSet() {
		this.map = new HashMap<ObjectType, ThrownException>();
		this.universalHandler = false;
	}

	/**
	 * Return an exact copy of this object.
	 */
	public ExceptionSet duplicate() {
		ExceptionSet dup = new ExceptionSet();
		for (Iterator<ThrownException> i = iterator(); i.hasNext(); ) {
			dup.add(i.next().duplicate());
		}
		return dup;
	}

	public int hashCode() {
		return map.hashCode();
	}

	public boolean equals(Object o) {
		if (o == null) return false;
		if (o.getClass() != this.getClass()) return false;

		ExceptionSet other = (ExceptionSet) o;
		return map.equals(other.map) && universalHandler == other.universalHandler;
	}

	/**
	 * Get the least (lowest in the lattice) common supertype
	 * of the exceptions in the set.  Returns the special TOP
	 * type if the set is empty.
	 */
	public Type getCommonSupertype() throws ClassNotFoundException {
		if (commonSupertype != null)
			return commonSupertype;

		if (isEmpty()) {
			// This probably means that we're looking at an
			// infeasible exception path.
			return TypeFrame.getTopType();
		}

		// Compute first common superclass
		Iterator<ThrownException> i = iterator();
		ReferenceType result = i.next().getType();
		while (i.hasNext()) {
			result = result.getFirstCommonSuperclass(i.next().getType());
			if (result == null) {
				// This should only happen if the class hierarchy
				// is incomplete.  We'll just be conservative.
				result = Type.THROWABLE;
				break;
			}
		}

		// Cache and return the result
		commonSupertype = result;
		return result;
	}

	/**
	 * Return an iterator over ThrownExceptions in the set.
	 */
	public Iterator<ThrownException> iterator() { return map.values().iterator(); }

	/**
	 * Return whether or not the set is empty.
	 */
	public boolean isEmpty() { return map.isEmpty(); }

	/**
	 * Add an explicit exception.
	 * @param type type of the exception
	 */
	public void addExplicit(ObjectType type) {
		add(new ThrownException(type, true));
	}

	/**
	 * Add an implicit exception.
	 * @param type type of the exception
	 */
	public void addImplicit(ObjectType type) {
		add(new ThrownException(type, false));
	}

	/**
	 * Add all exceptions in the given set.
	 * @param other the set
	 */
	public void addAll(ExceptionSet other) {
		for (Iterator<ThrownException> i = other.iterator(); i.hasNext(); ) {
			add(i.next().duplicate());
		}
	}

	private void add(ThrownException thrownException) {
		ThrownException old = map.put(thrownException.getType(), thrownException);

		// Avoid replacing an explicit exception with an identical
		// implicit exception.
		if (old != null && old.isExplicit())
			thrownException.setExplicit(true);

		// Invalidate cached common superclass
		commonSupertype = null;
	}

	/**
	 * Remove all exceptions from the set.
	 */
	public void clear() {
		map.clear();
		commonSupertype = null;
	}

	/**
	 * Return whether or not a universal exception handler
	 * was reached by the set.
	 */
	public void sawUniversal() {
		universalHandler = true;
		map.clear();
		commonSupertype = null;
	}

	/**
	 * Mark the set as having reached a universal exception handler.
	 */
	public boolean sawUniversalHandler() {
		return universalHandler;
	}

	/**
	 * Return whether or not the set contains any checked exceptions.
	 */
	public boolean containsCheckedExceptions() throws ClassNotFoundException {
		for (Iterator<ThrownException> i = iterator(); i.hasNext(); ) {
			ThrownException thrownException = i.next();
			if (!Hierarchy.isUncheckedException(thrownException.getType()))
				return true;
		}
		return false;
	}

	/**
	 * Return whether or not the set contains any explicit exceptions.
	 */
	public boolean containsExplicitExceptions() {
		for (Iterator<ThrownException> i = iterator(); i.hasNext(); ) {
			if (i.next().isExplicit())
				return true;
		}
		return false;
	}
}

// vim:ts=4
