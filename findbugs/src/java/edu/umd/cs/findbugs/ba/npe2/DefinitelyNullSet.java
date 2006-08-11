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

import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;

/**
 * Set of values that is definitely known to be null.
 * 
 * @author David Hovemeyer
 */
public class DefinitelyNullSet /*extends BitSet*/ {
	private BitSet definitelyNullSet;
	private int numValueNumbers;
	
	public DefinitelyNullSet(int numValueNumbers) {
		this.definitelyNullSet = new BitSet();
		this.numValueNumbers  = numValueNumbers;
	}
	
	public boolean isValueNull(ValueNumber valueNumber) throws DataflowAnalysisException {
		if (!isValid()) {
			throw new DataflowAnalysisException();
		}
		return definitelyNullSet.get(valueNumber.getNumber());
	}
	
	public void setValue(ValueNumber valueNumber, boolean isNull) throws DataflowAnalysisException  {
		if (!isValid()) {
			throw new DataflowAnalysisException();
		}
		definitelyNullSet.set(valueNumber.getNumber(), isNull);
	}
	
	public void clear() {
		definitelyNullSet.clear();
	}

	public void setTop() {
		definitelyNullSet.clear();
		definitelyNullSet.set(numValueNumbers);
	}
	
	public boolean isTop() {
		return definitelyNullSet.get(numValueNumbers);
	}
	
	public void setBottom() {
		definitelyNullSet.clear();
		definitelyNullSet.set(numValueNumbers + 1);
	}
	
	public boolean isBottom() {
		return definitelyNullSet.get(numValueNumbers + 1);
	}

	public boolean isValid() {
		return !(isTop() || isBottom());
	}
	
	public void makeSameAs(DefinitelyNullSet other) {
		definitelyNullSet.clear();
		definitelyNullSet.or(other.definitelyNullSet);
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
		this.definitelyNullSet.and(other.definitelyNullSet);
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
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return definitelyNullSet.hashCode();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null || obj.getClass() != this.getClass()) {
			return false;
		}
		
		DefinitelyNullSet other = (DefinitelyNullSet) obj;
		return this.definitelyNullSet.equals(other.definitelyNullSet);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		if (isTop()) {
			return "[TOP]";
		} else if (isBottom()) {
			return "[BOTTOM]";
		} else {
			return definitelyNullSet.toString();
		}
	}
}
