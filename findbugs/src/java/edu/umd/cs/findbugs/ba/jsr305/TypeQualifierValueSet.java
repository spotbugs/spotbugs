/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs.ba.jsr305;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import edu.umd.cs.findbugs.TigerSubstitutes;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;

/**
 * Set of ValueNumbers and their corresponding FlowValues.
 * 
 * @author David Hovemeyer
 */
public class TypeQualifierValueSet {
	// States
	enum State { VALID, TOP, BOTTOM };
	
	private static final FlowValue DEFAULT_FLOW_VALUE = FlowValue.MAYBE;

	private Map<ValueNumber, FlowValue> valueMap;
	private Map<ValueNumber, Set<Location>> whereAlways;
	private Map<ValueNumber, Set<Location>> whereNever;
	private State state = State.VALID;

	public TypeQualifierValueSet() {
		this.valueMap = new HashMap<ValueNumber, FlowValue>();
		this.whereAlways = new HashMap<ValueNumber, Set<Location>>();
		this.whereNever = new HashMap<ValueNumber, Set<Location>>();
		this.state = State.TOP;
	}

	public void setValue(ValueNumber vn, FlowValue flowValue, Location location) {
		setValue(vn, flowValue);
		
		if (flowValue == FlowValue.ALWAYS) {
			addLocation(whereAlways, vn, location);
		}
		
		if (flowValue == FlowValue.NEVER) {
			addLocation(whereNever, vn, location);
		}
	}

	private void setValue(ValueNumber vn, FlowValue flowValue) {
		if (flowValue == DEFAULT_FLOW_VALUE) {
			// Default flow value is not stored explicitly
			valueMap.remove(vn);
		} else {
			valueMap.put(vn, flowValue);
		}
	}

	private static void addLocation(Map<ValueNumber, Set<Location>> locationSetMap, ValueNumber vn, Location location) {
		Set<Location> locationSet = getOrCreateLocationSet(locationSetMap, vn);
		locationSet.add(location);
	}
	
	public Set<Location> getWhereAlways(ValueNumber vn) {
		return getOrCreateLocationSet(whereAlways, vn);
	}
	
	public Set<Location> getWhereNever(ValueNumber vn) {
		return getOrCreateLocationSet(whereNever, vn);
	}

	private static Set<Location> getOrCreateLocationSet(Map<ValueNumber, Set<Location>> locationSetMap, ValueNumber vn) {
		Set<Location> locationSet = locationSetMap.get(vn);
		if (locationSet == null) {
			locationSet = new HashSet<Location>();
			locationSetMap.put(vn, locationSet);
		}
		return locationSet;
	}

	public FlowValue getValue(ValueNumber vn) {
		FlowValue result = valueMap.get(vn);
		return result != null ? result : DEFAULT_FLOW_VALUE;
	}

	public boolean isValid() {
		return state == State.VALID;
	}

	public void makeValid() {
		this.state = State.VALID;
		this.valueMap.clear();
	}

	public void makeSameAs(TypeQualifierValueSet source) {
		this.state = source.state;
		this.valueMap.clear();
		this.valueMap.putAll(source.valueMap);
	}

	public boolean isTop() {
		return state == State.TOP;
	}

	public void setTop() {
		this.valueMap.clear();
		this.state = State.TOP;
	}

	public boolean isBottom() {
		return state == State.BOTTOM;
	}

	public void setBottom() {
		this.valueMap.clear();
		this.state = State.BOTTOM;
	}

	public void propagateAcrossPhiNode(ValueNumber targetVN, ValueNumber sourceVN) {
		assert isValid();
		assert targetVN.hasFlag(ValueNumber.PHI_NODE);

		setValue(sourceVN, getValue(targetVN));
		
		// XXX: should put some kind of bottom value here? Probably doesn't matter - targetVN will not be used
		setValue(targetVN, FlowValue.MAYBE);
		
		// Propagate sink location information
		transferLocationSet(whereAlways, sourceVN, targetVN);
		transferLocationSet(whereNever, sourceVN, targetVN);
	}

    private static void transferLocationSet(Map<ValueNumber, Set<Location>> locationSetMap, ValueNumber sourceVN, ValueNumber targetVN) {
		Set<Location> sinkLocSet = getOrCreateLocationSet(locationSetMap, targetVN);
		for (Location sinkLoc : sinkLocSet) {
			addLocation(locationSetMap, sourceVN, sinkLoc);
		}
		clearLocationSet(locationSetMap, targetVN);
    }

	private static void clearLocationSet(Map<ValueNumber, Set<Location>> locationSetMap, ValueNumber vn) {
		locationSetMap.remove(vn);
	}

	public void mergeWith(TypeQualifierValueSet fact) throws DataflowAnalysisException {
		if (!isValid() || !fact.isValid()) {
			throw new DataflowAnalysisException("merging an invalid TypeQualifierValueSet");
		}
		
		Set<ValueNumber> interesting = new HashSet<ValueNumber>();
		this.getInterestingValueNumbers(interesting);
		fact.getInterestingValueNumbers(interesting);
		
		for (ValueNumber vn : interesting) {
			setValue(vn, FlowValue.meet(this.getValue(vn), fact.getValue(vn)));
			mergeLocationSets(this.whereAlways, fact.whereAlways, vn);
			mergeLocationSets(this.whereNever, fact.whereNever, vn);
		}
	}

	private void mergeLocationSets(
			Map<ValueNumber, Set<Location>> locationSetMapToUpdate,
			Map<ValueNumber, Set<Location>> otherLocationSetMap,
			ValueNumber vn) {
		if (!otherLocationSetMap.containsKey(vn)) {
			return;
		}
		Set<Location> locationSetToUpdate = getOrCreateLocationSet(whereAlways, vn);
		locationSetToUpdate.addAll(getOrCreateLocationSet(otherLocationSetMap, vn));
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		TypeQualifierValueSet other = (TypeQualifierValueSet) obj;
		if (this.isValid() && other.isValid()) {
			return this.valueMap.equals(other.valueMap);
		} else {
			return this.state == other.state;
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		if (state != State.VALID) {
			return state.toString();
		}
		
		TreeSet<ValueNumber> interesting = new TreeSet<ValueNumber>(); 
		getInterestingValueNumbers(interesting);
		
		StringBuffer buf = new StringBuffer();
		
		for (ValueNumber vn : interesting) {
			if (buf.length() > 0) {
				buf.append(", ");
			}
			buf.append(vn.getNumber());
			buf.append("->");
			buf.append("{");
			buf.append(getValue(vn).toString());
			buf.append("[");
			appendLocations(buf, "YES=", getOrCreateLocationSet(whereAlways, vn));
			buf.append(",");
			appendLocations(buf, "NO=", getOrCreateLocationSet(whereNever, vn));
			buf.append("]}");
		}
		
		return buf.toString();
	}

	private void getInterestingValueNumbers(Set<ValueNumber> interesting) {
		interesting.addAll(valueMap.keySet());
		interesting.addAll(whereAlways.keySet());
		interesting.addAll(whereNever.keySet());
	}

	private static void appendLocations(StringBuffer buf, String key, Set<Location> locationSet) {
		TreeSet<Location> sortedLocSet = new TreeSet<Location>();
		boolean first = true;
		buf.append(key);
		buf.append("(");
		for (Location loc : sortedLocSet) {
			if (!first) {
				first = true;
			} else {
				buf.append(",");
			}
			buf.append(loc.toCompactString());
		}
		buf.append(")");
	}
}
