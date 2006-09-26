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

package edu.umd.cs.findbugs.ba.type;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.ba.Hierarchy;

/**
 * Class for keeping track of exceptions that can be
 * thrown by an instruction.  We distinguish <em>explicit</em>
 * and <em>implicit</em> exceptions.  Explicit exceptions
 * are explicitly declared, thrown, or caught.  Implicit exceptions
 * are runtime faults (NPE, array out of bounds)
 * not explicitly handled by the user code.
 *
 * @author David Hovemeyer
 * @see TypeAnalysis
 */
public class ExceptionSet implements Serializable {
        private static final long serialVersionUID = 1;

	private ExceptionSetFactory factory;
	private BitSet exceptionSet;
	private BitSet explicitSet;
	private int size;
	private boolean universalHandler;
	private Type commonSupertype;

	/**
	 * Object to iterate over the exception types in the set.
	 */
	public class ThrownExceptionIterator implements Iterator<ObjectType> {
		private int last = -1, next = -1;

		ThrownExceptionIterator() {
			findNext();
		}

		public boolean hasNext() {
			if (last == next)
				findNext();
			return next < factory.getNumTypes();
		}

		public ObjectType next() {
			if (!hasNext())
				throw new NoSuchElementException();
			ObjectType result = factory.getType(next);
			last = next;
			return result;
		}

		public boolean isExplicit() {
			return explicitSet.get(last);
		}

		public void remove() {
			exceptionSet.clear(last);
			explicitSet.clear(last);
			--size;
			commonSupertype = null;
		}

		private void findNext() {
			++next;
			while (next < factory.getNumTypes()) {
				if (exceptionSet.get(next))
					break;
				++next;
			}
		}
	}

	/**
	 * Constructor.
	 * Creates an empty set.
	 */
	ExceptionSet(ExceptionSetFactory factory) {
		this.factory = factory;
		this.exceptionSet = new BitSet();
		this.explicitSet = new BitSet();
		this.size = 0;
		this.universalHandler = false;
	}

	/**
	 * Return an exact copy of this object.
	 */
	public ExceptionSet duplicate() {
		ExceptionSet dup = factory.createExceptionSet();
		dup.exceptionSet.clear();
		dup.exceptionSet.or(this.exceptionSet);
		dup.explicitSet.clear();
		dup.explicitSet.or(this.explicitSet);
		dup.size = this.size;
		dup.universalHandler = this.universalHandler;
		dup.commonSupertype = this.commonSupertype;

		return dup;
	}

	@Override
         public int hashCode() {
		return exceptionSet.hashCode() + explicitSet.hashCode();
	}

	@Override
         public boolean equals(Object o) {
		if (o == null) return false;
		if (o.getClass() != this.getClass()) return false;

		ExceptionSet other = (ExceptionSet) o;
		return exceptionSet.equals(other.exceptionSet)
		        && explicitSet.equals(other.explicitSet)
		        && universalHandler == other.universalHandler;
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
		ThrownExceptionIterator i = iterator();
		ReferenceType result = i.next();
		while (i.hasNext()) {
			result = result.getFirstCommonSuperclass(i.next());
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
	 * Return an iterator over thrown exceptions.
	 */
	public ThrownExceptionIterator iterator() {
		return new ThrownExceptionIterator();
	}

	/**
	 * Return whether or not the set is empty.
	 */
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * Checks to see if the exception set is a singleton set
	 * containing just the named exception
	 * @param exceptionName (in dotted format)
	 * @return true if it is
	 */
	public boolean isSingleton(String exceptionName) {
		if (size != 1) return false;
		ObjectType e = iterator().next();
		return e.toString().equals(exceptionName);
		
	}
	/**
	 * Add an explicit exception.
	 *
	 * @param type type of the exception
	 */
	public void addExplicit(ObjectType type) {
		add(type, true);
	}

	/**
	 * Add an implicit exception.
	 *
	 * @param type type of the exception
	 */
	public void addImplicit(ObjectType type) {
		add(type, false);
	}

	/**
	 * Add an exception.
	 *
	 * @param type     the exception type
	 * @param explicit true if the exception is explicitly declared
	 *                 or thrown, false if implicit
	 */
	public void add(ObjectType type, boolean explicit) {
		int index = factory.getIndexOfType(type);
		if (!exceptionSet.get(index))
			++size;
		exceptionSet.set(index);
		if (explicit)
			explicitSet.set(index);

		commonSupertype = null;
	}

	/**
	 * Add all exceptions in the given set.
	 *
	 * @param other the set
	 */
	public void addAll(ExceptionSet other) {
		exceptionSet.or(other.exceptionSet);
		explicitSet.or(other.explicitSet);
		size = countBits(exceptionSet);

		commonSupertype = null;
	}

	private int countBits(BitSet bitSet) {
		int count = 0;
		for (int i = 0; i < factory.getNumTypes(); ++i) {
			if (bitSet.get(i))
				++count;
		}
		return count;
	}

	/**
	 * Remove all exceptions from the set.
	 */
	public void clear() {
		exceptionSet.clear();
		explicitSet.clear();
		universalHandler = false;
		commonSupertype = null;
	}

	/**
	 * Return whether or not a universal exception handler
	 * was reached by the set.
	 */
	public void sawUniversal() {
		clear();
		universalHandler = true;
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
		for (ThrownExceptionIterator i = iterator(); i.hasNext();) {
			ObjectType type = i.next();
			if (!Hierarchy.isUncheckedException(type))
				return true;
		}
		return false;
	}

	/**
	 * Return whether or not the set contains any explicit exceptions.
	 */
	public boolean containsExplicitExceptions() {
		for (ThrownExceptionIterator i = iterator(); i.hasNext();) {
			i.next();
			if (i.isExplicit())
				return true;
		}
		return false;
	}

	@Override
         public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append('{');
		boolean first = true;
		for (ThrownExceptionIterator i = iterator(); i.hasNext();) {
			ObjectType type = i.next();
			if (first)
				first = false;
			else
				buf.append(',');
			boolean implicit = !i.isExplicit();
			if (implicit) buf.append('[');
			buf.append(type.toString());
			if (implicit) buf.append(']');
		}
		buf.append('}');
		return buf.toString();
	}
}

// vim:ts=4
