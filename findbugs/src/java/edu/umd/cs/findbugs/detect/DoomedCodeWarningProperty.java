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

package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.props.AbstractWarningProperty;
import edu.umd.cs.findbugs.props.PriorityAdjustment;

/**
 * A WarningProperty for warnings that are reported at a "doomed" Location;
 * one that cannot return normally.
 * 
 * @author David Hovemeyer
 */
public class DoomedCodeWarningProperty extends AbstractWarningProperty {
	private DoomedCodeWarningProperty(String shortName, PriorityAdjustment priorityAdjustment) {
		super(shortName, priorityAdjustment);
	}

	/**
	 * Singleton instance of DoomedCodeWarningProperty. 
	 */
	public static DoomedCodeWarningProperty DOOMED_CODE =
		new DoomedCodeWarningProperty("DOOMED_CODE", PriorityAdjustment.NO_ADJUSTMENT);
}
