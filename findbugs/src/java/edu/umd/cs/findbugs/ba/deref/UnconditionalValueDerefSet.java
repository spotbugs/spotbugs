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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import edu.umd.cs.findbugs.TigerSubstitutes;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFactory;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

/**
 * A set of values unconditionally dereferenced in the future.
 * 
 * @author David Hovemeyer
 */
public class UnconditionalValueDerefSet {
	/** Number of distinct value numbers in method */
	private int numValueNumbersInMethod;

	/** Set of value numbers unconditionally dereferenced */
	private BitSet valueNumbersUnconditionallyDereferenced;


	/** Map of value numbers to locations */
	private Map<ValueNumber, Set<Location>> derefLocationSetMap;

	boolean resultsFromBackEdge = false;
	int backEdgeUpdateCount = 0;
	private int lastUpdateTimestamp;



	/**
	 * Constructor.
	 * 
	 * @param numValueNumbersInMethod number of distinct value numbers in method
	 */
	public UnconditionalValueDerefSet(int numValueNumbersInMethod) {
		this.numValueNumbersInMethod = numValueNumbersInMethod;
		this.valueNumbersUnconditionallyDereferenced = new BitSet();
	   this.derefLocationSetMap = new HashMap<ValueNumber, Set<Location>>();

	}

	/**
	 * Is this the bottom value?
	 * 
	 * @return true if this is the bottom value, false otherwise
	 */
	public boolean isBottom() {
		return valueNumbersUnconditionallyDereferenced.get(numValueNumbersInMethod);
	}

	/**
	 * Make this dataflow fact the bottom value.
	 */
	public void setIsBottom() {
		clear();
		valueNumbersUnconditionallyDereferenced.set(numValueNumbersInMethod);
	}

	/**
	 * Is this the top value?
	 * 
	 * @return true if this is the top value, false otherwise
	 */
	public boolean isTop() {
		return valueNumbersUnconditionallyDereferenced.get(numValueNumbersInMethod + 1);
	}

	/**
	 * Make this dataflow fact the top value.
	 */
	public void setIsTop() {
		clear();
		valueNumbersUnconditionallyDereferenced.set(numValueNumbersInMethod + 1);
		lastUpdateTimestamp = 0;
	}

	/**
	 * Clear the deref set.
	 * This sets the fact so it is valid as the dataflow entry fact:
	 * no future dereferences are guaranteed.
	 */
	void clear() {
		valueNumbersUnconditionallyDereferenced.clear();
		derefLocationSetMap.clear();
	}

