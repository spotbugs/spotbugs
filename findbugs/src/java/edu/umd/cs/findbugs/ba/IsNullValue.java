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

package edu.umd.cs.findbugs.ba;

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
	private static final boolean NO_WEAK_VALUES = !Boolean.getBoolean("inv.weak");
	private static final boolean DEBUG_EXCEPTION = Boolean.getBoolean("inv.debugException");

	private static final int NULL      = 0;
	private static final int WEAK_NULL = 1;
	private static final int NN        = 2;
	private static final int WEAK_NN   = 3;
	private static final int NSP       = 4;
	private static final int DNR       = 5;

	// This can be bitwise-OR'ed to indicate the value was propagated
	// along an exception path.
	private static final int EXCEPTION = 0x100;

	// TODO: think about this some more
	private static final int[][] weakValueMergeMatrix = {
		// NULL,      WEAK_NULL, NN,        WEAK_NN,   NSP,       DNR
		{  NULL                                                         }, // NULL
		{  WEAK_NULL, WEAK_NULL,                                        }, // WEAK_NULL
		{  NSP,       DNR,       NN                                     }, // NN
		{  NSP,       DNR,       WEAK_NN,   WEAK_NN,                    }, // WEAK_NN
		{  NSP,       NSP,       NSP,       NSP,       NSP              }, // NSP
		{  NSP,       DNR,       DNR,       DNR,       DNR,       DNR   }  // DNR
	};

	private static final int[][] noWeakValueMergeMatrix = {
		{  NULL                                                         }, // NULL
		{  -1,        -1,                                               }, // WEAK_NULL
		{  NSP,       -1,       NN                                      }, // NN
		{  -1,        -1,       -1,        -1,    	                    }, // WEAK_NN
		{  NSP,       -1,       NSP,       -1,         NSP              }, // NSP
		{  NSP,       -1,       DNR,       -1,         DNR,       DNR   }  // DNR
	};

	private static final int[][] mergeMatrix =
		NO_WEAK_VALUES ? noWeakValueMergeMatrix : weakValueMergeMatrix;

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

	public boolean equals(Object o) {
		if (this.getClass() != o.getClass())
			return false;
		IsNullValue other = (IsNullValue) o;
		return kind == other.kind;
	}

	public int hashCode() {
		throw new UnsupportedOperationException();
	}

	private int getBaseKind() {
		return kind & ~EXCEPTION;
	}

	/**
	 * Was this value propagated on an exception path?
	 */
	public boolean isException() {
		return (kind & EXCEPTION) != 0;
	}

	private IsNullValue toBaseValue() {
		if (!isException())
			return this;
		else
			return instanceList[getBaseKind()];
	}

	/**
	 * Convert to an exception path value.
	 */
	public IsNullValue toExceptionValue() {
		if (isException())
			return this;
		else
			return new IsNullValue(kind | EXCEPTION);
	}

	/** Get the instance representing values that are definitely null. */
	public static IsNullValue nullValue() {
		return instanceList[NULL];
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

	public static IsNullValue flowSensitiveNullValue() {
		return instanceList[NO_WEAK_VALUES ? NULL : WEAK_NULL];
	}

	public static IsNullValue flowSensitiveNonNullValue() {
		return instanceList[NO_WEAK_VALUES ? NN : WEAK_NN];
	}

	/** Merge two values. */
	public static IsNullValue merge(IsNullValue a, IsNullValue b) {
		boolean isException = a.isException() || b.isException();
		a = a.toBaseValue();
		b = b.toBaseValue();

		// Left hand value should be >=, since it is used
		// as the first dimension of the matrix to index.
		if (a.kind < b.kind) {
			IsNullValue tmp = a;
			a = b;
			b = tmp;
		}

		int result = mergeMatrix[a.kind][b.kind];
		IsNullValue resultValue = instanceList[result];
		if (isException)
			resultValue = resultValue.toExceptionValue();
		return resultValue;
	}

	/** Is this value definitely null? */
	public boolean isDefinitelyNull() {
		int baseKind = getBaseKind();
		return baseKind == NULL || baseKind == WEAK_NULL;
	}

	/** Is this value null on some path? */
	public boolean isNullOnSomePath() {
		int baseKind = getBaseKind();
		return baseKind == NSP;
	}

	/** Is this value definitely not null? */
	public boolean isDefinitelyNotNull() {
		int baseKind = getBaseKind();
		return baseKind == NN || baseKind == WEAK_NN;
	}

	public String toString() {
		String pfx = "";
		if (DEBUG_EXCEPTION) {
			pfx = (kind & EXCEPTION) != 0 ? "e" : "_";
		}
		switch (kind & ~EXCEPTION) {
		case NULL:
			return pfx + "n";
		case WEAK_NULL:
			return pfx + "w";
		case NN:
			return pfx + "N";
		case WEAK_NN:
			return pfx + "W";
		case NSP:
			return pfx + "s";
		case DNR:
			return pfx + "-";
		default:
			throw new IllegalStateException("unknown kind of IsNullValue: " + kind);
		}
	}
}

// vim:ts=4
