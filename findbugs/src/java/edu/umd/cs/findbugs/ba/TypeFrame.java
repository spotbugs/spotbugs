/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003, University of Maryland
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

package edu.umd.cs.daveho.ba;

import java.util.*;

// We require BCEL 5.0 or later.
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * A specialization of {@link Frame} for determining the types
 * of values in the Java stack frame (locals and operand stack).
 *
 * @see Frame
 * @see FrameTypeAnalysis
 * @author David Hovemeyer
 */
public class TypeFrame extends Frame<Type> implements Constants, ExtendedTypes {

	////////////////////////////////////////////////////////////////////////////////////
	// Helper classes
	////////////////////////////////////////////////////////////////////////////////////

	/** Special "top" type.  */
	private static class Top extends Type {
		public Top() {
			super(T_TOP, "<top>");
		}
		public boolean equals(Object o) { return o == this; }
	}

	/** The single instance of the Top type. */
	private static Top s_topInstance = new Top();

	/** Get the single instance of the "Top" type. */
	public static Top getTopType() { return s_topInstance; }

	/** Special "bottom" type. */
	private static class Bottom extends Type {
		public Bottom() {
			super(T_BOTTOM, "<bottom>");
		}
		public boolean equals(Object o) { return o == this; }
	}

	/** The single instance of the Bottom type. */
	private static Bottom s_bottomInstance = new Bottom();

	/** Get the single instance of the "Bottom" type. */
	public static Type getBottomType() { return s_bottomInstance; }

	/**
	 * Special type used to represent the "extra" part of a long
	 * value.  We say that when a long is stored, local <i>n</i> will
	 * have type long, and local <i>n+1</i> will have this
	 * type.
	 */
	private static class LongExtra extends Type {
		public LongExtra() {
			super(T_LONG_EXTRA, "<long extra>");
		}
		public boolean equals(Object o) { return o == this; }
	}

	/** The single instance of the LongExtra type. */
	private static LongExtra s_longExtraInstance = new LongExtra();

	/** Get the single instance of the "LongExtra" type. */
	public static Type getLongExtraType() { return s_longExtraInstance; }

	/**
	 * Special type used to represent the "extra" part of a double
	 * value.  We say that when a double is stored, local <i>n</i> will
	 * have type double, and local <i>n+1</i> will have this
	 * type.
	 */
	private static class DoubleExtra extends Type {
		public DoubleExtra() {
			super(T_DOUBLE_EXTRA, "<double extra>");
		}
		public boolean equals(Object o) { return o == this; }
	}

	/** The single instance of the DoubleExtra type. */
	private static DoubleExtra s_doubleExtraInstance = new DoubleExtra();

	/** Get the single instance of the "DoubleExtra" type. */
	public static Type getDoubleExtraType() { return s_doubleExtraInstance; }

	/**
	 * Special type representing the null value.
	 * This is a type which is higher in the lattice than any object type,
	 * but lower than the overall Top type.  It represents the type
	 * of the null value, which may logically be merged with any
	 * object type without loss of information.
	 */
	private static class Null extends Type {
		public Null() {
			super(T_NULL, "<null type>");
		}
		public boolean equals(Object o) { return o == this; }
	}

	/** The single instance of the Null type. */
	private static Null s_nullInstance = new Null();

	/** Get the single instance of the "Null" type. */
	public static Type getNullType() { return s_nullInstance; }

	////////////////////////////////////////////////////////////////////////////////////
	// Fields
	////////////////////////////////////////////////////////////////////////////////////

	private RepositoryLookupFailureCallback lookupFailureCallback;

	////////////////////////////////////////////////////////////////////////////////////
	// Methods
	////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Constructor.
	 */
	public TypeFrame(int numLocals, RepositoryLookupFailureCallback lookupFailureCallback) {
		super(numLocals);
		this.lookupFailureCallback = lookupFailureCallback;
	}

	/**
	 * Get default value to put in unintialized slots.
	 */
	public Type getDefaultValue() {
		return getBottomType();
	}

	/**
	 * Merge two types together.  This is somewhat complicated!
	 * @param slot the slot number
	 * @param a a type to be merged
	 * @param b a type to be merged
	 * @return the merged type
	 */
	public Type mergeValues(int slot, Type a, Type b) throws DataflowAnalysisException {
		byte aType = a.getType(), bType = b.getType();

		if (aType == T_TOP)			// Top is the identity element
			return b;
		else if (bType == T_TOP)	// Top is the identity element
			return a;
		else if (aType == T_BOTTOM || bType == T_BOTTOM)	// Bottom meet anything is bottom
			return getBottomType();
		else if (isObjectType(aType) && isObjectType(bType)) {	// Two object types!
			// Handle the Null type, which serves as a special "top"
			// value for object types.
			if (aType == T_NULL)
				return b;
			else if (bType == T_NULL)
				return a;

			// Two concrete object types.
			// According to the JVM spec, 2nd edition, §4.9.2,
			// the result of merging types is the "first common superclass".
			// Interfaces are NOT considered!
			// This will use the Repository to look up classes.
			ReferenceType aRef = (ReferenceType) a;
			ReferenceType bRef = (ReferenceType) b;
			try {
				return aRef.getFirstCommonSuperclass(bRef);
			} catch (ClassNotFoundException e) {
				lookupFailureCallback.reportMissingClass(e);
				throw new DataflowAnalysisException("Repository lookup failure", e);
			}
		} else if (isObjectType(aType) || isObjectType(bType))	// Object meet non-object is bottom
			return getBottomType();
		else if (aType == bType)	// Same non-object type?
			return a;
		else if (isIntegerType(aType) && isIntegerType(bType)) // Two different integer types - use T_INT
			return Type.INT;
		else						// Default - types are incompatible
			return getBottomType();
	}

	/**
	 * Does given typecode refer to an Integer type (other than long)?
	 */
	private static boolean isIntegerType(byte type) {
		return type == T_INT || type == T_BYTE || type == T_BOOLEAN || type == T_CHAR || type == T_SHORT;
	}

	/**
	 * Does the given typecode refer to an Object (reference) type?
	 */
	private static boolean isObjectType(byte type) {
		return type == T_OBJECT || type == T_NULL;
	}

	protected String valueToString(Type value) {
		return value.toString() + ",";
	}

}

// vim:ts=4
