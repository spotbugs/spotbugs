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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.ba.Frame;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.util.Strings;

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
			setAvailableLoadMap(Collections.EMPTY_MAP);
			setMergedLoads(Collections.EMPTY_MAP);
			setPreviouslyKnownAs(Collections.EMPTY_MAP);
		}
	}

	public String availableLoadMapAsString() {
		StringBuffer buf = new StringBuffer("{ ");
		for(Map.Entry<AvailableLoad, ValueNumber[]> e : getAvailableLoadMap().entrySet()) {
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
        if (!REDUNDANT_LOAD_ELIMINATION) return null;
		for(Map.Entry<AvailableLoad, ValueNumber[]> e : getAvailableLoadMap().entrySet()) {
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
		return getAvailableLoadMap().get(availableLoad);
	}

	/**
	 * Add an available load.
	 *
	 * @param availableLoad the AvailableLoad (reference and field)
	 * @param value         the value(s) loaded
	 */
	public void addAvailableLoad(AvailableLoad availableLoad, @NonNull ValueNumber[] value) {
		if (value == null) throw new IllegalStateException();
		getUpdateableAvailableLoadMap().put(availableLoad, value);

		for(ValueNumber v : value) {
			getUpdateablePreviouslyKnownAs().put(v, availableLoad);
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
		Iterator<AvailableLoad> i = getAvailableLoadMap().keySet().iterator();
		while (i.hasNext()) {
			AvailableLoad availableLoad = i.next();
			if (availableLoad.getField().equals(field)) {
				if (RLE_DEBUG) 
					System.out.println("KILLING Load of " + availableLoad + " in " + this);
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
			for(Iterator<AvailableLoad> i = getAvailableLoadMap().keySet().iterator(); i.hasNext(); ) {
				AvailableLoad availableLoad = i.next();
				if (!availableLoad.getField().isFinal()) {
					if (RLE_DEBUG) 
						System.out.println("KILLING load of " + availableLoad + " in " + this);
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
			for(Iterator<AvailableLoad> i = getAvailableLoadMap().keySet().iterator(); i.hasNext(); ) {
				AvailableLoad availableLoad = i.next();
				if (!availableLoad.getField().isFinal() && availableLoad.getReference() == v) {
					if (RLE_DEBUG) System.out.println("Killing load of " + availableLoad + " in " + this);
					i.remove();
				}
			}
		}
	}

    public void killLoadsWithSimilarName(String className, String methodName) {
        String packageName = extractPackageName(className);
        if (REDUNDANT_LOAD_ELIMINATION) {
            for(Iterator<AvailableLoad> i = getAvailableLoadMap().keySet().iterator(); i.hasNext(); ) {
                AvailableLoad availableLoad = i.next();
                
                XField field = availableLoad.getField();
                String fieldPackageName = extractPackageName(field.getClassName());
                if (packageName.equals(fieldPackageName) && field.isStatic() 
                        && methodName.toLowerCase().indexOf(field.getName().toLowerCase()) >= 0)
                    i.remove();
                
            }
        }
    }

    /**
     * @param className
     * @return
     */
    private String extractPackageName(String className) {
        return className.substring(className.lastIndexOf('.')+1);
    }

	void mergeAvailableLoadSets(ValueNumberFrame other, ValueNumberFactory factory, MergeTree mergeTree) {
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
				changed = !this.getAvailableLoadMap().isEmpty();
				setAvailableLoadMap(Collections.EMPTY_MAP);
			}
			else if (!other.isTop()) {
				for(Map.Entry<AvailableLoad,ValueNumber[]> e : getAvailableLoadMap().entrySet()) {
					AvailableLoad load = e.getKey();
					ValueNumber[] myVN = e.getValue();
					ValueNumber[] otherVN = other.getAvailableLoadMap().get(load);
					if (false && this.phiNodeForLoads && myVN != null && myVN.length == 1 && myVN[0].hasFlag(ValueNumber.PHI_NODE))
						continue;
					if (!Arrays.equals(myVN, otherVN)) {
						
						ValueNumber phi = getMergedLoads().get(load);
						if (phi == null) {
							phi = factory.createFreshValue();
							int flags = ValueNumber.PHI_NODE;
							
							getUpdateableMergedLoads().put(load, phi);
							for(ValueNumber vn : myVN) {
								mergeTree.mapInputToOutput(vn, phi);
								flags |= vn.getFlags();
							}
							if (otherVN != null) for(ValueNumber vn : otherVN) {
								mergeTree.mapInputToOutput(vn, phi);
								flags |= vn.getFlags();
							}
							phi.setFlag(flags);
							if (RLE_DEBUG)
								System.out.println("Creating phi node " + phi + " for " + load + " from " + Strings.toString(myVN) + " x " +  Strings.toString(otherVN) + " in " + System.identityHashCode(this));	
							changed = true;
							e.setValue(new ValueNumber[] { phi });
						} else {
							if (RLE_DEBUG)
									System.out.println("Reusing phi node : " + phi + " for " + load + " from "+ Strings.toString(myVN) + " x " +  Strings.toString(otherVN)+ " in " + System.identityHashCode(this));
							if (myVN.length != 0 || !myVN[0].equals(phi))
								e.setValue(new ValueNumber[] { phi });
						}

					}
					
				}	
			}
			Map<ValueNumber, AvailableLoad> previouslyKnownAsOther = other.getPreviouslyKnownAs();
			if (previouslyKnownAsOther.size() != 0)
				getUpdateablePreviouslyKnownAs().putAll(previouslyKnownAsOther);
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
			Map<AvailableLoad, ValueNumber[]> availableLoadMapOther = ((ValueNumberFrame) other).getAvailableLoadMap();
			if (availableLoadMapOther.size() == 0) 
				setAvailableLoadMap(Collections.EMPTY_MAP);
			else {
				getUpdateableAvailableLoadMap().clear();
				getUpdateableAvailableLoadMap().putAll(availableLoadMapOther);
			}
			Map<ValueNumber, AvailableLoad> previouslyKnownAsOther = ((ValueNumberFrame) other).getPreviouslyKnownAs();
			int size = previouslyKnownAsOther.size();
               if (size == 0) 
				setPreviouslyKnownAs(Collections.EMPTY_MAP);
			else {
				assignPreviouslyKnownAs(previouslyKnownAsOther);  // HUGE AMOUNT OF ALLOCATIONS HAPPEN HERE
			}
		
		}

		super.copyFrom(other);
	}

	@Override
	public String toString() {
		String frameValues = super.toString();
		if (RLE_DEBUG) {
			StringBuffer buf = new StringBuffer();
			buf.append(frameValues);

			Iterator<AvailableLoad> i = getAvailableLoadMap().keySet().iterator();
			boolean first = true;
			while (i.hasNext()) {
				AvailableLoad key = i.next();
				ValueNumber[] value = getAvailableLoadMap().get(key);
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
		if (REDUNDANT_LOAD_ELIMINATION)
		  return v1.equals(v2) || fromMatchingLoads(v1, v2) || haveMatchingFlags(v1, v2); 
		else
		  return v1.equals(v2);
	}
		
	public boolean fromMatchingLoads(ValueNumber v1, ValueNumber v2) {
		AvailableLoad load1 = getLoad(v1);
		if (load1 == null) load1 = getPreviouslyKnownAs().get(v1);
		if (load1 == null) return false;
		AvailableLoad load2 = getLoad(v2);
		if (load2 == null) load2 = getPreviouslyKnownAs().get(v2);
		if (load2 == null) return false;
		return load1.equals(load2);
	}

	/**
	 * @param v1
	 * @param v2
	 * @return true if v1 and v2 have a flag in common
	 */
	public boolean haveMatchingFlags(ValueNumber v1, ValueNumber v2) {
		int flag1 = v1.getFlags();
		int flag2 = v2.getFlags();
		return (flag1 & flag2) != 0;
	}
	
	public Collection<ValueNumber> valueNumbersForLoads() {
		HashSet<ValueNumber> result = new HashSet<ValueNumber>();
		if (REDUNDANT_LOAD_ELIMINATION)
		for(Map.Entry<AvailableLoad, ValueNumber[]> e : getAvailableLoadMap().entrySet()) {
			if (e.getValue() != null)
				for(ValueNumber v2 : e.getValue())
					result.add(v2);
		}

		return result;
	}

	/**
	 * @param availableLoadMap The availableLoadMap to set.
	 */
	private void setAvailableLoadMap(Map<AvailableLoad, ValueNumber[]> availableLoadMap) {
		this.availableLoadMap = availableLoadMap;
	}

	/**
	 * @return Returns the availableLoadMap.
	 */
	private Map<AvailableLoad, ValueNumber[]> getAvailableLoadMap() {
		return availableLoadMap;
	}
	private Map<AvailableLoad, ValueNumber[]> getUpdateableAvailableLoadMap() {
		if (!(availableLoadMap instanceof HashMap))
			availableLoadMap = new HashMap<AvailableLoad, ValueNumber[]>();
		return availableLoadMap;
	}
	/**
	 * @param mergedLoads The mergedLoads to set.
	 */
	private void setMergedLoads(Map<AvailableLoad,ValueNumber> mergedLoads) {
		this.mergedLoads = mergedLoads;
	}

	/**
	 * @return Returns the mergedLoads.
	 */
	private Map<AvailableLoad,ValueNumber> getMergedLoads() {
		return mergedLoads;
	}
	private Map<AvailableLoad,ValueNumber> getUpdateableMergedLoads() {
		if (!(mergedLoads instanceof HashMap))
			mergedLoads = new HashMap<AvailableLoad, ValueNumber>();
		
		return mergedLoads;
	}

	/**
	 * @param previouslyKnownAs The previouslyKnownAs to set.
	 */
	private void setPreviouslyKnownAs(Map<ValueNumber, AvailableLoad> previouslyKnownAs) {
		this.previouslyKnownAs = previouslyKnownAs;
	}

	/**
	 * @return Returns the previouslyKnownAs.
	 */
	private Map<ValueNumber, AvailableLoad> getPreviouslyKnownAs() {
		return previouslyKnownAs;
	}
	private Map<ValueNumber, AvailableLoad> getUpdateablePreviouslyKnownAs() {
		if (!(previouslyKnownAs instanceof HashMap))
			previouslyKnownAs = new HashMap<ValueNumber, AvailableLoad>(4);
		
		
		return previouslyKnownAs;
	}
    private  void assignPreviouslyKnownAs(Map<ValueNumber, AvailableLoad> newValue) {
        previouslyKnownAs = new HashMap<ValueNumber, AvailableLoad>(newValue);
    }
	
}

// vim:ts=4
