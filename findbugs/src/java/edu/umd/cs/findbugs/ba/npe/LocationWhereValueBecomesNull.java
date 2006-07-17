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

package edu.umd.cs.findbugs.ba.npe;

import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;

/**
 * A Location where a particular value number becomes null.
 * 
 * @author David Hovemeyer
 */
public class LocationWhereValueBecomesNull implements Comparable<LocationWhereValueBecomesNull> {
	private Location location;
	private ValueNumber valueNumber;
	private InstructionHandle handle;
	
	/**
	 * Constructor.
	 * 
	 * @param location    the Location where a value becomes null
	 * @param valueNumber the value number
	 * @param handle      the instruction responsible for the value being null
	 */
	public LocationWhereValueBecomesNull(Location location, ValueNumber valueNumber, InstructionHandle handle) {
		this.location = location;
		this.valueNumber = valueNumber;
		this.handle = handle;
	}
	
	/**
	 * @return Returns the location.
	 */
	public Location getLocation() {
		return location;
	}
	
	/**
	 * @return Returns the valueNumber.
	 */
	public ValueNumber getValueNumber() {
		return valueNumber;
	}
	
	/**
	 * Get the handle of the instruction that is responsible for
	 * the value being null.
	 * 
	 * @return Returns the handle.
	 */
	public InstructionHandle getHandle() {
		return handle;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(LocationWhereValueBecomesNull o) {
		int cmp = this.location.compareTo(o.location);
		if (cmp != 0) {
			return cmp;
		}
		cmp = this.valueNumber.compareTo(o.valueNumber);
		if (cmp != 0) {
			return cmp;
		}
		return this.handle.getPosition() - o.handle.getPosition();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		LocationWhereValueBecomesNull other = (LocationWhereValueBecomesNull) obj;
		return this.location.equals(other.location)
			&& this.valueNumber.equals(other.valueNumber)
			&& this.handle == other.handle;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return location.hashCode() * 6563 + valueNumber.hashCode() * 101 + handle.getPosition();
	}
}
