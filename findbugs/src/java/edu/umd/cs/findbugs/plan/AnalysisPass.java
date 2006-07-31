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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector2;
import edu.umd.cs.findbugs.DetectorFactory;
import edu.umd.cs.findbugs.FindBugs2;

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
//	private Detector2[] detectorList;

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
	 * Get the number of detectors in this pass.
	 * 
	 * @return the number of detectors in this pass
	 */
	public int getNumDetectors() {
		return orderedFactoryList.size();
	}

	/**
	 * Instantiate all of the Detector2s in this pass and return
	 * them in a (correctly-ordered) array.
	 * 
	 * @param bugReporter the BugReporter
	 * @return array of Detector2s
	 */
	public Detector2[] instantiateDetector2sInPass(BugReporter bugReporter) {
		Detector2[] detectorList = new Detector2[getNumDetectors()];
		int count = 0;
		for (Iterator<DetectorFactory> j = iterator(); j.hasNext();) {
			detectorList[count++] = j.next().createDetector2(bugReporter);
		}
		return detectorList;
	}
}

// vim:ts=4
