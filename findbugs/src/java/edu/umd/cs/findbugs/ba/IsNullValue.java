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

/**
 * A class to abstractly represent values in stack slots,
 * indicating whether thoses values can be null, non-null,
 * null on some incoming path, or unknown.
 *
 * @author David Hovemeyer
 * @see IsNullValueFrame
 * @see IsNullValueAnalysis
 */
public class IsNullValue {
	private static final boolean DEBUG_EXCEPTION = Boolean.getBoolean("inv.debugException");

	private static final int NULL = 0;
	private static final int CHECKED_NULL = 1;
	private static final int NN = 2;
	private static final int CHECKED_NN = 3;
	private static final int NSP = 4;
	private static final int NN_DNR = 5;
	private static final int NSP_DNR = 6;

	// This can be bitwise-OR'ed to indicate the value was propagated
	// along an exception path.
	private static final int EXCEPTION = 0x100;

	private static final int[][] mergeMatrix = {
		// NULL,    CHECKED_NULL, NN,      CHECKED_NN, NSP,     NN_DNR,   NSP_DNR
		{NULL}, // NULL
		{NULL, CHECKED_NULL, }, // CHECKED_NULL
		{NSP, NSP, NN}, // NN
		{NSP, NSP, NN, CHECKED_NN, }, // CHECKED_NN
		{NSP, NSP, NSP, NSP, NSP}, // NSP
		{NSP, NSP, NN_DNR, NN_DNR, NSP, NN_DNR, }, // NN_DNR
		{NSP_DNR, NSP_DNR, NSP_DNR, NSP_DNR, NSP_DNR, NSP_DNR, NSP_DNR, }  // NSP_DNR
	};

	private static IsNullValue[] instanceList = {
		new IsNullValue(NULL),
		new IsNullValue(CHECKED_NULL),
		new IsNullValue(NN),
		new IsNullValue(CHECKED_NN),
		new IsNullValue(NSP),
		new IsNullValue(NN_DNR),
		new IsNullValue(NSP_DNR)
	};

	private static IsNullValue[] exceptionInstanceList = {
		new IsNullValue(NULL | EXCEPTION),
		new IsNullValue(CHECKED_NULL | EXCEPTION),
		new IsNullValue(NN | EXCEPTION),
		new IsNullValue(CHECKED_NN | EXCEPTION),
		new IsNullValue(NSP | EXCEPTION),
		new IsNullValue(NN_DNR | EXCEPTION),
		new IsNullValue(NSP_DNR | EXCEPTION)
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
	 * on some incoming path.
	 */
	public static IsNullValue nullOnSomePathValue() {
		return instanceList[NSP];
	}

	/**
	 * Get non-reporting non-null value.
	 * This is what we use for unknown values.
	 */
	public static IsNullValue nonReportingNotNullValue() {
		return instanceList[NN_DNR];
	}

	/**
	 * Get non-reporting null on some path value.
	 */
	public static IsNullValue nonReportingNullOnSomePathValue() {
		return instanceList[NSP_DNR];
	}

	public static IsNullValue flowSensitiveNullValue() {
		return instanceList[CHECKED_NULL];
	}

	public static IsNullValue flowSensitiveNonNullValue() {
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
		return baseKind == NSP;
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
		case NN_DNR:
			return pfx + "-";
		case NSP_DNR:
			return pfx + "/";
		default:
			throw new IllegalStateException("unknown kind of IsNullValue: " + kind);
		}
	}
}

// vim:ts=4
