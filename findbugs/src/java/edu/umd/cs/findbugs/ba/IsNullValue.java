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

/**
 * A class to abstractly represent values in stack slots,
 * indicating whether thoses values can be null, non-null,
 * null on some incoming path, or unknown.
 * Null and non-null values produced by flow-sensitivity are treated specially
 * so that they don't flow out of the regions protected by the condition
 * which makes them precise (the WEAK_NULL and WEAK_NN values).
 *
 * @see IsNullValueFrame
 * @see IsNullValueAnalysis
 * @author David Hovemeyer
 */
public class IsNullValue {
	private static final int NULL      = 0;
	private static final int WEAK_NULL = 1;
	private static final int NN        = 2;
	private static final int WEAK_NN   = 3;
	private static final int NSP       = 4;
	private static final int DNR       = 5;

	// TODO: think about this some more
	private static final int[][] mergeMatrix = {
		// NULL,      WEAK_NULL, NN,        WEAK_NN,   NSP,       DNR
		{  NULL                                                         }, // NULL
		{  WEAK_NULL, WEAK_NULL,                                        }, // WEAK_NULL
		{  NSP,       NN,        NN                                     }, // NN
		{  NSP,       DNR,       WEAK_NN,   WEAK_NN,                    }, // WEAK_NN
		{  NSP,       DNR,       DNR,       DNR,       DNR              }, // NSP
		{  NSP,       DNR,       DNR,       DNR,       DNR,       DNR   }  // DNR
	};

	private static IsNullValue[] instanceList = {
		new IsNullValue(NULL),
		new IsNullValue(WEAK_NULL),
		new IsNullValue(NN),
		new IsNullValue(WEAK_NN),
		new IsNullValue(NSP),
		new IsNullValue(DNR)
	};

	private int kind;

	private IsNullValue(int kind) {
		this.kind = kind;
	}

	/** Get the instance representing values that are definitely null. */
	public static IsNullValue nullValue() {
		return instanceList[NULL];
	}

	/** Get the instance representing values which we know are null by virtual
	    of having been made more precise by an IFNULL or IFNONNULL instruction. */
	public static IsNullValue weakNullValue() {
		return instanceList[WEAK_NULL];
	}

	/** Get the instance representing values that are definitely not null. */
	public static IsNullValue nonNullValue() {
		return instanceList[NN];
	}

	/**
	 * Get the instance representing values that are definitely null
	 * on some incoming path.
	 */
	public static IsNullValue nullOnSomePathValue() {
		return instanceList[NSP];
	}

	/**
	 * Get the instance representing values which we aren't sure 
	 * can be null or not.
	 */
	public static IsNullValue doNotReportValue() {
		return instanceList[DNR];
	}

	public static IsNullValue flowSensitiveNullValue(IsNullValue conditionValue) {
		return conditionValue.kind == NULL ? instanceList[NULL] : instanceList[WEAK_NULL];
	}

	public static IsNullValue flowSensitiveNonNullValue(IsNullValue conditionValue) {
		return conditionValue.kind == NN ? instanceList[NN] : instanceList[WEAK_NN];
	}

	/** Merge two values. */
	public static IsNullValue merge(IsNullValue a, IsNullValue b) {
		// Left hand value should be >=, since it is used
		// as the first dimension of the matrix to index.
		if (a.kind < b.kind) {
			IsNullValue tmp = a;
			a = b;
			b = tmp;
		}

		int result = mergeMatrix[a.kind][b.kind];
		return instanceList[result];
	}

	/** Is this value definitely null? */
	public boolean isDefinitelyNull() {
		return kind == NULL || kind == WEAK_NULL;
	}

	/** Is this value null on some path? */
	public boolean isNullOnSomePath() {
		return kind == NSP;
	}

	public String toString() {
		switch (kind) {
		case NULL:
			return "n";
		case WEAK_NULL:
			return "w";
		case NN:
			return "N";
		case WEAK_NN:
			return "W";
		case NSP:
			return "S";
		case DNR:
			return "-";
		default:
			throw new IllegalStateException("unknown kind of IsNullValue: " + kind);
		}
	}
}

// vim:ts=4
