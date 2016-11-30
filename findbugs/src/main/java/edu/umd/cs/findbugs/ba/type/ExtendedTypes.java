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

/**
 * Extended type codes used by StackAndLocalTypes and StackAndLocalTypeAnalysis
 * for typing locals and stack values used in Java bytecode.
 *
 * @author David Hovemeyer
 * @see TypeFrame
 * @see TypeAnalysis
 */
public interface ExtendedTypes {
    // FIXME: these values depend on those in org.apache.bcel.Constants.
    // They need to be distinct from all type codes defined there.
    // It would be nice if BCEL provided built-in functionality for
    // some or all of these.

    /**
     * Special type code for the "Top" type in the lattice.
     */
    public static final byte T_TOP = 17;

    /**
     * Special type that represents the value store in local <i>n+1</i> when a
     * long value is stored in local <i>n</i>.
     */
    public static final byte T_LONG_EXTRA = 18;

    /**
     * Special type that represents the value store in local <i>n+1</i> when a
     * double value is stored in local <i>n</i>.
     */
    public static final byte T_DOUBLE_EXTRA = 19;

    /**
     * Special type code for the "Bottom" type in the lattice.
     */
    public static final byte T_BOTTOM = 20;

    /**
     * Special type code for the "Null" type. This is a type which is higher in
     * the lattice than any object type, but lower than the overall Top type. It
     * represents the type of the null value, which may logically be merged with
     * any object type without loss of information.
     */
    public static final byte T_NULL = 21;

    /**
     * Special type code for "Exception" object type, which is an instance of
     * the ExceptionObjectType class. These instances maintain an ExceptionSet
     * to keep track of possible exceptions that are feasible, and whether those
     * exceptions are explicit or implicit.
     */
    public static final byte T_EXCEPTION = 22;

    /**
     * A Type code that is available for "user-defined" types. Any type code
     * equal or greated than this one is guaranteed to be distinct from both
     * standard and extended types.
     */
    public static final byte T_AVAIL_TYPE = 100;
}