	   /**
	 * Make this dataflow fact the same as the given one.
	 * 
	 * @param source another dataflow fact
	 */
	public void makeSameAs(UnconditionalValueDerefSet source) {
		// Copy value numbers
		valueNumbersUnconditionallyDereferenced.clear();
		valueNumbersUnconditionallyDereferenced.or(source.valueNumbersUnconditionallyDereferenced);
		lastUpdateTimestamp = source.lastUpdateTimestamp;
		// Copy dereference locations for each value number
		derefLocationSetMap.clear();
		if (source.derefLocationSetMap.size() > 0)
		for (Map.Entry<ValueNumber, Set<Location>> sourceEntry : source.derefLocationSetMap.entrySet()) {
			Set<Location> derefLocationSet = new HashSet<Location>();
			derefLocationSet.addAll(sourceEntry.getValue());
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
		return 
		 valueNumbersUnconditionallyDereferenced.equals(otherFact.valueNumbersUnconditionallyDereferenced)
		&& derefLocationSetMap.equals(otherFact.derefLocationSetMap);
	}

	/**
	 * Merge given dataflow fact into this one.
	 * We take the intersection of the unconditional deref value number set,
	 * and union the deref locations.
	 * 
	 * @param fact another dataflow fact
	 * @param skipMe TODO
	 */
	public void mergeWith(UnconditionalValueDerefSet fact, @CheckForNull ValueNumber skipMe, ValueNumberFactory valueNumberFactory) {
		if (UnconditionalValueDerefAnalysis.DEBUG) {
			System.out.println("merge update of # " + System.identityHashCode(this) + " from " + System.identityHashCode(fact));
			System.out.println("update " + this);
			System.out.println("with " + fact);

		}
		boolean resultForSkippedValue = false;
		if (skipMe != null) {
			resultForSkippedValue = valueNumbersUnconditionallyDereferenced.get(skipMe.getNumber());
		}
		// Compute the intersection of the unconditionally dereferenced value sets
		valueNumbersUnconditionallyDereferenced.and(fact.valueNumbersUnconditionallyDereferenced);
		if (skipMe != null) {
			valueNumbersUnconditionallyDereferenced.set(skipMe.getNumber(), resultForSkippedValue);
		}

		// For each unconditionally dereferenced value...
		for (int i = 0; i < numValueNumbersInMethod; i++) {
			ValueNumber vn = valueNumberFactory.forNumber(i);
			if (vn.equals(skipMe)) continue;
			Set<Location> factDerefLocationSet = fact.derefLocationSetMap.get(vn);
			if (valueNumbersUnconditionallyDereferenced.get(i) ) {
				if (factDerefLocationSet != null && !factDerefLocationSet.isEmpty()) {
					// Compute the union of the dereference locations for
					// this value number.
					Set<Location> derefLocationSet = derefLocationSetMap.get(vn);
					if (derefLocationSet == null) {
						derefLocationSet = new HashSet<Location>();
						derefLocationSetMap.put(vn, derefLocationSet);
					}
					derefLocationSet.addAll(fact.derefLocationSetMap.get(vn));
				}
			} else {
				Set<Location> removed = derefLocationSetMap.remove(vn);
				// The value number is not in the fact:
				// remove its location set
				if (removed != null) {
						if (UnconditionalValueDerefAnalysis.DEBUG) System.out.println("Goodbye: " + removed);
				}
			}
		}
	}
	public void unionWith(UnconditionalValueDerefSet fact, ValueNumberFactory valueNumberFactory) {
		if (UnconditionalValueDerefAnalysis.DEBUG) {
			System.out.println("union update of # " + System.identityHashCode(this) + " from " + System.identityHashCode(fact));
		}
		// Compute the union of the unconditionally dereferenced value sets
		valueNumbersUnconditionallyDereferenced.or(fact.valueNumbersUnconditionallyDereferenced);

		// For each unconditionally dereferenced value...
		for (int i = 0; i < numValueNumbersInMethod; i++) {
			ValueNumber vn = valueNumberFactory.forNumber(i);

			if (fact.valueNumbersUnconditionallyDereferenced.get(i)) {
					// Compute the union of the dereference locations for
				// this value number.
				Set<Location> derefLocationSet = derefLocationSetMap.get(vn);
				if (derefLocationSet == null) {
					derefLocationSet = new HashSet<Location>();
					derefLocationSetMap.put(vn,derefLocationSet);
				}
				derefLocationSet.addAll(fact.derefLocationSetMap.get(vn));
			} else {
				derefLocationSetMap.put(vn, new HashSet<Location>(fact.getDerefLocationSet(vn)));
			}
		}
	}

	/**
	 * Mark a value as being dereferenced at given Location.
	 * 
	 * @param vn       the value
	 * @param location the Location
	 */
	public void addDeref(ValueNumber vn, Location location) {
		if (UnconditionalValueDerefAnalysis.DEBUG) {
			System.out.println("Adding dereference of " + vn + " to # " + System.identityHashCode(this) + " @ " + location);
		}
		valueNumbersUnconditionallyDereferenced.set(vn.getNumber());

		Set<Location> derefLocationSet = getDerefLocationSet(vn);
		derefLocationSet.add(location);
	}

	/**
	 * Set a value as being unconditionally dereferenced at the
	 * given set of locations. 
	 * 
	 * @param vn       the value
	 * @param derefSet the Set of dereference Locations
	 */
	public void setDerefSet(ValueNumber vn, Set<Location> derefSet) {
		if (UnconditionalValueDerefAnalysis.DEBUG) {
			System.out.println("Adding dereference of " + vn + " for # " + System.identityHashCode(this) + " to " + derefSet);
		}
		valueNumbersUnconditionallyDereferenced.set(vn.getNumber());

		Set<Location> derefLocationSet = getDerefLocationSet(vn);
		derefLocationSet.clear();
		derefLocationSet.addAll(derefSet);
	}

	/**
	 * Clear the set of dereferences for given ValueNumber
	 * 
	 * @param value the ValueNumber
	 */
	public void clearDerefSet(ValueNumber value) {
		if (UnconditionalValueDerefAnalysis.DEBUG) {
			System.out.println("Clearing dereference of " + value + " for # " + System.identityHashCode(this));
		}
		valueNumbersUnconditionallyDereferenced.clear(value.getNumber());
		  derefLocationSetMap.remove(value);
	}

	/**
	 * Get the set of dereference Locations for given value number.
	 * 
	 * @param vn the value number
	 * @return the set of dereference Locations
	 */
	private Set<Location> getDerefLocationSet(ValueNumber vn) {
		Set<Location> derefLocationSet = derefLocationSetMap.get(vn);
		if (derefLocationSet == null) {
			derefLocationSet  = new HashSet<Location>();
			derefLocationSetMap.put(vn, derefLocationSet);
		}
		return derefLocationSet;
	}

	/**
	 * Return whether or not the given value number is unconditionally dereferenced.
	 * 
	 * @param vn the value number
	 * @return true if the value is unconditionally dereferenced, false otherwise
	 */
	public boolean isUnconditionallyDereferenced(ValueNumber vn) {
		return valueNumbersUnconditionallyDereferenced.get(vn.getNumber());
	}

	public Set<ValueNumber> getValueNumbersThatAreUnconditionallyDereferenced() {
		HashSet<ValueNumber> result = new HashSet<ValueNumber>();
		for(Map.Entry<ValueNumber, Set<Location>> e : derefLocationSetMap.entrySet()) {
			if (!e.getValue().isEmpty())
				result.add(e.getKey());
		}
		return result;
	}

	public void retainOnlyTheseValueNumbers(Collection<ValueNumber> valueNumbers) {
		for(Iterator<ValueNumber> i = derefLocationSetMap.keySet().iterator(); i.hasNext(); ) {
			ValueNumber v = i.next();
			if (!valueNumbers.contains(v)) {
				i.remove();
				valueNumbersUnconditionallyDereferenced.clear(v.getNumber());
			 }
		}
	}
	/**
	 * Get the set of Locations where given value is guaranteed to be dereferenced.
	 * (I.e., if non-implicit-exception control paths are followed, one of these
	 * locations will be reached).
	 * 
	 * @param vn the value
	 * @return set of Locations, one of which will definitely be reached
	 *          if non-implicit-exception control paths are followed
	 */
	public Set<Location> getUnconditionalDerefLocationSet(ValueNumber vn) {
		Set<Location> derefLocationSet = derefLocationSetMap.get(vn);
		if (derefLocationSet == null ) {
			derefLocationSet = TigerSubstitutes.emptySet();
		}
		return derefLocationSet;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (isTop()) {
			return "[TOP]";
		}
		if (isBottom()) {
			return "[BOTTOM]";
		}

		StringBuffer buf = new StringBuffer();
		buf.append('[');
		boolean firstVN = true;
		for (int i = 0; i < numValueNumbersInMethod; i++)  {
			if (!(valueNumbersUnconditionallyDereferenced.get(i))) {
				continue;
			}
			if (firstVN) {
				firstVN = false;
			} else {
				buf.append(',');
			}
			buf.append('{');
			buf.append(i);
			if (valueNumbersUnconditionallyDereferenced.get(i))
			buf.append(':');
			else buf.append('?');
			TreeSet<Location> derefLocationSet = new TreeSet<Location>();
			derefLocationSet.addAll(getDerefLocationSet(i));
			boolean firstLoc = true;
			for (Location location : derefLocationSet) {
				if (firstLoc) {
					firstLoc = false;
				} else {
					buf.append(',');
				}
				buf.append(
						"(" +
								location.getBasicBlock().getLabel() +
								":" +
								location.getHandle().getPosition() +
								")");
			}
			buf.append('}');
		}
		buf.append(']');
		buf.append(" # ");
		buf.append(System.identityHashCode(this));
		return buf.toString();
	}

	private Set<Location> getDerefLocationSet(int vn) {
		for (Map.Entry<ValueNumber, Set<Location>> entry : derefLocationSetMap.entrySet()) {
			if (entry.getKey().getNumber() == vn) {
				return Collections.unmodifiableSet(entry.getValue());
			}
		}
		return new HashSet<Location>();
	}

	/**
	 * @param location
	 * @param vnaFrame
	 */
	public  void cleanDerefSet(@CheckForNull Location location, ValueNumberFrame vnaFrame) {

		Set<ValueNumber> valueNumbers = new HashSet<ValueNumber>(vnaFrame.allSlots());

		valueNumbers.addAll(vnaFrame.valueNumbersForLoads());

		if (UnconditionalValueDerefAnalysis.DEBUG) {
			for(ValueNumber v : getValueNumbersThatAreUnconditionallyDereferenced())
				if (!valueNumbers.contains(v)) {
					System.out.println("\nWhy is " + v + " unconditionally dereferenced in #" + System.identityHashCode(this));
					System.out.println("VN: " + vnaFrame);
					System.out.println("UD: " + this);
					System.out.println("Location: " + location);
					System.out.println();
				}

		}
		retainOnlyTheseValueNumbers(valueNumbers);
	}

	/**
	 * @param lastUpdateTimestamp The lastUpdateTimestamp to set.
	 */
	public void setLastUpdateTimestamp(int lastUpdateTimestamp) {
		this.lastUpdateTimestamp = lastUpdateTimestamp;
	}

	/**
	 * @return Returns the lastUpdateTimestamp.
	 */
	public int getLastUpdateTimestamp() {
		return lastUpdateTimestamp;
	}

	/**
	 * @return
	 */
	public boolean isEmpty() {
		return valueNumbersUnconditionallyDereferenced.isEmpty();
	}
}
