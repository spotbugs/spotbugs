/*
 * FindBugs - Find Bugs in Java programs
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

package edu.umd.cs.findbugs.props;

/**
 * Abstract base class for implementing warning properties.
 * 
 * @author David Hovemeyer
 */
public abstract class AbstractWarningProperty implements WarningProperty {
	private String shortName;
	private PriorityAdjustment priorityAdjustment;

	/**
	 * Constructor.
	 * 
	 * @param shortName          the short name of the property; will be qualified
	 *                           with the full name of the warning property class
	 * @param priorityAdjustment the priority adjustment
	 */
	protected AbstractWarningProperty(String shortName, PriorityAdjustment priorityAdjustment) {
		this.shortName = shortName;
		this.priorityAdjustment = priorityAdjustment;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.props.WarningProperty#getPriorityAdjustment()
	 */
	public PriorityAdjustment getPriorityAdjustment() {
		return priorityAdjustment;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.props.WarningProperty#getName()
	 */
	public String getName() {
		return this.getClass().getName() + "." + shortName;
	}

}
