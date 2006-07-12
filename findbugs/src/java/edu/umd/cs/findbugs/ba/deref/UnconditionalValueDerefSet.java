/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

package edu.umd.cs.findbugs.ba.deref;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFactory;

/**
 * A set of values unconditionally dereferenced in the future.
 * 
 * @author David Hovemeyer
 */
public class UnconditionalValueDerefSet {
	/** Number of distinct value numbers in method */
	private int numValueNumbersInMethod;
	
	/** Set of value numbers unconditionally dereferenced */
	private BitSet valueNumberSet;
	
	/** Map of value numbers to locations */
	private Map<ValueNumber, BitSet> derefOffsetMap;
	
	/**
	 * Constructor.
	 * 
	 * @param numValueNumbersInMethod number of distinct value numbers in method
	 */
	public UnconditionalValueDerefSet(int numValueNumbersInMethod) {
		this.numValueNumbersInMethod = numValueNumbersInMethod;
		this.valueNumberSet = new BitSet();
		this.derefOffsetMap = new HashMap<ValueNumber, BitSet>();
	}

	public boolean isBottom() {
		return valueNumberSet.get(numValueNumbersInMethod);
	}
	
	public void setIsBottom() {
		clear();
		valueNumberSet.set(numValueNumbersInMethod);
	}

	public boolean isTop() {
		return valueNumberSet.get(numValueNumbersInMethod + 1);
	}
	
	public void setIsTop() {
		clear();
		valueNumberSet.set(numValueNumbersInMethod + 1);
	}

	/**
	 * Clear the deref set.
	 * This sets the fact so it is valid as the dataflow entry fact:
	 * no future dereferences are guaranteed.
	 */
	void clear() {
		valueNumberSet.clear();
		derefOffsetMap.clear();
	}

	/**
	 * Make this dataflow fact the same as the given one.
	 * 
	 * @param source another dataflow fact
	 */
	public void makeSameAs(UnconditionalValueDerefSet source) {
		// Copy value numbers
		valueNumberSet.clear();
		valueNumberSet.or(source.valueNumberSet);

		// Copy dereference locations for each value number
		derefOffsetMap.clear();
		for (Map.Entry<ValueNumber, BitSet> entry : derefOffsetMap.entrySet()) {
			BitSet derefLocationSet = new BitSet();
			derefLocationSet.or(entry.getValue());
			derefOffsetMap.put(entry.getKey(), derefLocationSet);
		}
	}

	/**
	 * Return whether or not this dataflow fact is identical
	 * to the one given.
	 * 
	 * @param otherFact another dataflow fact
	 * @return true if the other dataflow fact is identical to this one,
	 *          false otherwise
	 */
	public boolean isSameAs(UnconditionalValueDerefSet otherFact) {
		return valueNumberSet.equals(otherFact.valueNumberSet)
			&& derefOffsetMap.equals(otherFact.derefOffsetMap);
	}

	/**
	 * Merge given dataflow fact into this one.
	 * We take the intersection of the unconditional deref value number set,
	 * and union the deref locations.
	 * 
	 * @param fact another dataflow fact
	 */
	public void mergeWith(UnconditionalValueDerefSet fact, ValueNumberFactory valueNumberFactory) {
		valueNumberSet.and(fact.valueNumberSet);
		
		for (int i = 0; i < numValueNumbersInMethod; i++) {
			ValueNumber vn = valueNumberFactory.forNumber(i);

			if (valueNumberSet.get(i)) {
				BitSet derefLocationSet = derefOffsetMap.get(vn);
				derefLocationSet.or(fact.derefOffsetMap.get(vn));
			} else {
				// The value number is not in the fact:
				// remove its location set
				derefOffsetMap.remove(vn);
			}
		}
	}
}
