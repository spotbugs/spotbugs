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
 * A dataflow object representing a single Java stack frame,
 * where each stack slot tracks whether or not it contains
 * the "this" reference.
 *
 * @see ThisValue
 * @see ThisValueAnalysis
 * @author David Hovemeyer
 */
public class ThisValueFrame extends Frame<ThisValue> {

	/**
	 * Constructor.
	 * @param numLocals number of locals in the frame
	 */
	public ThisValueFrame(int numLocals) {
		super(numLocals);
	}

	/**
	 * Merge two slot values.
	 * @param a a slot value
	 * @param b another slot value
	 * @return the merged value
	 */
	public ThisValue mergeValues(ThisValue a, ThisValue b) {
		if (a.isTop())
			return b;
		else if (b.isTop())
			return a;
		else if (a.isBottom() || b.isBottom())
			return ThisValue.bottom();
		else if (a.equals(b))
			return a;
		else
			return ThisValue.bottom();
	}

	/**
	 * Return the default value to be placed in uninitialized slots.
	 */
	public ThisValue getDefaultValue() {
		return ThisValue.top();
	}
}

// vim:ts=4
