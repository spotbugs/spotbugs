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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.FindBugsAnalysisProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;

/**
 * A Set of WarningProperty objects, each with an optional attribute Object.
 * A WarningPropertySet is useful for collecting heuristics to use in
 * the determination of whether or not a warning is a false positive,
 * or what the warning's priority should be.
 * 
 * @author David Hovemeyer
 */
public class WarningPropertySet {
	private Map<WarningProperty, Object> map;


	public String toString() {
		StringBuffer buf = new StringBuffer("{ ");
		for (Iterator<Map.Entry<WarningProperty,Object>> i = map.entrySet().iterator();
				i.hasNext();) {
			Map.Entry<WarningProperty,Object> entry = i.next();
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
	
	/**
	 * Add a warning property to the set.
	 * The warning implicitly has the boolean value "true"
	 * as its attribute.
	 * 
	 * @param prop the WarningProperty
	 */
	public void addProperty(WarningProperty prop) {
		map.put(prop, Boolean.TRUE);
	}

	/**
	 * Add a warning property and its attribute value.
	 * 
	 * @param prop  the WarningProperty
	 * @param value the attribute value
	 */
	public void setProperty(WarningProperty prop, String value) {
		map.put(prop, value);
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
		boolean relaxedReporting = AnalysisContext.currentAnalysisContext().getBoolProperty(
				FindBugsAnalysisProperties.RELAXED_REPORTING_MODE);
		
		int priority = basePriority;
		if (!relaxedReporting) {
			for (Iterator<WarningProperty> i = map.keySet().iterator(); i.hasNext();) {
				PriorityAdjustment adj = i.next().getPriorityAdjustment();
				if (adj == PriorityAdjustment.FALSE_POSITIVE)
					return Detector.EXP_PRIORITY + 1;
				else if (adj == PriorityAdjustment.RAISE_PRIORITY)
					--priority;
				else if (adj == PriorityAdjustment.LOWER_PRIORITY)
					++priority;
			}
			
			if (priority < Detector.HIGH_PRIORITY)
				priority = Detector.HIGH_PRIORITY;
			else if (priority > Detector.EXP_PRIORITY)
				priority = Detector.EXP_PRIORITY;
		}
		
		return priority;
	}

	/**
	 * Decorate given BugInstance with properties.
	 * 
	 * @param bugInstance the BugInstance
	 */
	public void decorateBugInstance(BugInstance bugInstance) {
		for (Iterator<Map.Entry<WarningProperty,Object>> i = map.entrySet().iterator();
				i.hasNext();) {
			Map.Entry<WarningProperty,Object> entry = i.next();
			WarningProperty prop = entry.getKey();
			Object attribute = entry.getValue();
			if (attribute == null)
				attribute = "";
			bugInstance.setProperty(prop.getName(), attribute.toString());
		}
	}
}
