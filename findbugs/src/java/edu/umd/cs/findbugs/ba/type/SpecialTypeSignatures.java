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

/**
 * Fake type signatures for "special" non-JVM types.
 * These can be used with TypeRepository.typeFromSignature()
 * to get special types.  They all begin with "@",
 * which is not a valid character in Java type signatures
 * as far as I know.  However, no code should depend on
 * the actual string values associated with these
 * constants, except that they are distinct from
 * any valid JVM type signature.
 *
 * @author David Hovemeyer
 * @see TypeRepository
 */
public interface SpecialTypeSignatures {
	/**
	 * Prefix used to identify special types.
	 */
	static final String SPECIAL_TYPE_PREFIX = "@";

	/**
	 * Signature of special "Top" type, which is the identity
	 * element of the type meet operation.
	 */
	public static final String TOP_TYPE_SIGNATURE = "@top";

	/**
	 * Signature of special "Bottom" type, which is the zero
	 * element of the type meet operation.
	 */
	public static final String BOTTOM_TYPE_SIGNATURE = "@bottom";

	/**
	 * Signature of the special type of the null value,
	 * which is an identity element for all reference types.
	 */
	public static final String NULL_TYPE_SIGNATURE = "@null";

	/**
	 * Signature of the "extra" slot occupied by a long value.
	 * BCEL treats longs as taking up two stack slots,
	 * and the JVM treats longs as taking up two local slots.
	 */
	public static final String LONG_EXTRA_TYPE_SIGNATURE = "@longextra";

	/**
	 * Signature of the "extra" slot occupied by a double value.
	 * BCEL treats doubles as taking up two stack slots,
	 * and the JVM treats doubles as taking up two local slots.
	 */
	public static final String DOUBLE_EXTRA_TYPE_SIGNATURE = "@doubleextra";

	/**
	 * Signature of special return address type.
	 */
	public static final String RETURN_ADDRESS_TYPE_SIGNATURE = "@returnaddress";
}

// vim:ts=4
