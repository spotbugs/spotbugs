/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2008, University of Maryland
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

package edu.umd.cs.findbugs.plan;

/**
 * An ordering constraint which must be taken into account
 * when Detectors are run.
 *
 * @see edu.umd.cs.findbugs.Plugin
 * @see edu.umd.cs.findbugs.plan.ExecutionPlan
 * @author David Hovemeyer
 */
public class DetectorOrderingConstraint {
	private DetectorFactorySelector earlier;
	private DetectorFactorySelector later;
	private boolean singleSource;

	public DetectorOrderingConstraint(DetectorFactorySelector earlier, DetectorFactorySelector later) {
		this.earlier = earlier;
		this.later = later;
	}

	/**
	 * Set whether or not this ordering constraint resulted from
	 * an ordering constraint having a single detector as its source
	 * (ealier detector).  Such constraints automatically enable
	 * the source (earlier) detector if the target (later)
	 * detector is enabled.
	 * 
	 * @return true if this edge has a single detector as its source (earlier detector)
	 */
	public void setSingleSource(boolean singleSource) {
		this.singleSource = singleSource;
	}
	
	/**
	 * Determine whether or not this ordering constraint resulted from
	 * an ordering constraint having a single detector as its source
	 * (ealier detector).  Such constraints automatically enable
	 * the source (earlier) detector if the target (later)
	 * detector is enabled.
	 * 
	 * @return true if this edge has a single detector as its source (earlier detector)
	 */
	public boolean isSingleSource() {
		return singleSource;
	}

	public DetectorFactorySelector getEarlier() {
		return earlier;
	}

	public DetectorFactorySelector getLater() {
		return later;
	}

	@Override
	public String toString() {
		return earlier.toString() + " -> " + later.toString();
	}
}

// vim:ts=4
