/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003, University of Maryland
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

package edu.umd.cs.daveho.ba;

/**
 * A control decision which resulted in information being gained
 * about whether a particular value is null or non-null.
 *
 * @see IsNullValue
 * @see IsNullValueAnalysis
 */
public class IsNullConditionDecision {
	private Location branchLocation;
	private ValueNumber value;
	private IsNullValue decision;

	/**
	 * Constructor.
	 * @param branchLocation the location of the branch
	 * @param value the ValueNumber for which we have new information
	 * @param decision the decision reached about the value;
	 *   i.e., whether it is null or not null
	 */
	public IsNullConditionDecision(Location branchLocation, ValueNumber value, IsNullValue decision) {
		this.branchLocation = branchLocation;
		this.value = value;
		this.decision = decision;
	}

	/** Get the branch location.  */
	public Location getBranchLocation() {
		return branchLocation;
	}

	/** Get the value about which the branch yields information.  */
	public ValueNumber getValue() {
		return value;
	}

	/** Get the decision reached about the value (whether it is null or not null).  */
	public IsNullValue getDecision() {
		return decision;
	}
}

// vim:ts=4
