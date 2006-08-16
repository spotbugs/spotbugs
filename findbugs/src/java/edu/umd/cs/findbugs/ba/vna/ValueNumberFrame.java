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

package edu.umd.cs.findbugs.ba.vna;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.ba.Frame;
import edu.umd.cs.findbugs.ba.XField;

/**
 * A dataflow value representing a Java stack frame with value number
 * information.
 *
 * @author David Hovemeyer
 * @see ValueNumber
 * @see ValueNumberAnalysis
 */
public class ValueNumberFrame extends Frame<ValueNumber> implements ValueNumberAnalysisFeatures {

	private ArrayList<ValueNumber> mergedValueList;
	private Map<AvailableLoad, ValueNumber[]> availableLoadMap;
	private Map<AvailableLoad,ValueNumber> mergedLoads ;
	private Map<ValueNumber, AvailableLoad> previouslyKnownAs;
	public boolean phiNodeForLoads;

	public ValueNumberFrame(int numLocals) {
		super(numLocals);
		if (REDUNDANT_LOAD_ELIMINATION) {
			availableLoadMap = new HashMap<AvailableLoad, ValueNumber[]>();
			mergedLoads =  new HashMap<AvailableLoad,ValueNumber> ();
			previouslyKnownAs =  new HashMap<ValueNumber, AvailableLoad> ();
		}
	}

	public String availableLoadMapAsString() {
		StringBuffer buf = new StringBuffer("{ ");
		for(Map.Entry<AvailableLoad, ValueNumber[]> e : availableLoadMap.entrySet()) {
			buf.append(e.getKey());
			buf.append("=");
			for(ValueNumber v : e.getValue()) 
				buf.append(v).append(",");
			buf.append(";  ");
		}
		
		buf.append(" }");
		return buf.toString();
	}
	public @CheckForNull AvailableLoad getLoad(ValueNumber v) {
		for(Map.Entry<AvailableLoad, ValueNumber[]> e : availableLoadMap.entrySet()) {
			if (e.getValue() != null)
				for(ValueNumber v2 : e.getValue())
					if (v.equals(v2)) return e.getKey();
		}
		return null;
	}
	/**
	 * Look for an available load.
	 *
	 * @param availableLoad the AvailableLoad (reference and field)
	 * @return the value(s) available, or null if no matching entry is found
	 */
	public ValueNumber[] getAvailableLoad(AvailableLoad availableLoad) {
		return availableLoadMap.get(availableLoad);
	}

	/**
	 * Add an available load.
	 *
	 * @param availableLoad the AvailableLoad (reference and field)
	 * @param value         the value(s) loaded
	 */
	public void addAvailableLoad(AvailableLoad availableLoad, @NonNull ValueNumber[] value) {
		if (value == null) throw new IllegalStateException();
		availableLoadMap.put(availableLoad, value);

		for(ValueNumber v : value) {
			previouslyKnownAs.put(v, availableLoad);
			if (RLE_DEBUG) {
				System.out.println("Adding available load of " + availableLoad + " for " + v + " to " + System.identityHashCode(this));
			}
		}
	}

	/**
	 * Kill all loads of given field.
	 *
	 * @param field the field
	 */
	public void killLoadsOfField(XField field) {
		Iterator<AvailableLoad> i = availableLoadMap.keySet().iterator();
		while (i.hasNext()) {
			AvailableLoad availableLoad = i.next();
			if (availableLoad.getField().equals(field)) {
				i.remove();
			}
		}
	}

	/**
	 * Kill all loads.
	 * This conservatively handles method calls where we
	 * don't really know what fields might be assigned.
	 */
	public void killAllLoads() {
		if (REDUNDANT_LOAD_ELIMINATION) {
			for(Iterator<AvailableLoad> i = availableLoadMap.keySet().iterator(); i.hasNext(); ) {
				AvailableLoad availableLoad = i.next();
				if (!availableLoad.getField().isFinal()) {
					if (false) System.out.println("KILLING load of " + availableLoad);
					i.remove();
				}
			}
		}
	}
	/**
	 * Kill all loads.
	 * This conservatively handles method calls where we
	 * don't really know what fields might be assigned.
	 */
	public void killAllLoadsOf(@CheckForNull ValueNumber v) {
		if (REDUNDANT_LOAD_ELIMINATION) {
			for(Iterator<AvailableLoad> i = availableLoadMap.keySet().iterator(); i.hasNext(); ) {
				AvailableLoad availableLoad = i.next();
				if (!availableLoad.getField().isFinal() && availableLoad.getReference() == v) {
					if (false) System.out.println("Killing load of " + availableLoad);
					i.remove();
				}
			}
		}
	}

