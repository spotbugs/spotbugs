/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003-2005 University of Maryland
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
 *
 * @author David Hovemeyer
 * @see IsNullValueFrame
 * @see IsNullValueAnalysis
 */
public class IsNullValue implements IsNullValueAnalysisFeatures {
	private static final boolean DEBUG_EXCEPTION = Boolean.getBoolean("inv.debugException");

	/** Definitely null. */
	private static final int NULL = 0;
	/** Definitely null because of a comparison to a known null value. */
	private static final int CHECKED_NULL = 1;
	/** Definitely not null. */
	private static final int NN = 2;
	/** Definitely not null because of a comparison to a known null value. */
	private static final int CHECKED_NN = 3;
	/** Null on some simple path (at most one branch) to current location. */
	private static final int NSP = 4;
	/** Unknown value (method param, value read from heap, etc.), assumed not null. */
	private static final int NN_UNKNOWN = 5;
	/** Null on some complex path (at least two branches) to current location. */
	private static final int NCP2 = 6;
	/** Null on some complex path (at least three branches) to current location. */
	private static final int NCP3 = 7;

	// This can be bitwise-OR'ed to indicate the value was propagated
	// along an exception path.
	private static final int EXCEPTION = 0x100;

	private static final int[][] mergeMatrix = {
		// NULL, CHECKED_NULL, NN,         CHECKED_NN, NSP,  NN_UNKNOWN, NCP2, NCP3
		{NULL},                                                                      // NULL
		{NULL,   CHECKED_NULL, },                                                    // CHECKED_NULL
		{NSP,    NSP,          NN},                                                  // NN
		{NSP,    NSP,          NN,         CHECKED_NN, },                            // CHECKED_NN
		{NSP,    NSP,          NSP,        NSP,        NSP},                         // NSP
		{NSP,    NSP,          NN_UNKNOWN, NN_UNKNOWN, NSP,  NN_UNKNOWN, },          // NN_UNKNOWN
		{NCP2,   NCP2,         NCP2,       NCP2,       NCP2, NCP2,        NCP2,},    // NCP2
		{NCP3,   NCP3,         NCP3,       NCP3,       NCP3, NCP3,        NCP3, NCP3}// NCP3
	};

	private static IsNullValue[] instanceList = {
		new IsNullValue(NULL),
		new IsNullValue(CHECKED_NULL),
		new IsNullValue(NN),
		new IsNullValue(CHECKED_NN),
		new IsNullValue(NSP),
		new IsNullValue(NN_UNKNOWN),
		new IsNullValue(NCP2),
		new IsNullValue(NCP3),
	};

	private static IsNullValue[] exceptionInstanceList = {
		new IsNullValue(NULL | EXCEPTION),
		new IsNullValue(CHECKED_NULL | EXCEPTION),
		new IsNullValue(NN | EXCEPTION),
		new IsNullValue(CHECKED_NN | EXCEPTION),
		new IsNullValue(NSP | EXCEPTION),
		new IsNullValue(NN_UNKNOWN | EXCEPTION),
		new IsNullValue(NCP2 | EXCEPTION),
		new IsNullValue(NCP3 | EXCEPTION),
	};

	private final int kind;

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

	/**
	 * Is this value known because of an explicit null check?
	 */
	public boolean isChecked() {
		return getBaseKind() == CHECKED_NULL || getBaseKind() == CHECKED_NN;
	}

	private IsNullValue toBaseValue() {
		return instanceList[getBaseKind()];
	}

	/**
	 * Convert to an exception path value.
	 */
	public IsNullValue toExceptionValue() {
		return exceptionInstanceList[getBaseKind()];
	}

	/**
	 * Get the instance representing values that are definitely null.
	 */
	public static IsNullValue nullValue() {
		return instanceList[NULL];
	}

	/**
	 * Get the instance representing a value known to be null
	 * because it was compared against null value, or because
	 * we saw that it was assigned the null constant.
	 */
	public static IsNullValue checkedNullValue() {
		return instanceList[CHECKED_NULL];
	}
	/**
	 * Get the instance representing values that are definitely not null.
	 */
	public static IsNullValue nonNullValue() {
		return instanceList[NN];
	}

	/**
	 * Get the instance representing a value known to be non-null
	 * because it was compared against null value, or because
	 * we saw the object creation.
	 */
	public static IsNullValue checkedNonNullValue() {
		return instanceList[CHECKED_NN];
	}

	/**
	 * Get the instance representing values that are definitely null
	 * on some simple (no branches) incoming path.
	 */
	public static IsNullValue nullOnSimplePathValue() {
		return instanceList[NSP];
	}

	/**
	 * Get non-reporting non-null value.
	 * This is what we use for unknown values.
	 */
	public static IsNullValue nonReportingNotNullValue() {
		return instanceList[NN_UNKNOWN];
	}

	/**
	 * Get null on complex path value.
	 * This is like null on simple path value, but there
	 * are at least two branches between the explicit null value
	 * and the current location.  If the conditions are correlated,
	 * then the path on which the value is null may be infeasible.
	 */
	public static IsNullValue nullOnComplexPathValue() {
		return instanceList[NCP2];
	}
	
	/**
	 * Like "null on complex path" except that there are at least
	 * <em>three</em> branches between the explicit null value
	 * and the current location.
	 */
	public static IsNullValue nullOnComplexPathValue3() {
		return instanceList[NCP3];
	}

	/**
	 * Get null value resulting from comparison to explicit null.
	 */
	public static IsNullValue pathSensitiveNullValue() {
		return instanceList[CHECKED_NULL];
	}

	/**
	 * Get non-null value resulting from comparison to explicit null.
	 */
	public static IsNullValue pathSensitiveNonNullValue() {
		return instanceList[CHECKED_NN];
	}

	/**
	 * Merge two values.
	 */
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

	/**
	 * Is this value definitely null?
	 */
	public boolean isDefinitelyNull() {
		int baseKind = getBaseKind();
		return baseKind == NULL || baseKind == CHECKED_NULL;
	}

	/**
	 * Is this value null on some path?
	 */
	public boolean isNullOnSomePath() {
		int baseKind = getBaseKind();
		if (NCP_EXTRA_BRANCH) {
			// Note: NCP_EXTRA_BRANCH is an experimental feature
			// to see how many false warnings we get when we allow
			// two branches between an explicit null and a
			// a dereference.
			return baseKind == NSP || baseKind == NCP2;
		} else {
			return baseKind == NSP;
		}
	}

	/**
	 * Is this value definitely not null?
	 */
	public boolean isDefinitelyNotNull() {
		int baseKind = getBaseKind();
		return baseKind == NN || baseKind == CHECKED_NN;
	}

	public String toString() {
		String pfx = "";
		if (DEBUG_EXCEPTION) {
			pfx = (kind & EXCEPTION) != 0 ? "e" : "_";
		}
		switch (kind & ~EXCEPTION) {
		case NULL:
			return pfx + "n";
		case CHECKED_NULL:
			return pfx + "w";
		case NN:
			return pfx + "N";
		case CHECKED_NN:
			return pfx + "W";
		case NSP:
			return pfx + "s";
		case NN_UNKNOWN:
			return pfx + "-";
		case NCP2:
			return pfx + "/";
		default:
			throw new IllegalStateException("unknown kind of IsNullValue: " + kind);
		}
	}
}

// vim:ts=4
