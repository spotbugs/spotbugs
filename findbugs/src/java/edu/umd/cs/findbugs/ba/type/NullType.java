/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003,2004 University of Maryland
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

import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

/**
 * Special type representing the null value.
 * This is a type which is higher in the lattice than any object type,
 * but lower than the overall Top type.  It represents the type
 * of the null value, which may logically be merged with any
 * object type without loss of information.
 *
 * @author David Hovemeyer
 * @see TypeAnalysis
 * @see TypeFrame
 * @see TypeMerger
 */
public class NullType extends Type implements ExtendedTypes {

	private static final long serialVersionUID = 1L;
	private static final Type theInstance = new NullType();

	private NullType() {
		super(T_NULL, "<null type>");
	}

	@Override
         public int hashCode() {
		return System.identityHashCode(this);
	}

	@Override
         public boolean equals(Object o) {
		return o == this;
	}

	public static Type instance() {
		return theInstance;
	}
}

// vim:ts=4
