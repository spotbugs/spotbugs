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

import java.util.ArrayList;

/**
 * A dataflow value representing a Java stack frame with value number
 * information.
 *
 * @see ValueNumber
 * @see ValueNumberAnalysis
 * @author David Hovemeyer
 */
public class ValueNumberFrame extends Frame<ValueNumber> {

	private ValueNumberFactory factory;
	private ArrayList<ValueNumber> mergedValueList;

	public ValueNumberFrame(int numLocals, final ValueNumberFactory factory) {
		super(numLocals);
		this.factory = factory;
	}

	public ValueNumber mergeValues(ValueNumber a, ValueNumber b) {
		// This method is not needed for ValueNumberFrame.
		throw new IllegalStateException("mergeValues called on a ValueNumberFrame");
	}

	public ValueNumber getDefaultValue() {
		//return factory.topValue();
		return null;
	}

	public void copyFrom(Frame<ValueNumber> other) {
		// If merged value list hasn't been created yet, create it.
		if (mergedValueList == null) {
			// This is where this frame gets its size.
			// It will have the same size as long as it remains valid.
			mergedValueList = new ArrayList<ValueNumber>();
			int numSlots = other.getNumSlots();
			for (int i = 0; i < numSlots; ++i)
				mergedValueList.add(null);
		}

		super.copyFrom(other);
	}

	public void setMergedValue(int i, ValueNumber valueNumber) {
		mergedValueList.set(i, valueNumber);
	}

	public ValueNumber getMergedValue(int i) {
		return mergedValueList.get(i);
	}
}

// vim:ts=4
