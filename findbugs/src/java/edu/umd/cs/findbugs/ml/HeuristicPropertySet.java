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

package edu.umd.cs.findbugs.ml;

import java.util.BitSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import edu.umd.cs.findbugs.BugInstance;

/**
 * Set of boolean- and string-valued properties associated with
 * a FindBugs warning which represent heuristics we believe will
 * be useful in determining whether or not the warning is a real
 * bug or false.
 * 
 * @author David Hovemeyer
 */
public class HeuristicPropertySet {
	private BitSet boolPropertySet;
	private TreeMap<String,String> namedValueMap;
	
	public HeuristicPropertySet() {
		this.boolPropertySet = new BitSet();
		this.namedValueMap = new TreeMap<String,String>();
	}
	
	public void clear() {
		boolPropertySet.clear();
		namedValueMap.clear();
	}
	
	public void setBoolProperty(int boolPropertyIndex, boolean value) {
		boolPropertySet.set(boolPropertyIndex, value);
	}
	
	public void setStringProperty(String propertyName, String value) {
		namedValueMap.put(propertyName, value);
	}
	
	public boolean getBoolProperty(int boolPropertyIndex) {
		return boolPropertySet.get(boolPropertyIndex);
	}
	
	public String getStringProperty(String propertyName) {
		return namedValueMap.get(propertyName);
	}
	
	public void decorateBugInstance(BugInstance bugInstance, HeuristicPropertySetSchema schema) {
		for (int i = 0; i < schema.boolPropertyNameList.length; ++i) {
			boolean value = boolPropertySet.get(i);
			bugInstance.setProperty(schema.boolPropertyNameList[i], value ? "true" : "false");
		}
		for (Iterator<Map.Entry<String,String>> i = namedValueMap.entrySet().iterator(); i.hasNext();){
			Map.Entry<String,String> entry = i.next();
			bugInstance.setProperty(entry.getKey(), entry.getValue());
		}
	}
}
