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
 *
 * @see IsNullValueFrame
 * @see IsNullValueAnalysis
 * @author David Hovemeyer
 */
public class IsNullValue {
	private static final int NULL = 0;
	private static final int NN   = 1;
	private static final int NSP  = 2;
	private static final int DNR  = 3;

	private static final int[][] mergeMatrix = {
		// NULL  NN    NSP   DNR
		{  NULL, NSP,  NSP,  NSP  }, // NULL
		{  -1,   NN,   DNR,  DNR  }, // NN
		{  -1,   -1,   DNR,  DNR  }, // NSP
		{  -1,   -1,   -1,   DNR  }, // DNR
	};

	private static IsNullValue[] instanceList = {
		new IsNullValue(NULL),
		new IsNullValue(NN),
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

	/** Merge two values. */
	public static IsNullValue merge(IsNullValue a, IsNullValue b) {
		// Left hand value should be smaller.
		if (a.kind > b.kind) {
			IsNullValue tmp = a;
			a = b;
			b = tmp;
		}

		int result = mergeMatrix[a.kind][b.kind];
		assert result >= 0;
		return instanceList[result];
	}
}

// vim:ts=4
