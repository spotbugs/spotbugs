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

	/**
	 * Constructor.
	 * Creates an empty set.
	 */
	public ExceptionSet() {
		this.map = new HashMap<ObjectType, ThrownException>();
		this.universalHandler = false;
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

	private void add(ThrownException thrownException) {
		ThrownException old = map.put(thrownException.getType(), thrownException);

		// Avoid replacing an explicit exception with an identical
		// implicit exception.
		if (old != null && old.isExplicit())
			thrownException.setExplicit(true);
	}

	/**
	 * Return whether or not a universal exception handler
	 * was reached by the set.
	 */
	public void sawUniversal() {
		universalHandler = true;
		map.clear();
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
