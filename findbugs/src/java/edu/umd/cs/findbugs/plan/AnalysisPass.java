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

package edu.umd.cs.findbugs.plan;

import edu.umd.cs.findbugs.DetectorFactory;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * An analysis pass in the overall ExecutionPlan.
 * This is a list of Detectors to be applied to analyzed classes.
 *
 * @see ExecutionPlan
 * @author David Hovemeyer
 */
public class AnalysisPass {
	private LinkedList<DetectorFactory> factoryList;

	/**
	 * Constructor.
	 *
	 * Creates an empty analysis pass.
	 */
	public AnalysisPass() {
		this.factoryList = new LinkedList<DetectorFactory>();
	}

	/**
	 * Add a DetectorFactory to the end of the pass.
	 *
	 * @param factory the DetectorFactory
	 */
	public void addDetectorFactory(DetectorFactory factory) {
		factoryList.addLast(factory);
	}

	/**
	 * Add a DetectorFactory to the beginning of the pass.
	 *
	 * @param factory the DetectorFactory
	 */
	public void prependDetectorFactory(DetectorFactory factory) {
		factoryList.addFirst(factory);
	}

	/**
	 * Get the List of DetectorFactory objects in the pass.
	 */
	public List<DetectorFactory> getDetectorFactoryList() {
		return factoryList;
	}

	/**
	 * Get an Iterator over the DetectorFactory objects in the pass.
	 */
	public Iterator<DetectorFactory> detectorFactoryIterator() {
		return factoryList.iterator();
	}

	/**
	 * Clear out all of the DetectorFactory objects.
	 * This can be useful as part of re-ordering the
	 * DetectorFactory objects within the pass.
	 */
	public void clear() {
		factoryList.clear();
	}
}

// vim:ts=4
