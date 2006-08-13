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
	private BitSet contents;
	private int numValueNumbers;
	
	public DefinitelyNullSet(int numValueNumbers) {
		this.contents = new BitSet();
		this.numValueNumbers  = numValueNumbers;
	}
	
	public NullnessValue getNulllessValue(ValueNumber valueNumber) {
		return getNullnessValue(valueNumber.getNumber());
	}
	
	private NullnessValue getNullnessValue(int vn) {
		int flags = 0;
		
		int start = getStartIndex(vn);
		for (int i = 0; i < NullnessValue.FLAGS_MAX; i++) {
			if (contents.get(start + i)) {
				flags |= (1 << i);
			}
		}
		
		return NullnessValue.fromFlags(flags);
	}

	public void setNullnessValue(ValueNumber valueNumber, NullnessValue nullnessValue) {
		int flags = nullnessValue.getFlags();

		int start = getStartIndex(valueNumber.getNumber());
		for (int i = 0; i < NullnessValue.FLAGS_MAX; i++) {
			contents.set(start + i, (flags & (1 << i)) != 0);
		}
	}
	
	public void clear() {
		contents.clear();
	}

	public void setTop() {
		contents.clear();
		contents.set(lastUsedBit());
	}
	
	public boolean isTop() {
		return contents.get(lastUsedBit());
	}
	
	public void setBottom() {
		contents.clear();
		contents.set(lastUsedBit() + 1);
	}
	
	public boolean isBottom() {
		return contents.get(lastUsedBit() + 1);
	}

	public boolean isValid() {
		return !(isTop() || isBottom());
	}
	
	public void makeSameAs(DefinitelyNullSet other) {
		contents.clear();
		contents.or(other.contents);
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
		this.contents.and(other.contents);
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

	private int getStartIndex(int vn) {
		return vn * (1 << NullnessValue.FLAGS_MAX);
	}

	private int lastUsedBit() {
		return numValueNumbers * NullnessValue.FLAGS_MAX;
	}
	
	private int topBit() {
		return lastUsedBit();
	}
	
	private int bottomBit() {
		return lastUsedBit() + 1;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return contents.hashCode();
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
		return this.contents.equals(other.contents);
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
			StringBuffer buf = new StringBuffer();
			boolean first = true;
			
			buf.append("{");
			
			for (int i = 0; i < numValueNumbers; i++)  {
				NullnessValue val = getNullnessValue(i);
				if (val.isDefinitelyNull() || val.isDefinitelyNotNull()) {
					if (first) {
						first = false;
					} else {
						buf.append(", ");
					}
					buf.append(i);
					buf.append("->");
					buf.append(val.toString());
				}
			}
			
			buf.append("}");
			
			return buf.toString();
		}
	}
}
