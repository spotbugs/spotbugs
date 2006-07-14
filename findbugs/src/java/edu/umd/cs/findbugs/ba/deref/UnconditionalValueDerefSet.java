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
	private Map<ValueNumber, BitSet> derefLocationSetMap;
	
	/**
	 * Constructor.
	 * 
	 * @param numValueNumbersInMethod number of distinct value numbers in method
	 */
	public UnconditionalValueDerefSet(int numValueNumbersInMethod) {
		this.numValueNumbersInMethod = numValueNumbersInMethod;
		this.valueNumberSet = new BitSet();
		this.derefLocationSetMap = new HashMap<ValueNumber, BitSet>();
	}

	/**
	 * Is this the bottom value?
	 * 
	 * @return true if this is the bottom value, false otherwise
	 */
	public boolean isBottom() {
		return valueNumberSet.get(numValueNumbersInMethod);
	}
	
	/**
	 * Make this dataflow fact the bottom value.
	 */
	public void setIsBottom() {
		clear();
		valueNumberSet.set(numValueNumbersInMethod);
	}

	/**
	 * Is this the top value?
	 * 
	 * @return true if this is the top value, false otherwise
	 */
	public boolean isTop() {
		return valueNumberSet.get(numValueNumbersInMethod + 1);
	}
	
	/**
	 * Make this dataflow fact the top value.
	 */
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
		derefLocationSetMap.clear();
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
		derefLocationSetMap.clear();
		for (Map.Entry<ValueNumber, BitSet> sourceEntry : source.derefLocationSetMap.entrySet()) {
			BitSet derefLocationSet = new BitSet();
			derefLocationSet.or(sourceEntry.getValue());
			derefLocationSetMap.put(sourceEntry.getKey(), derefLocationSet);
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
			&& derefLocationSetMap.equals(otherFact.derefLocationSetMap);
	}

	/**
	 * Merge given dataflow fact into this one.
	 * We take the intersection of the unconditional deref value number set,
	 * and union the deref locations.
	 * 
	 * @param fact another dataflow fact
	 */
	public void mergeWith(UnconditionalValueDerefSet fact, ValueNumberFactory valueNumberFactory) {
		// Compute the intersection of the unconditionally dereferenced value sets
		valueNumberSet.and(fact.valueNumberSet);

		// For each unconditionally dereferenced value...
		for (int i = 0; i < numValueNumbersInMethod; i++) {
			ValueNumber vn = valueNumberFactory.forNumber(i);

			if (valueNumberSet.get(i)) {
				// Compute the union of the dereference locations for
				// this value number.
				BitSet derefLocationSet = derefLocationSetMap.get(vn);
				derefLocationSet.or(fact.derefLocationSetMap.get(vn));
			} else {
				// The value number is not in the fact:
				// remove its location set
				derefLocationSetMap.remove(vn);
			}
		}
	}

	/**
	 * Mark a value as being dereferenced at given bytecode offset.
	 * 
	 * @param vn       the value
	 * @param offset the bytecode offset
	 */
	public void addDeref(ValueNumber vn, int offset) {
		valueNumberSet.set(vn.getNumber());
		
		BitSet derefLocationSet = derefLocationSetMap.get(vn);
		if (derefLocationSet == null) {
			derefLocationSet  = new BitSet();
			derefLocationSetMap.put(vn, derefLocationSet);
		}
		derefLocationSet.set(offset);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer buf = new StringBuffer();
		buf.append('[');
		boolean firstVN = true;
		for (int i = 0; i < numValueNumbersInMethod; i++)  {
			if (!valueNumberSet.get(i)) {
				continue;
			}
			if (firstVN) {
				firstVN = false;
			} else {
				buf.append(',');
			}
			buf.append('{');
			buf.append(i);
			buf.append(':');
			BitSet derefLocationSet = getDerefLocationSet(i);
			boolean firstLoc = true;
			for (int j = 0; j < 65536; j++) {
				if (derefLocationSet.get(j)) {
					if (firstLoc) {
						firstLoc = false;
					} else {
						buf.append(',');
					}
					buf.append(j);
				}
			}
			buf.append('}');
		}
		buf.append(']');
		return buf.toString();
	}

	private BitSet getDerefLocationSet(int vn) {
		for (Map.Entry<ValueNumber, BitSet> entry : derefLocationSetMap.entrySet()) {
			if (entry.getKey().getNumber() == vn) {
				return entry.getValue();
			}
		}
		return new BitSet();
	}
}
