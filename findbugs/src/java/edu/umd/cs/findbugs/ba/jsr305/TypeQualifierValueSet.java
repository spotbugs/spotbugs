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

import java.util.Collection;
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
		valueMap.put(vn, flowValue);
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
		return result != null ? result : FlowValue.TOP;
	}

	public Collection<? extends ValueNumber> getValueNumbers() {
		return valueMap.keySet();
	}

	public boolean isValid() {
		return state == State.VALID;
	}

	public void makeValid() {
		this.state = State.VALID;
		this.valueMap.clear();
		this.whereAlways.clear();
		this.whereNever.clear();
	}

	public void makeSameAs(TypeQualifierValueSet source) {
		this.state = source.state;
		this.valueMap.clear();
		this.valueMap.putAll(source.valueMap);
		copyLocationSetMap(this.whereAlways, source.whereAlways);
		copyLocationSetMap(this.whereNever, source.whereNever);
	}

	private void copyLocationSetMap(Map<ValueNumber, Set<Location>> dest, Map<ValueNumber, Set<Location>> source) {
		dest.keySet().retainAll(source.keySet());
		
		for (Map.Entry<ValueNumber, Set<Location>> entry : source.entrySet()) {
			Set<Location> locSet = getOrCreateLocationSet(dest, entry.getKey());
			locSet.clear();
			locSet.addAll(entry.getValue());
		}
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

	public void propagateAcrossPhiNode(ValueNumber fromVN, ValueNumber toVN) {
		assert isValid();

		setValue(toVN, getValue(fromVN));
		
		// Propagate sink location information
		transferLocationSet(whereAlways, toVN, fromVN);
		transferLocationSet(whereNever, toVN, fromVN);
		
		// Remove all information about the "from" value
		valueMap.remove(fromVN);
		whereAlways.remove(fromVN);
		whereNever.remove(fromVN);
	}

    private static void transferLocationSet(Map<ValueNumber, Set<Location>> locationSetMap, ValueNumber toVN, ValueNumber fromVN) {
		Set<Location> locSet = getOrCreateLocationSet(locationSetMap, fromVN);
		
		for (Location loc : locSet) {
			addLocation(locationSetMap, toVN, loc);
		}
    }

	public void mergeWith(TypeQualifierValueSet fact) throws DataflowAnalysisException {
		if (!isValid() || !fact.isValid()) {
			throw new DataflowAnalysisException("merging an invalid TypeQualifierValueSet");
		}
		
		Set<ValueNumber> interesting = new HashSet<ValueNumber>();
		interesting.addAll(this.valueMap.keySet());
		interesting.addAll(fact.valueMap.keySet());
		
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
		interesting.addAll(valueMap.keySet());
		
		StringBuffer buf = new StringBuffer();
		
		buf.append("{");
		
		for (ValueNumber vn : interesting) {
			if (buf.length() > 1) {
				buf.append(", ");
			}
			buf.append(vn.getNumber());
			buf.append("->");
			buf.append(getValue(vn).toString());
			buf.append("[");
			appendLocations(buf, "YES=", getOrCreateLocationSet(whereAlways, vn));
			buf.append(",");
			appendLocations(buf, "NO=", getOrCreateLocationSet(whereNever, vn));
			buf.append("]");
		}
		
		buf.append("}");
		
		return buf.toString();
	}

	private static void appendLocations(StringBuffer buf, String key, Set<Location> locationSet) {
		TreeSet<Location> sortedLocSet = new TreeSet<Location>();
		sortedLocSet.addAll(locationSet);
		boolean first = true;
		buf.append(key);
		buf.append("(");
		for (Location loc : sortedLocSet) {
			if (first) {
				first = false;
			} else {
				buf.append(",");
			}
			buf.append(loc.toCompactString());
		}
		buf.append(")");
	}
}
