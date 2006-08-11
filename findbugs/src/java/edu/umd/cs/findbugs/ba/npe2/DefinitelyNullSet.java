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

package edu.umd.cs.findbugs.ba.npe2;

import java.util.BitSet;

import edu.umd.cs.findbugs.ba.vna.ValueNumber;

/**
 * Set of values that is definitely known to be null.
 * 
 * @author David Hovemeyer
 */
public class DefinitelyNullSet extends BitSet {
	private int numValueNumbers;
	
	public DefinitelyNullSet(int numValueNumbers) {
		this.numValueNumbers  = numValueNumbers;
	}

	public void setTop() {
		clear();
		set(numValueNumbers);
	}
	
	public boolean isTop() {
		return get(numValueNumbers);
	}
	
	public void setBottom() {
		clear();
		set(numValueNumbers + 1);
	}
	
	public boolean isBottom() {
		return get(numValueNumbers + 1);
	}

	public boolean isValid() {
		return !(isTop() || isBottom());
	}
	
	public void makeSameAs(DefinitelyNullSet other) {
		clear();
		or(other);
	}
	
	public void mergeWith(DefinitelyNullSet other) {
		if (this.isBottom() || other.isTop()) {
			return;
		}
		
		if (this.isTop() || other.isBottom()) {
			this.makeSameAs(other);
			return;
		}
		
		// Result is intersection of sets
		this.and(other);
	}
	
	public BitSet getAssignedNullLocationSet(ValueNumber vn) {
		throw new UnsupportedOperationException();
	}

	public void addAssignedNullLocation(int valueNumber, int locationNumber) {
		// Base class does not maintain this information.
	}

	public void clearAssignNullLocations(int valueNumber) {
		// Base class does not maintain this information.
	}
}
