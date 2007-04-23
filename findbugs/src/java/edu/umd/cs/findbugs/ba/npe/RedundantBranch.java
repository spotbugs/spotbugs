/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003-2005 University of Maryland
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

import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.Location;

/**
 * An instruction recorded as a redundant reference comparison. We keep track of
 * the line number, in order to ensure that if the branch was duplicated, all
 * duplicates are determined in the same way. (If they aren't, then we don't
 * report it.)
 */
public class RedundantBranch {
	public final Location location;

	public final int lineNumber;

	public final IsNullValue firstValue, secondValue;

	public Edge infeasibleEdge;

	/**
	 * Constructor.
	 * 
	 * @param location
	 *            Location of ref comparison
	 * @param lineNumber
	 *            line number of ref comparison
	 * @param firstValue
	 * 			first value compared
	 * @param secondValue
	 * 			second value compared
	 */
	public RedundantBranch(Location location, int lineNumber,
			IsNullValue firstValue, IsNullValue secondValue) {
		this.location = location;
		this.lineNumber = lineNumber;
		this.firstValue = firstValue;
		this.secondValue = secondValue;
	}

	/**
	 * Constructor.
	 * 
	 * @param location
	 *            Location of ref comparison
	 * @param lineNumber
	 *            line number of ref comparison
	 * @param firstValue
	 * 			first value compared
	 */
	public RedundantBranch(Location location, int lineNumber,
			IsNullValue firstValue) {
		this.location = location;
		this.lineNumber = lineNumber;
		this.firstValue = firstValue;
		this.secondValue = null;
	}

	/**
	 * Set the edge which has been determined to be infeasible.
	 * 
	 * @param infeasibleEdge The infeasibleEdge to set.
	 */
	public void setInfeasibleEdge(Edge infeasibleEdge) {
		this.infeasibleEdge = infeasibleEdge;
	}

	@Override
		 public String toString() {
		return location.toString() + ": line " + lineNumber;
	}
}
