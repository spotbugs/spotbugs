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

import edu.umd.cs.findbugs.ba.DataflowAnalysisException;

/**
 * A dataflow value that indicates what kind of return path
 * is possible at the current program location.
 * Either:
 * <ul>
 * <li>It is possible to return normally</li>
 * <li>It is not possible to return normally
 *     (i.e., an exception is guaranteed to be thrown)</li>
 * </ul>
 * 
 * @author David Hovemeyer
 */
public class ReturnPathType {
	private static final int CAN_RETURN_NORMALLY = 0;
	private static final int CANNOT_RETURN_NORMALLY = 1;
	private static final int TOP = 2;
	
	private int type;

	/**
	 * Constructor.
	 * Creates a top dataflow fact.
	 */
	public ReturnPathType() {
		type = TOP; 
	}
	
	/**
	 * @return true if the method can return normally at this
	 *          location, false otherwise
	 */
	public boolean canReturnNormally() throws DataflowAnalysisException {
		if (isTop()) {
			throw new DataflowAnalysisException();
		}
		return type == CAN_RETURN_NORMALLY;
	}
	
	/**
	 * Make this dataflow fact an exact copy of the other one.
	 * 
	 * @param other another dataflow fact
	 */
	public void copyFrom(ReturnPathType other) {
		this.type = other.type;
	}
	
	/**
	 * Set the dataflow fact to top.
	 */
	public void setTop() {
		type = TOP;
	}
	
	/**
	 * @return true if the dataflow fact is top, false otherwise
	 */
	public boolean isTop() {
		return type == TOP;
	}
	
	/**
	 * Set whether or not it is possible to return normally.
	 * 
	 * @param canReturnNormally true if the method can return normally at this
	 *                           location, false otherwise
	 */
	public void setCanReturnNormally(boolean canReturnNormally) {
		type = canReturnNormally ? CAN_RETURN_NORMALLY : CANNOT_RETURN_NORMALLY;
	}

	/**
	 * Merge this fact with given fact.
	 * 
	 * @param fact another dataflow fact
	 */
	public void mergeWith(ReturnPathType fact) {
		if (fact.isTop()) {
			// other fact is top: no change to this one
			return;
		} else if (this.isTop()) {
			// this fact is top: copy other fact
			this.copyFrom(fact);
		} else {
			// neither fact is top: as long as one of the two
			// facts represents a (possible) normal return, then the result
			// is a possible normal return
			if (fact.type == CAN_RETURN_NORMALLY) {
				this.type = CAN_RETURN_NORMALLY;
			}
		}
	}

	boolean sameAs(ReturnPathType other) {
		return this.type == other.type;
	}
}
