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

import java.util.Comparator;

/**
 * @author David Hovemeyer
 */
public class FuzzyBugComparator implements Comparator<BugInstance> {
	private SortedBugCollection bugCollection;
	
	public FuzzyBugComparator(SortedBugCollection bugCollection) {
		this.bugCollection = bugCollection;
	}
	
	public int compare(BugInstance o1, BugInstance o2) {
		
		
		// TODO
		return 0;
	}
	
	// Compare classes: either exact fully qualified name must match, or class hash must match
	
	// Compare methods: either exact name and signature must match, or method hash must match
}
