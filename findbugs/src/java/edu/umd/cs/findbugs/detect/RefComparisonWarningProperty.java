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
package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.props.PriorityAdjustment;
import edu.umd.cs.findbugs.props.WarningProperty;

/**
 * Warning properties for FindRefComparison detector.
 * 
 * @author David Hovemeyer
 */
public class RefComparisonWarningProperty implements WarningProperty {
	private String name;
	private PriorityAdjustment priorityAdjustment;
	
	private RefComparisonWarningProperty(String name, PriorityAdjustment priorityAdjustment) {
		this.name= name;
		this.priorityAdjustment = priorityAdjustment;
	}
	
	/** There is a call to equals() in the method. */
	public static final RefComparisonWarningProperty SAW_CALL_TO_EQUALS =
		new RefComparisonWarningProperty("SAW_CALL_TO_EQUALS", PriorityAdjustment.LOWER_PRIORITY);
	/** Method is private (or package-protected). */
	public static final RefComparisonWarningProperty PRIVATE_METHOD =
		new RefComparisonWarningProperty("PRIVATE_METHOD", PriorityAdjustment.LOWER_PRIORITY);
	/** Comparing static strings using equals operator. */
	public static final RefComparisonWarningProperty COMPARE_STATIC_STRINGS =
		new RefComparisonWarningProperty("COMPARE_STATIC_STRINGS", PriorityAdjustment.FALSE_POSITIVE);
	/** Comparing a dynamic string using equals operator. */
	public static final RefComparisonWarningProperty DYNAMIC_AND_UNKNOWN =
		new RefComparisonWarningProperty("DYNAMIC_AND_UNKNOWN", PriorityAdjustment.RAISE_PRIORITY);
	/** Comparing static string and an unknown string. */
	public static final RefComparisonWarningProperty STATIC_AND_UNKNOWN =
		new RefComparisonWarningProperty("STATIC_AND_UNKNOWN", PriorityAdjustment.LOWER_PRIORITY);
	/** Saw a call to String.intern(). */
	public static final RefComparisonWarningProperty SAW_INTERN =
		new RefComparisonWarningProperty("SAW_INTERN", PriorityAdjustment.LOWER_PRIORITY);
	
	public PriorityAdjustment getPriorityAdjustment() {
		return priorityAdjustment;
	}

	public String getName() {
		return this.getClass().getName() + "." + name;
	}

}
