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

package edu.umd.cs.findbugs.ba;

import org.apache.bcel.generic.Type;

/**
 * A specialization of {@link Frame} for determining the types
 * of values in the Java stack frame (locals and operand stack).
 *
 * @author David Hovemeyer
 * @see Frame
 * @see TypeAnalysis
 */
public class TypeFrame extends Frame<Type> {
	/**
	 * Constructor.
	 */
	public TypeFrame(int numLocals) {
		super(numLocals);
	}

	protected String valueToString(Type value) {
		return value.toString() + ",";
	}

	/**
	 * Get the single instance of the "Top" type.
	 */
	public static Type getTopType() {
		return TopType.instance();
	}

	/**
	 * Get the single instance of the "Bottom" type.
	 */
	public static Type getBottomType() {
		return BottomType.instance();
	}

	/**
	 * Get the single instance of the "LongExtra" type.
	 */
	public static Type getLongExtraType() {
		return LongExtraType.instance();
	}

	/**
	 * Get the single instance of the "DoubleExtra" type.
	 */
	public static Type getDoubleExtraType() {
		return DoubleExtraType.instance();
	}

	/**
	 * Get the single instance of the "Null" type.
	 */
	public static Type getNullType() {
		return NullType.instance();
	}

}

// vim:ts=4
