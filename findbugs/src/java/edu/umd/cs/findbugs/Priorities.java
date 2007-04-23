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

package edu.umd.cs.findbugs;

/**
 * @author pugh
 */
public interface Priorities {
	/**
	 *  priority for bug instances that should be ignored
	 */
	public static final int IGNORE_PRIORITY = 5;	
	/**
	 * Experimental priority for bug instances.
	 */
	public static final int EXP_PRIORITY = 4;	

	/**
	 * Low priority for bug instances.
	 */
	public static final int LOW_PRIORITY = 3;

	/**
	 * Normal priority for bug instances.
	 */
	public static final int NORMAL_PRIORITY = 2;

	/**
	 * High priority for bug instances.
	 */
	public static final int HIGH_PRIORITY = 1;


}
