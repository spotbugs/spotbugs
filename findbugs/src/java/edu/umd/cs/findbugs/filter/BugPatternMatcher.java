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
package edu.umd.cs.findbugs.filter;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import edu.umd.cs.findbugs.BugInstance;

/**
 * Matcher class to check whether or not BugInstances have
 * one of a particular set of bug pattern types.
 * 
 * @author David Hovemeyer
 */
public class BugPatternMatcher implements Matcher {
	private Set<String> bugPatternSet;
	
	/**
	 * Constructor.
	 * 
	 * @param bugPatternTypes comma-separated list of bug pattern to check for 
	 */
	public BugPatternMatcher(String bugPatternTypes) {
		this.bugPatternSet = new HashSet<String>();
		StringTokenizer tok = new StringTokenizer(bugPatternTypes, ", \t");
		while (tok.hasMoreTokens()) {
			bugPatternSet.add(tok.nextToken());
		}
	}

	public boolean match(BugInstance bugInstance) {
		return bugPatternSet.contains(bugInstance.getType());
	}
}
