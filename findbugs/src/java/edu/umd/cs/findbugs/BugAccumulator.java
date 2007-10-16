/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

import java.util.LinkedList;

import edu.umd.cs.findbugs.util.MultiMap;

/**
 * Accumulate warnings that may occur at multiple source locations,
 * consolidating them into a single warning.
 * 
 * @author Bill Pugh
 * @author David Hovemeyer
 */
public class BugAccumulator {

	private BugReporter reporter;
	private MultiMap<BugInstance, SourceLineAnnotation> map;
	
	/**
	 * Constructor.
	 * 
	 * @param reporter the BugReporter to which warnings should eventually be reported
	 */
	public BugAccumulator(BugReporter reporter) {
		this.reporter = reporter;
		this.map = new MultiMap<BugInstance, SourceLineAnnotation>(LinkedList.class);
	}

	/**
	 * Accumulate a warning at given source location.
	 * 
	 * @param bug        the warning
	 * @param sourceLine the source location
	 */
	public void accumulateBug(BugInstance bug, SourceLineAnnotation sourceLine) {
		map.add(bug,sourceLine);
		
	}

	/**
	 * Accumulate a warning at source location currently being visited by 
	 * given BytecodeScanningDetector.
	 * 
	 * @param bug     the warning
	 * @param visitor the BytecodeScanningDetector
	 */
	public void accumulateBug(BugInstance bug, BytecodeScanningDetector visitor) {
		SourceLineAnnotation source = SourceLineAnnotation.fromVisitedInstruction(visitor);
		accumulateBug(bug, source);
	}
	

	public Iterable<? extends BugInstance> uniqueBugs() {
		return map.keySet();
	}
	
	public Iterable<? extends SourceLineAnnotation> locations(BugInstance bug) {
		return map.get(bug);
	}
	
	/**
	 * Report accumulated warnings to the BugReporter.
	 * Clears all accumulated warnings as a side-effect.
	 */
	public void reportAccumulatedBugs() {
		for(BugInstance bug : map.keySet()) {
			boolean first = true;
			for (SourceLineAnnotation source  : map.get(bug)) {
				if (source != null) {
					bug.addSourceLine(source);
					if (first) {
						first = false;
					} else {
						bug.describe(SourceLineAnnotation.ROLE_ANOTHER_INSTANCE);
					}
				}
			}
			reporter.reportBug(bug);
		}
		map.clear();
	}
}
