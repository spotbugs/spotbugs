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
 * Lock counts for values (as produced by ValueNumberAnalysis).
 * A LockSet tells us the lock counts for all values in a method,
 * insofar as we can accurately determine them.
 *
 * @see ValueNumberAnalysis
 * @author David Hovemeyer
 */
public class LockSet {
	/** Special TOP value, meaning this is an uninitialized lock count. */
	public static final int TOP = -1;

	/** Special BOTTOM value, meaning that we can't accurately determine the lock count. */
	public static final int BOTTOM = -2;

	private int[] lockCountByValueList;

	/**
	 * Constructor.
	 * @param numValues number of ValueNumbers that will be tracked
	 */
	public LockSet(int numValues) {
		lockCountByValueList = new int[numValues];
	}

	/**
	 * Fill all lock counts with the top value.
	 */
	public void makeTop() {
		setAll(TOP);
	}

	/**
	 * Fill all lock counts with zero.
	 */
	public void makeZero() {
		setAll(0);
	}

	private void setAll(int value) {
		for (int i = 0; i < lockCountByValueList.length; ++i)
			lockCountByValueList[i] = value;
	}

	/**
	 * Get the lock count for given value number.
	 * A non-negative value is (hopefully) an accurate count.
	 * A TOP value is uninitialized - once the analysis completes,
	 * you should not see any TOP values.  A BOTTOM value means
	 * that we don't have an accurate count; in addition, the
	 * existence of a BOTTOM value means that any code reachable
	 * from the point where the BOTTOM value exists will not
	 * have trustworthy lock counts.
	 *
	 * <p> I have reason to believe that the combination of ValueNumberAnalysis
	 * and LockSetAnalysis will not produce any BOTTOM values for properly-formed
	 * Java code.  We'll see how this works out in practice.
	 *
	 * @param valueNum the value number we want the count for
	 * @return the lock count for the value
	 */
	public int getCount(int valueNum) {
		return lockCountByValueList[valueNum];
	}

	/**
	 * Set lock count for given value.
	 * @param valueNum the value
	 * @param count the lock count
	 */
	public void setCount(int valueNum, int count) {
		lockCountByValueList[valueNum] = count;
	}

	/**
	 * Make this LockSet the same as the one given.
	 * @param other the other LockSet
	 */
	public void copyFrom(LockSet other) {
		for (int i = 0; i < lockCountByValueList.length; ++i)
			lockCountByValueList[i] = other.lockCountByValueList[i];
	}

	/**
	 * Determine if this LockSet is the same as the one given.
	 * @param other the other LockSet
	 * @return true if the LockSets are the same, false otherwise
	 */
	public boolean sameAs(LockSet other) {
		assert lockCountByValueList.length == other.lockCountByValueList.length;

		for (int i = 0; i < lockCountByValueList.length; ++i) {
			if (lockCountByValueList[i] != other.lockCountByValueList[i])
				return false;
		}

		return true;
	}

	/**
	 * Merge this LockSet with another.
	 * @param other the other LockSet
	 */
	public void mergeWith(LockSet other) {
		assert lockCountByValueList.length == other.lockCountByValueList.length;

		for (int i = 0; i < lockCountByValueList.length; ++i) {
			int mine = lockCountByValueList[i];
			int his = other.lockCountByValueList[i];

			int result;
			if (mine == TOP)
				result = his;
			else if (his == TOP)
				result = mine;
			else if (mine == BOTTOM || his == BOTTOM)
				result = BOTTOM;
			else if (mine != his)
				result = BOTTOM;
			else
				result = mine;

			lockCountByValueList[i] = result;
		}
	}
}

// vim:ts=4
