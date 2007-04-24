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

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.ba.Debug;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.XMethodParameter;

/**
 * A class to abstractly represent values in stack slots,
 * indicating whether thoses values can be null, non-null,
 * null on some incoming path, or unknown.
 *
 * @author David Hovemeyer
 * @see IsNullValueFrame
 * @see IsNullValueAnalysis
 */
public class IsNullValue implements IsNullValueAnalysisFeatures, Debug {
	private static final boolean DEBUG_EXCEPTION = SystemProperties.getBoolean("inv.debugException");
	private static final boolean DEBUG_KABOOM = SystemProperties.getBoolean("inv.debugKaboom");

	/** Definitely null. */
	private static final int NULL = 0;
	/** Definitely null because of a comparison to a known null value. */
	private static final int CHECKED_NULL = 1;

	/** Definitely not null. */
	private static final int NN = 2;
	/** Definitely not null because of a comparison to a known null value. */
	private static final int CHECKED_NN = 3;
	/** Definitely not null an NPE would have occurred and we would not be here if it were null. */
	private static final int NO_KABOOM_NN = 4;


	/** Null on some simple path (at most one branch) to current location. */
	private static final int NSP = 5;
	/** Unknown value (method param, value read from heap, etc.), assumed not null. */
	private static final int NN_UNKNOWN = 6;
	/** Null on some complex path (at least two branches) to current location. */
	private static final int NCP2 = 7;
	/** Null on some complex path (at least three branches) to current location. */
	private static final int NCP3 = 8;

	private static final int FLAG_SHIFT = 8;

	/** Value was propagated along an exception path. */
	private static final int EXCEPTION = 1 << FLAG_SHIFT;
	/** Value is (potentially) null because of a parameter passed to the method. */
	private static final int PARAM = 2 << FLAG_SHIFT;
	/** Value is (potentially) null because of a value returned from a called method. */
	private static final int RETURN_VAL = 4 << FLAG_SHIFT;
	private static final int FIELD_VAL = 8 << FLAG_SHIFT;

	private static final int FLAG_MASK = EXCEPTION | PARAM | RETURN_VAL | FIELD_VAL; 

