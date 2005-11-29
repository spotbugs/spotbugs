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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.DetectorFactory;

/**
 * An analysis pass in the overall ExecutionPlan.
 * This is a list of Detectors to be applied to analyzed classes.
 *
 * @see ExecutionPlan
 * @author David Hovemeyer
 */
public class AnalysisPass {
	private LinkedList<DetectorFactory> orderedFactoryList;
	private HashSet<DetectorFactory> memberSet;
	private Detector[] detectorList;

	/**
	 * Constructor.
	 *
	 * Creates an empty analysis pass.
	 */
	public AnalysisPass() {
		this.orderedFactoryList = new LinkedList<DetectorFactory>();
		this.memberSet = new HashSet<DetectorFactory>();
	}

	/**
	 * Make given DetectorFactory a member of this pass.
	 * Does not position the factory within the overall list of detectors.
	 * 
	 * @param factory a DetectorFactory
	 */
	public void addToPass(DetectorFactory factory) {
		this.memberSet.add(factory);
	}

	/**
	 * Append the given DetectorFactory to the end of the ordered detector list.
	 * The factory must be a member of the pass.
	 * 
	 * @param factory a DetectorFactory
	 */
	public void append(DetectorFactory factory) {
		if (!memberSet.contains(factory))
			throw new IllegalArgumentException("Detector " + factory.getFullName() + " appended to pass it doesn't belong to");
		this.orderedFactoryList.addLast(factory);
	}
	
	/**
	 * Get the members of this pass.
	 * 
	 * @return members of this pass
	 */
	public Collection<DetectorFactory> getMembers() {
		return memberSet;
	}
	
	/**
	 * Get Set of pass members which haven't been assigned a position in the pass.
	 */
	public Set<DetectorFactory> getUnpositionedMembers() {
		HashSet<DetectorFactory> result = new HashSet<DetectorFactory>(memberSet);
		result.removeAll(orderedFactoryList);
		return result;
	}

	/**
	 * Get an Iterator over the DetectorFactory objects in the pass,
	 * in their assigned order.
	 */
	public Iterator<DetectorFactory> iterator() {
		return orderedFactoryList.iterator();
	}
	
	/**
	 * Return whether or not this pass contains the given DetectorFactory.
	 * 
	 * @param factory the DetectorFactory
	 * @return true if this pass contains the DetectorFactory, false if not
	 */
	public boolean contains(DetectorFactory factory) {
		return memberSet.contains(factory);
	}
	
	/**
	 * Create all of the Detectors in this analysis pass.
	 * 
	 * @param bugReporter BugReporter to pass to the constructor of each created Detector
	 */
	public void createDetectors(BugReporter bugReporter) {
		ArrayList<Detector> detectorList = new ArrayList<Detector>();
		for (DetectorFactory factory : orderedFactoryList) {
			detectorList.add(factory.create(bugReporter));
		}
		this.detectorList = detectorList.toArray(new Detector[detectorList.size()]);
	}
	
	/**
	 * Get list of all Detectors.
	 * This should only be called after createDetectors() has been called.
	 */
	public Detector[] getDetectorList() {
		if (detectorList == null)
			throw new IllegalStateException("Detectors haven't been created yet");
		return detectorList;
	}
}

// vim:ts=4
