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

package edu.umd.cs.findbugs.ba;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Compute a compact numbering of Locations in a CFG.
 * This is useful for analyses that want to use a BitSet to
 * keep track of Locations.
 * 
 * @author David Hovemeyer
 */
public class CompactLocationNumbering {
	private HashMap<Location, Integer> locationToNumberMap;
	private HashMap<Integer, Location> numberToLocationMap;

	/**
	 * Constructor.
	 * 
	 * @param cfg the CFG containing the Locations to number
	 */
	public CompactLocationNumbering(CFG cfg) {
		this.locationToNumberMap = new HashMap<Location, Integer>();
		this.numberToLocationMap = new HashMap<Integer, Location>();
		build(cfg);
	}

	/**
	 * Get the size of the numbering,
	 * which is the maximum number assigned plus one.
	 * 
	 * @return the maximum number assigned plus one
	 */
	public int getSize() {
		return locationToNumberMap.size();
	}

	/**
	 * Get the number of given Location,
	 * which will be a non-negative integer
	 * in the range 0..getSize() - 1.
	 * 
	 * @param location
	 * @return the number of the location
	 */
	public int getNumber(Location location) {
		return locationToNumberMap.get(location).intValue();
	}

	/**
	 * Get the Location given its number.
	 * 
	 * @param number the number
	 * @return Location corresponding to that number
	 */
	public Location getLocation(int number) {
		return numberToLocationMap.get((Integer)(number));
	}

	private void build(CFG cfg) {
		int count = 0;
		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext(); ) {
			Integer number = (Integer)(count++);
			Location location = i.next();
			locationToNumberMap.put(location, number);
			numberToLocationMap.put(number, location);
		}
	}
}
