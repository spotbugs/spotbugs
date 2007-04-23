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

import org.apache.bcel.generic.ObjectType;

/**
 * An exception thrown from an instruction.
 * These can be implicit (i.e., runtime exceptions and errors),
 * or explicit (athrow, or declared exception from called method).
 * This information is used in TypeAnalysis in order to determine:
 * <ul>
 * <li> what exceptions can be thrown along exception edges, and
 * <li> which exceptions are explicit (declared or explicitly thrown)
 * and which are implicit (result of failed runtime checks)
 * </ul>
 *
 * @author David Hovemeyer
 * @see ExceptionSet
 * @see TypeAnalysis
 */
public class ThrownException {
	private ObjectType type;
	private boolean explicit;

	/**
	 * Constructor.
	 *
	 * @param type     type of exception
	 * @param explicit true if explicit, false if implicit
	 */
	public ThrownException(ObjectType type, boolean explicit) {
		this.type = type;
		this.explicit = explicit;
	}

	/**
	 * Return an identical copy of this object.
	 */
	public ThrownException duplicate() {
		return new ThrownException(type, explicit);
	}

	/**
	 * Get the exception type.
	 */
	public ObjectType getType() {
		return type;
	}

	/**
	 * Return whether or not the exception is explicit.
	 */
	public boolean isExplicit() {
		return explicit;
	}

	/**
	 * Set whether or not the exception is explicit.
	 */
	public void setExplicit(boolean explicit) {
		this.explicit = explicit;
	}

	@Override
		 public int hashCode() {
		return type.hashCode();
	}

	@Override
		 public boolean equals(Object o) {
		if (o == null) return false;
		if (o.getClass() != this.getClass()) return false;

		ThrownException other = (ThrownException) o;
		return this.type.equals(other.type) && this.explicit == other.explicit;
	}
}

// vim:ts=4