	private static final int[][] mergeMatrix = {
		// NULL, CHECKED_NULL, NN,         CHECKED_NN, NO_KABOOM_NN, NSP,  NN_UNKNOWN, NCP2, NCP3
		{NULL},                                                                      // NULL
		{NULL,   CHECKED_NULL, },                                                    // CHECKED_NULL
		{NSP,    NSP,          NN},                                                  // NN
		{NSP,    NSP,          NN,         CHECKED_NN, },                            // CHECKED_NN
		{NSP,    NSP,          NN,         NN,			 NO_KABOOM_NN},                // NO_KABOOM_NN
		{NSP,    NSP,          NSP,        NSP,		NSP,     NSP},                         // NSP
		{NSP,    NSP,          NN_UNKNOWN, NN_UNKNOWN, NN_UNKNOWN,	NSP,  NN_UNKNOWN, },          // NN_UNKNOWN
		{NSP,   NSP,         NCP2,       NCP2,       NCP2, NCP2, NCP2,        NCP2,},    // NCP2
		{NSP,   NSP,         NCP3,       NCP3,       NCP3, NCP3, NCP3,        NCP3, NCP3}// NCP3
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
					null, // NO_KABOOM_NN values must be allocated dynamically
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
	private final Location locationOfKaBoom;

	private IsNullValue(int kind) {
		this.kind = kind;
		locationOfKaBoom = null;
		if (VERIFY_INTEGRITY) checkNoKaboomNNLocation();
	}

	private IsNullValue(int kind, Location ins) {
		this.kind = kind;
		locationOfKaBoom = ins;
		if (VERIFY_INTEGRITY) checkNoKaboomNNLocation();
	}

	private void checkNoKaboomNNLocation() {
		if (getBaseKind() == NO_KABOOM_NN && locationOfKaBoom == null) {
			throw new IllegalStateException("construction of no-KaBoom NN without Location");
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || this.getClass() != o.getClass())
			return false;
		IsNullValue other = (IsNullValue) o;
		if ( kind != other.kind) return false;
		if (locationOfKaBoom == other.locationOfKaBoom) return true;
		if (locationOfKaBoom == null || other.locationOfKaBoom == null) return false;
		return locationOfKaBoom.equals(other.locationOfKaBoom);
	}

	@Override
	public int hashCode() {
		int hashCode =  kind;
		if (locationOfKaBoom != null)
			hashCode += locationOfKaBoom.hashCode();
		return hashCode;
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
	 * Was this value marked as a possibly null return value?
	 */
	public boolean isReturnValue() {
		return (kind & RETURN_VAL) != 0;
	}

		public boolean isFieldValue() {
			return (kind & FIELD_VAL) != 0;
		}
	/**
	 * Was this value marked as a possibly null parameter?
	 */
	public boolean isParamValue() {
		return (kind & PARAM) != 0;
	}

	/**
	 * Is this value known because of an explicit null check?
	 */
	public boolean isChecked() {
		return getBaseKind() == CHECKED_NULL || getBaseKind() == CHECKED_NN;
	}

	/**
	 * Is this value known to be non null because a NPE would have occurred otherwise?
	 */
	public boolean wouldHaveBeenAKaboom() {
		return getBaseKind() == NO_KABOOM_NN;
	}

	private IsNullValue toBaseValue() {
		return instanceByFlagsList[0][getBaseKind()];
	}

	/**
	 * Convert to an exception path value.
	 */
	public IsNullValue toExceptionValue() {
		if (getBaseKind() == NO_KABOOM_NN) return new IsNullValue(kind | EXCEPTION, locationOfKaBoom);
		return instanceByFlagsList[(getFlags() | EXCEPTION) >> FLAG_SHIFT][getBaseKind()];
	}

	/**
	 * Convert to a value known because it was returned from a method
	 * in a method property database.
	 * @param methodInvoked TODO
	 */
	public IsNullValue markInformationAsComingFromReturnValueOfMethod(XMethod methodInvoked) {
		if (getBaseKind() == NO_KABOOM_NN) return new IsNullValue(kind | RETURN_VAL, locationOfKaBoom);
		return instanceByFlagsList[(getFlags() | RETURN_VAL) >> FLAG_SHIFT][getBaseKind()];
	}
	/**
	 * Convert to a value known because it was returned from a method
	 * in a method property database.
	 * @param methodInvoked TODO
	 */
	public IsNullValue markInformationAsComingFromFieldValue(XField field) {
		if (getBaseKind() == NO_KABOOM_NN) return new IsNullValue(kind | FIELD_VAL, locationOfKaBoom);
		return instanceByFlagsList[(getFlags() | FIELD_VAL) >> FLAG_SHIFT][getBaseKind()];
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
	 * Get the instance representing a value known to be non-null
	 * because a NPE would have occurred if it were null.
	 */
	public static IsNullValue noKaboomNonNullValue(@NonNull Location ins) {
		if (ins == null) 
			throw new NullPointerException("ins cannot be null");
		return new IsNullValue(NO_KABOOM_NN, ins);
	}

	/**
	 * Get the instance representing values that are definitely null
	 * on some simple (no branches) incoming path.
	 */
	public static IsNullValue nullOnSimplePathValue() {
		return instanceByFlagsList[0][NSP];
	}

	/**
	 * Get instance representing a parameter marked as MightBeNull
	 */
	public static IsNullValue parameterMarkedAsMightBeNull(XMethodParameter mp) {
		return instanceByFlagsList[PARAM >> FLAG_SHIFT][NSP];
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
		if (a.equals(b)) return a;
		int aKind = a.kind & 0xff;
		int bKind = b.kind & 0xff;
		int aFlags = a.getFlags();
		int bFlags = b.getFlags();


		int combinedFlags =  aFlags & bFlags;


		if (!(a.isNullOnSomePath() || a.isDefinitelyNull()) && b.isException())
				combinedFlags |= EXCEPTION;
		else
			if (!(b.isNullOnSomePath() || b.isDefinitelyNull()) && a.isException())
				combinedFlags |= EXCEPTION;

		// Left hand value should be >=, since it is used
		// as the first dimension of the matrix to index.
		if (aKind < bKind) {
			int tmp = aKind;
			aKind = bKind;
			bKind = tmp;
		}
		assert aKind >= bKind;
		int result = mergeMatrix[aKind][bKind];

		IsNullValue resultValue = (result == NO_KABOOM_NN)
				? noKaboomNonNullValue(a.locationOfKaBoom)
				: instanceByFlagsList[combinedFlags >> FLAG_SHIFT][result];

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
	 * Is this value null on a complicated path?
	 */
	public boolean isNullOnComplicatedPath() {
		int baseKind = getBaseKind();
		 return baseKind == NN_UNKNOWN || baseKind == NCP2 || baseKind == NCP3;
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
		return baseKind == NN || baseKind == CHECKED_NN || baseKind == NO_KABOOM_NN;
	}

	@Override
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
				if ((flags & FIELD_VAL) != 0) pfx += "f";
			}
		}
		if (DEBUG_KABOOM && locationOfKaBoom == null) {
			pfx += "[?]";
		}
		switch (getBaseKind()) {
		case NULL:
			return pfx + "n" + ",";
		case CHECKED_NULL:
			return pfx + "w" + ",";
		case NN:
			return pfx + "N" + ",";
		case CHECKED_NN:
			return pfx + "W" + ",";
		case NO_KABOOM_NN:
			return pfx + "K" + ",";
		case NSP:
			return pfx + "s" + ",";
		case NN_UNKNOWN:
			return pfx + "-" + ",";
		case NCP2:
			return pfx + "/" + ",";
		default:
			throw new IllegalStateException("unknown kind of IsNullValue: " + kind);
		}
	}
	public Location getLocationOfKaBoom() {
		return locationOfKaBoom;
	}

	/**
	 * Control split: move given value down in the lattice
	 * if it is a conditionally-null value.
	 * 
	 * @return another value (equal or further down in the lattice)
	 */
	public IsNullValue downgradeOnControlSplit() {
		IsNullValue value = this;

		if (NCP_EXTRA_BRANCH) {
			// Experimental: track two distinct kinds of "null on complex path" values.
			if (value.isNullOnSomePath())
				value = nullOnComplexPathValue();
			else if (value.equals(nullOnComplexPathValue()))
				value = nullOnComplexPathValue3();

		} else {
			// Downgrade "null on simple path" values to
			// "null on complex path".
			if (value.isNullOnSomePath())
				value = nullOnComplexPathValue();
		}
		return value;
	}
}

// vim:ts=4
