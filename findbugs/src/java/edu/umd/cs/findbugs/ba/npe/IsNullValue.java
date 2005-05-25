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

package edu.umd.cs.findbugs.ba.npe;

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

	private static final int FLAG_SHIFT = 8;
	
	/** Value was propagated along an exception path. */
	private static final int EXCEPTION = 1 << FLAG_SHIFT;
	/** Value is (potentially) null because of a parameter passed to the method. */
	private static final int PARAM = 2 << FLAG_SHIFT;
	/** Value is (potentially) null because of a value returned from a called method. */
	private static final int RETURN_VAL = 4 << FLAG_SHIFT;

	private static final int FLAG_MASK = EXCEPTION | PARAM | RETURN_VAL; 

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
	
	private static final IsNullValue[][] instanceByFlagsList = createInstanceByFlagList();

	private static IsNullValue[][] createInstanceByFlagList() {
		final int max = FLAG_MASK >>> FLAG_SHIFT;
		IsNullValue[][] result = new IsNullValue[max + 1][];
		for (int i = 0; i <= max; ++i) {
			final int flags = i << FLAG_SHIFT;
			result[i] = new IsNullValue[]{
					new IsNullValue(NULL | flags),
					new IsNullValue(CHECKED_NULL | flags),
					new IsNullValue(NN | flags),
					new IsNullValue(CHECKED_NN | flags),
					new IsNullValue(NSP | flags),
					new IsNullValue(NN_UNKNOWN | flags),
					new IsNullValue(NCP2 | flags),
					new IsNullValue(NCP3 | flags),
			};
		}
		
		return result;
	}

	// Fields
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
		return kind;
	}

	private int getBaseKind() {
		return kind & ~FLAG_MASK;
	}
	
	private int getFlags() {
		return kind & FLAG_MASK;
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
		return instanceByFlagsList[0][getBaseKind()];
	}

	/**
	 * Convert to an exception path value.
	 */
	public IsNullValue toExceptionValue() {
		return instanceByFlagsList[(getFlags() | EXCEPTION) >> FLAG_SHIFT][getBaseKind()];
	}
	
	/**
	 * Convert to a null return value value.
	 */
	public IsNullValue toMayReturnNullValue() {
		return instanceByFlagsList[(getFlags() | RETURN_VAL) >> FLAG_SHIFT][getBaseKind()];
	}

	/**
	 * Get the instance representing values that are definitely null.
	 */
	public static IsNullValue nullValue() {
		return instanceByFlagsList[0][NULL];
	}

	/**
	 * Get the instance representing a value known to be null
	 * because it was compared against null value, or because
	 * we saw that it was assigned the null constant.
	 */
	public static IsNullValue checkedNullValue() {
		return instanceByFlagsList[0][CHECKED_NULL];
	}
	/**
	 * Get the instance representing values that are definitely not null.
	 */
	public static IsNullValue nonNullValue() {
		return instanceByFlagsList[0][NN];
	}

	/**
	 * Get the instance representing a value known to be non-null
	 * because it was compared against null value, or because
	 * we saw the object creation.
	 */
	public static IsNullValue checkedNonNullValue() {
		return instanceByFlagsList[0][CHECKED_NN];
	}

	/**
	 * Get the instance representing values that are definitely null
	 * on some simple (no branches) incoming path.
	 */
	public static IsNullValue nullOnSimplePathValue() {
		return instanceByFlagsList[0][NSP];
	}

	/**
	 * Get non-reporting non-null value.
	 * This is what we use for unknown values.
	 */
	public static IsNullValue nonReportingNotNullValue() {
		return instanceByFlagsList[0][NN_UNKNOWN];
	}

	/**
	 * Get null on complex path value.
	 * This is like null on simple path value, but there
	 * are at least two branches between the explicit null value
	 * and the current location.  If the conditions are correlated,
	 * then the path on which the value is null may be infeasible.
	 */
	public static IsNullValue nullOnComplexPathValue() {
		return instanceByFlagsList[0][NCP2];
	}
	
	/**
	 * Like "null on complex path" except that there are at least
	 * <em>three</em> branches between the explicit null value
	 * and the current location.
	 */
	public static IsNullValue nullOnComplexPathValue3() {
		return instanceByFlagsList[0][NCP3];
	}

	/**
	 * Get null value resulting from comparison to explicit null.
	 */
	public static IsNullValue pathSensitiveNullValue() {
		return instanceByFlagsList[0][CHECKED_NULL];
	}

	/**
	 * Get non-null value resulting from comparison to explicit null.
	 */
	public static IsNullValue pathSensitiveNonNullValue() {
		return instanceByFlagsList[0][CHECKED_NN];
	}

	/**
	 * Merge two values.
	 */
	public static IsNullValue merge(IsNullValue a, IsNullValue b) {
		if (a == b) return a;
		int combinedFlags = a.getFlags() | b.getFlags(); // FIXME: union appropriate for all flags?
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
		IsNullValue resultValue = instanceByFlagsList[combinedFlags >> FLAG_SHIFT][result];
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
	 * Return true if this value is either definitely null,
	 * or might be null on a simple path. 
	 * 
	 * @return true if this value is either definitely null,
	 *         or might be null on a simple path, false otherwise
	 */
	public boolean mightBeNull() {
		return isDefinitelyNull() || isNullOnSomePath();
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
			int flags = getFlags();
			if (flags == 0)
				pfx = "_";
			else {
				if ((flags & EXCEPTION) != 0) pfx += "e";
				if ((flags & PARAM) != 0) pfx += "p";
				if ((flags & RETURN_VAL) != 0) pfx += "r";
			}
		}
		switch (getBaseKind()) {
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
