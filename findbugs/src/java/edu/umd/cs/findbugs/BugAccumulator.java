/*
 * FindBugs - Find Bugs in Java programs
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

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;


public class BugAccumulator {

	private BugReporter reporter;
	public BugAccumulator(	BugReporter reporter) {
		this.reporter = reporter;
	}

	private Map<BugInstance, LinkedList<SourceLineAnnotation>> map 
			= new HashMap<BugInstance, LinkedList<SourceLineAnnotation>>();


	public void accumulateBug(BugInstance bug, SourceLineAnnotation sourceLine) {
		LinkedList<SourceLineAnnotation> where = map.get(bug);
		if (where == null) {
			where = new LinkedList<SourceLineAnnotation>();
			map.put(bug,where);
		}
		where.add(sourceLine);
	}


	public void accumulateBug(BugInstance bug, BytecodeScanningDetector visitor) {
		SourceLineAnnotation source = SourceLineAnnotation.fromVisitedInstruction(visitor);
		accumulateBug(bug, source);
	}
	public void reportAccumulatedBugs() {
		for(Map.Entry<BugInstance,LinkedList<SourceLineAnnotation>> e : map.entrySet()) {
			BugInstance bug = e.getKey();
			boolean first = true;
			for(SourceLineAnnotation source : e.getValue())
				if (source != null) {
					bug.addSourceLine(source);
					if (first) first = false;
					else bug.describe(SourceLineAnnotation.ROLE_ANOTHER_INSTANCE);
				}
			reporter.reportBug(bug);
		}
		map.clear();
	}
}
