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
package edu.umd.cs.findbugs.props;

/**
 * General warning properties.
 * These are properties that could be attached to any warning
 * to provide information which might be useful in determining
 * whether or not the bug is a false positive, and/or the
 * severity of the warning.
 * 
 * @author David Hovemeyer
 */
public class GeneralWarningProperty implements WarningProperty {
	private String name;
	private PriorityAdjustment priorityAdjustment;
	
	private GeneralWarningProperty(String name, PriorityAdjustment priorityAdjustment) {
		this.name = name;
		this.priorityAdjustment = priorityAdjustment;
	}
	
	/** The type of the receiver object in a method call or instance field access. */
	public static final GeneralWarningProperty RECEIVER_OBJECT_TYPE =
		new GeneralWarningProperty("RECEIVER_OBJECT_TYPE", PriorityAdjustment.NO_ADJUSTMENT);

	public PriorityAdjustment getPriorityAdjustment() {
		return priorityAdjustment;
	}

	public String getName() {
		return this.getClass().getName() + "." + name;
	}

}
