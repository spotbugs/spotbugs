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
import java.util.HashMap;
import java.util.Map;

/**
 * A dataflow value representing a Java stack frame with value number
 * information.
 *
 * @see ValueNumber
 * @see ValueNumberAnalysis
 * @author David Hovemeyer
 */
public class ValueNumberFrame extends Frame<ValueNumber> {
	private static final boolean REDUNDANT_LOAD_ELIMINATION = Boolean.getBoolean("vna.rle");

	private ValueNumberFactory factory;
	private ArrayList<ValueNumber> mergedValueList;
	private Map<AvailableLoad, ValueNumber> availableLoadMap;

	public ValueNumberFrame(int numLocals, final ValueNumberFactory factory) {
		super(numLocals);
		this.factory = factory;
		if (REDUNDANT_LOAD_ELIMINATION) {
			this.availableLoadMap = new HashMap<AvailableLoad, ValueNumber>();
		}
	}

	/**
	 * Look for an available load.
	 * @param availableLoad the AvailableLoad (reference and field)
	 * @return the ValueNumber available, or null if the is no matching entry
	 */
	public ValueNumber getAvailableLoad(AvailableLoad availableLoad) {
		return availableLoadMap.get(availableLoad);
	}

	/**
	 * Add an available load.
	 * @param availableLoad the AvailableLoad (reference and field)
	 * @param value the ValueNumber loaded
	 */
	public void addAvailableLoad(AvailableLoad availableLoad, ValueNumber value) {
		availableLoadMap.put(availableLoad, value);
	}

	public void mergeWith(Frame<ValueNumber> other_) throws DataflowAnalysisException {
		ValueNumberFrame other = (ValueNumberFrame) other_;

		if (REDUNDANT_LOAD_ELIMINATION) {
			// Merge available load sets.
			// Only loads that are available in both frames
			// remain available. All others are discarded.
			this.availableLoadMap.entrySet().retainAll(other.availableLoadMap.entrySet());
		}

		// Merge slot values.
		super.mergeWith(other);
	}

	public ValueNumber mergeValues(int slot, ValueNumber mine, ValueNumber other) {
		// Merging slot values:
		//   - Merging identical values results in no change
		//   - If the values are different, and the value in the result
		//     frame is not the result of a previous result, a fresh value
		//     is allocated.
		//   - If the value in the result frame is the result of a
		//     previous merge, IT STAYS THE SAME.
		//
		// The "one merge" rule means that merged values are essentially like
		// phi nodes.  They combine some number of other values.

		// I believe that this strategy is correct - slots with the same
		// value number will have identical values at runtime.
		// The lattice has a finite height because the CFGs have a finite
		// maximum length path, which limits the number of times a value
		// merge can propagate through the CFG; so, the analysis terminates.
		// Each merge results in a lowering in the lattice.

		// I need to think about this a bit more before trusting the results
		// of ValueNumberAnalysis.

		if (mine != getValue(slot)) throw new IllegalStateException();

		if (mine.equals(other))
			return mine;

		ValueNumber mergedValue = mergedValueList.get(slot);
		if (mergedValue == null) {
			mergedValue = factory.createFreshValue();
			mergedValueList.set(slot, mergedValue);
		}

		return mergedValue;
	}

	public ValueNumber getDefaultValue() {
		// Default values should never be looked at.
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

		if (REDUNDANT_LOAD_ELIMINATION) {
			// Copy available load set.
			availableLoadMap.clear();
			availableLoadMap.entrySet().addAll(((ValueNumberFrame)other).availableLoadMap.entrySet());
		}

		super.copyFrom(other);
	}
}

// vim:ts=4
