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

package edu.umd.cs.findbugs;

/**
 * Boolean-valued analysis properties for FindBugs.
 * 
 * @see edu.umd.cs.findbugs.ba.AnalysisContext#setBoolProperty(int, boolean)
 * @see edu.umd.cs.findbugs.ba.AnalysisContext#getBoolProperty(int)
 * @author David Hovemeyer
 */
public interface FindBugsAnalysisProperties {
	/**
	 * "Relaxed" warning reporting mode.
	 * Rather than using hard-coded heuristics to decide when
	 * to suppress a warning, report warnings freely and
	 * encode the heuristics as BugProperties (for consumption
	 * by a machine-learning-based ranking algorithm).
	 */
	public static final int RELAXED_REPORTING_MODE = 0; 
}
