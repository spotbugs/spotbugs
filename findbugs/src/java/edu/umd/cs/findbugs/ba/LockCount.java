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
 * Dataflow value for representing the number of locks held.
 *
 * @author David Hovemeyer
 * @see LockCountAnalysis
 */
public class LockCount {
	private int count;

	/**
	 * Top value.
	 */
	public static final int TOP = -1;

	/**
	 * Bottom value.
	 */
	public static final int BOTTOM = -2;

	/**
	 * Constructor.
	 *
	 * @param count the lock count, or the special TOP or BOTTOM values
	 */
	public LockCount(int count) {
		this.count = count;
	}

	/**
	 * Get the lock count.
	 */
	public int getCount() {
		return count;
	}

	/**
	 * Set the lock count.
	 *
	 * @param count the lock count
	 */
	public void setCount(int count) {
		this.count = count;
	}

	/**
	 * Is this the top value?
	 */
	public boolean isTop() {
		return count == TOP;
	}

	/**
	 * Is this the bottom value?
	 */
	public boolean isBottom() {
		return count == BOTTOM;
	}

	/**
	 * Convert to string.
	 */
	public String toString() {
		if (isTop())
			return "(TOP)";
		else if (isBottom())
			return "(BOTTOM)";
		else
			return "(" + count + ")";
	}
}

// vim:ts=4
