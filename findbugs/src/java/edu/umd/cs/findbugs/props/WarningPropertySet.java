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

public class WarningPropertySet {
	private Map<WarningProperty, Object> map;
	
	public WarningPropertySet() {
		this.map = new HashMap<WarningProperty, Object>();
	}
	
	public void addProperty(WarningProperty prop) {
		map.put(prop, Boolean.TRUE);
	}

	public void setProperty(WarningProperty prop, String value) {
		map.put(prop, value);
	}
	
	public void setProperty(WarningProperty prop, Boolean value) {
		map.put(prop, value);
	}
	
	public boolean isPropertySet(WarningProperty prop) {
		return map.keySet().contains(prop);
	}
	
	public boolean checkProperty(WarningProperty prop, Object value) {
		Object attribute = getProperty(prop);
		return (attribute != null && attribute.equals(value));
	}
	
	public Object getProperty(WarningProperty prop) {
		return map.get(prop);
	}
	
	public int computePriority(int basePriority) {
		int priority = basePriority;
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
		
		return priority;
	}

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
