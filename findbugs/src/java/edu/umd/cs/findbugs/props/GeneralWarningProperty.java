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
public class GeneralWarningProperty extends AbstractWarningProperty {
	private GeneralWarningProperty(String name, PriorityAdjustment priorityAdjustment) {
		super(name, priorityAdjustment);
	}

	/** The type of the receiver object in a method call or instance field access. */
	public static final GeneralWarningProperty RECEIVER_OBJECT_TYPE =
		new GeneralWarningProperty("RECEIVER_OBJECT_TYPE", PriorityAdjustment.NO_ADJUSTMENT);

	/** Name of most recently called method. */ 
	public static final GeneralWarningProperty CALLED_METHOD_1 =
		new GeneralWarningProperty("CALLED_METHOD_1", PriorityAdjustment.NO_ADJUSTMENT);
	/** Name of second-most recently called method. */ 
	public static final GeneralWarningProperty CALLED_METHOD_2 =
		new GeneralWarningProperty("CALLED_METHOD_2", PriorityAdjustment.NO_ADJUSTMENT);
	/** Name of third-most recently called method. */ 
	public static final GeneralWarningProperty CALLED_METHOD_3 =
		new GeneralWarningProperty("CALLED_METHOD_3", PriorityAdjustment.NO_ADJUSTMENT);
	/** Name of fourth-most recently called method. */ 
	public static final GeneralWarningProperty CALLED_METHOD_4 =
		new GeneralWarningProperty("CALLED_METHOD_4", PriorityAdjustment.NO_ADJUSTMENT);

	/** Warning occurs on an exception control path. */
	public static final GeneralWarningProperty ON_EXCEPTION_PATH =
		new GeneralWarningProperty("ON_EXCEPTION_PATH", PriorityAdjustment.NO_ADJUSTMENT);
	
	/** issue is in uncallable method */
	public static final GeneralWarningProperty IN_UNCALLABLE_METHOD =
		new GeneralWarningProperty("IN_UNCALLABLE_METHOD", PriorityAdjustment.AT_MOST_LOW);
	
}