	void mergeAvailableLoadSets(ValueNumberFrame other, ValueNumberFactory factory) {
		if (REDUNDANT_LOAD_ELIMINATION) {
			// Merge available load sets.
			// Only loads that are available in both frames
			// remain available. All others are discarded.
			String s = "";
			if (RLE_DEBUG) {
				s = "Merging " + this.availableLoadMapAsString() + " and " + other.availableLoadMapAsString();
			}
			boolean changed = false;
			if (other.isBottom()) {
				changed = !this.availableLoadMap.isEmpty();
				availableLoadMap.clear();
			}
			else if (!other.isTop()) {
				for(Map.Entry<AvailableLoad,ValueNumber[]> e : availableLoadMap.entrySet()) {
					AvailableLoad load = e.getKey();
					ValueNumber[] myVN = e.getValue();
					ValueNumber[] otherVN = other.availableLoadMap.get(load);
					if (this.phiNodeForLoads && myVN != null && myVN.length == 1 && myVN[0].hasFlag(ValueNumber.PHI_NODE))
						continue;
					if (!Arrays.equals(myVN, otherVN)) {
						
						ValueNumber phi = mergedLoads.get(load);
						if (phi == null) {
							phi = factory.createFreshValue();
							phi.setFlag(ValueNumber.PHI_NODE);
							mergedLoads.put(load, phi);
							if (RLE_DEBUG)
								System.out.println("Creating phi node " + phi + " for " + load);	
						}
						
						changed = true;
						e.setValue(new ValueNumber[] { phi });
					}
					
				}	
			}
			previouslyKnownAs.putAll(other.previouslyKnownAs);
			if (changed)
				this.phiNodeForLoads = true;
			if (changed && RLE_DEBUG) {
				System.out.println(s);
				System.out.println("  Result is " + this.availableLoadMapAsString());
				System.out.println(" Set phi for " + System.identityHashCode(this));
			}
		}
	}


	ValueNumber getMergedValue(int slot) {
		return mergedValueList.get(slot);
	}

	void setMergedValue(int slot, ValueNumber value) {
		mergedValueList.set(slot, value);
	}

	@Override
	public void copyFrom(Frame<ValueNumber> other) {
		// If merged value list hasn't been created yet, create it.
		if (mergedValueList == null && other.isValid()) {
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
			availableLoadMap.putAll(((ValueNumberFrame) other).availableLoadMap);
			previouslyKnownAs.clear();
			previouslyKnownAs.putAll(((ValueNumberFrame) other).previouslyKnownAs);
		}

		super.copyFrom(other);
	}

	@Override
	public String toString() {
		String frameValues = super.toString();
		if (RLE_DEBUG) {
			StringBuffer buf = new StringBuffer();
			buf.append(frameValues);

			Iterator<AvailableLoad> i = availableLoadMap.keySet().iterator();
			boolean first = true;
			while (i.hasNext()) {
				AvailableLoad key = i.next();
				ValueNumber[] value = availableLoadMap.get(key);
				if (first)
					first = false;
				else
					buf.append(',');
				buf.append(key + "=" + valueToString(value));
			}
			
			buf.append(" #");
			buf.append(System.identityHashCode(this));
			if (phiNodeForLoads) buf.append(" phi");
			return buf.toString();
		} else {
			return frameValues;
		}
	}

	private static String valueToString(ValueNumber[] valueNumberList) {
		StringBuffer buf = new StringBuffer();
		buf.append('[');
		boolean first = true;
		for (ValueNumber aValueNumberList : valueNumberList) {
			if (first)
				first = false;
			else
				buf.append(',');
			buf.append(aValueNumberList.getNumber());
		}
		buf.append(']');
		return buf.toString();
	}

	public boolean fuzzyMatch(ValueNumber v1, ValueNumber v2) {
		return v1.equals(v2) || fromMatchingLoads(v1, v2) || haveMatchingFlags(v1, v2); 
	}
		
	public boolean fromMatchingLoads(ValueNumber v1, ValueNumber v2) {
		AvailableLoad load1 = getLoad(v1);
		if (load1 == null) load1 = previouslyKnownAs.get(v1);
		if (load1 == null) return false;
		AvailableLoad load2 = getLoad(v2);
		if (load2 == null) load2 = previouslyKnownAs.get(v2);
		if (load2 == null) return false;
		return load1.equals(load2);
	}

	/**
	 * @param v1
	 * @param v2
	 * @return
	 */
	public boolean haveMatchingFlags(ValueNumber v1, ValueNumber v2) {
		int flag1 = v1.getFlags();
		int flag2 = v2.getFlags();
		return (flag1 & flag2) != 0;
	}
	
	public Collection<ValueNumber> valueNumbersForLoads() {
		HashSet<ValueNumber> result = new HashSet<ValueNumber>();
		for(Map.Entry<AvailableLoad, ValueNumber[]> e : availableLoadMap.entrySet()) {
			if (e.getValue() != null)
				for(ValueNumber v2 : e.getValue())
					result.add(v2);
		}

		return result;
	}
	
	
}

// vim:ts=4
