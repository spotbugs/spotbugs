/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004, University of Maryland
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
 * An ordering constraint which must be taken into account
 * when Detectors are run.
 *
 * @see Plugin
 * @author David Hovemeyer
 */
public class DetectorOrderingConstraint {
	private String earlierDetector;
	private String laterDetector;

	/**
	 * Constructor.
	 * Defines a pair of Detectors with an ordering constraint.
	 *
	 * @param earlierDetector Detector that must be run earlier
	 * @param laterDetector   Detector that must be run later
	 */
	public DetectorOrderingConstraint(String earlierDetector, String laterDetector) {
		this.earlierDetector = earlierDetector;
		this.laterDetector = laterDetector;
	}

	/**
	 * Get the Detector in the pair of Detectors involved
	 * in the ordering constraint that must be run earlier.
	 *
	 * @return the earlier Detector
	 */
	public String getEarlierDetector() {
		return earlierDetector;
	}

	/**
	 * Get the Detector in the pair of Detectors involved
	 * in the ordering constraint that must be run later.
	 *
	 * @return the later Detector
	 */
	public String getLaterDetector() {
		return laterDetector;
	}
}

// vim:ts=4
