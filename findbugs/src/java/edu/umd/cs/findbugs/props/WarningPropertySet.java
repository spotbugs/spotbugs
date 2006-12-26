/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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
package edu.umd.cs.findbugs.props;


import edu.umd.cs.findbugs.*;
import java.util.*;

/**
 * A Set of WarningProperty objects, each with an optional attribute Object.
 * A WarningPropertySet is useful for collecting heuristics to use in
 * the determination of whether or not a warning is a false positive,
 * or what the warning's priority should be.
 * 
 * @author David Hovemeyer
 */
public class WarningPropertySet implements Cloneable {
	private Map<WarningProperty, Object> map;

	@Override
         public String toString() {
		StringBuffer buf = new StringBuffer("{ ");
		for (Map.Entry<WarningProperty, Object> entry : map.entrySet()) {
			WarningProperty prop = entry.getKey();
			Object attribute = entry.getValue();
			buf.append("  ");
			buf.append(prop.getPriorityAdjustment());
			buf.append("\t");
			buf.append(prop.getName());
			buf.append("\t");
			buf.append(attribute);
			buf.append("\n");
		}
		buf.append("}\n");
		return buf.toString();
		}
	
	/**
	 * Constructor
	 * Creates empty object.
	 */
	public WarningPropertySet() {
		this.map = new HashMap<WarningProperty, Object>();
	}

	//@Override
	@Override
         public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}
	
	/**
	 * Add a warning property to the set.
	 * The warning implicitly has the boolean value "true"
	 * as its attribute.
	 * 
	 * @param prop the WarningProperty
	 * @return this object
	 */
	public WarningPropertySet addProperty(WarningProperty prop) {
		map.put(prop, Boolean.TRUE);
		return this;
	}

	/**
	 * Add a warning property and its attribute value.
	 * 
	 * @param prop  the WarningProperty
	 * @param value the attribute value
	 * @return this object
	 */
	public WarningPropertySet setProperty(WarningProperty prop, String value) {
		map.put(prop, value);
		return this;
	}
	
	/**
	 * Add a warning property and its attribute value.
	 * 
	 * @param prop  the WarningProperty
	 * @param value the attribute value
	 */
	public void setProperty(WarningProperty prop, Boolean value) {
		map.put(prop, value);
	}
	
	/**
	 * Return whether or not the set contains the given WarningProperty.
	 * 
	 * @param prop the WarningProperty
	 * @return true if the set contains the WarningProperty, false if not
	 */
	public boolean containsProperty(WarningProperty prop) {
		return map.keySet().contains(prop);
	}
	
	/**
	 * Check whether or not the given WarningProperty has the given
	 * attribute value.
	 * 
	 * @param prop  the WarningProperty
	 * @param value the attribute value
	 * @return true if the set contains the WarningProperty and has
	 *         an attribute equal to the one given, false otherwise
	 */
	public boolean checkProperty(WarningProperty prop, Object value) {
		Object attribute = getProperty(prop);
		return (attribute != null && attribute.equals(value));
	}
	
	/**
	 * Get the value of the attribute for the given WarningProperty.
	 * Returns null if the set does not contain the WarningProperty. 
	 * 
	 * @param prop the WarningProperty
	 * @return the WarningProperty's attribute value, or null if
	 *         the set does not contain the WarningProperty
	 */
	public Object getProperty(WarningProperty prop) {
		return map.get(prop);
	}
	
	/**
	 * Use the PriorityAdjustments specified by the set's WarningProperty
	 * elements to compute a warning priority from the given
	 * base priority.
	 * 
	 * @param basePriority the base priority
	 * @return the computed warning priority
	 */
	public int computePriority(int basePriority) {
		boolean relaxedReporting = FindBugsAnalysisFeatures.isRelaxedMode();
	
		boolean atLeastMedium = false;
		boolean falsePositive = false;
        boolean atMostLow = false;
		int priority = basePriority;
		if (!relaxedReporting) {
			for (WarningProperty warningProperty : map.keySet()) {
				PriorityAdjustment adj = warningProperty.getPriorityAdjustment();
				if (adj == PriorityAdjustment.FALSE_POSITIVE) {
					falsePositive = true;
				}else if (adj == PriorityAdjustment.RAISE_PRIORITY)
					
					--priority;
				else if (adj == PriorityAdjustment.RAISE_PRIORITY_TO_AT_LEAST_NORMAL) {
					
					--priority;
					atLeastMedium = true;
				} else if (adj == PriorityAdjustment.RAISE_PRIORITY_TO_HIGH) {
					
					return Detector.HIGH_PRIORITY;
				}else if (adj == PriorityAdjustment.LOWER_PRIORITY) {
					++priority;
                }else if (adj == PriorityAdjustment.AT_MOST_LOW) {
                    priority++;
                    atMostLow = true;
                }else if (adj == PriorityAdjustment.NO_ADJUSTMENT) {
                    assert true; // do nothing
               } else throw new IllegalStateException("Unknown priority " + adj);
                 
			}
			if (atMostLow) return Math.min(Math.max(Detector.LOW_PRIORITY, priority), Detector.EXP_PRIORITY);
			if (atLeastMedium && priority > Detector.NORMAL_PRIORITY) priority = Detector.NORMAL_PRIORITY;
			else if (falsePositive && !atLeastMedium) return Detector.EXP_PRIORITY+1;
			
				
			if (priority < Detector.HIGH_PRIORITY)
				priority = Detector.HIGH_PRIORITY;
			else if (priority > Detector.EXP_PRIORITY)
				priority = Detector.EXP_PRIORITY;
		}
		
		return priority;
	}

	/**
	 * Determine whether or not a warning with given priority
	 * is expected to be a false positive.
	 * 
	 * @param priority the priority
	 * @return true if the warning is expected to be a false positive, false if not
	 */
	public boolean isFalsePositive(int priority) {
		return priority > Detector.EXP_PRIORITY;
	}

	/**
	 * Decorate given BugInstance with properties.
	 * 
	 * @param bugInstance the BugInstance
	 */
	public void decorateBugInstance(BugInstance bugInstance) {
		for (Map.Entry<WarningProperty, Object> entry : map.entrySet()) {
			WarningProperty prop = entry.getKey();
			Object attribute = entry.getValue();
			if (attribute == null)
				attribute = "";
			bugInstance.setProperty(prop.getName(), attribute.toString());
		}
	}
}
