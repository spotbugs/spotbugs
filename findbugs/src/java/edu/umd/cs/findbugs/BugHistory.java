/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

import java.util.*;

/**
 * Analyze bug results to find new, fixed, and retained bugs
 * between versions of the same program.  In order to determine that
 * bugs in different versions are the "same", we just eliminate source
 * lines from consideration when comparing bug instances.
 * This isn't guaranteed to do the right thing, so we might want
 * to make this determination more sophisticated in the future.
 */
public class BugHistory {

	public static void main(String[] argv) throws Exception {
		if (argv.length != 3) {
			System.err.println("Usage: " + BugHistory.class.getName() +
			        " <operation> <old results> <new results>\n" +
			        "Operations:\n" +
			        "   -new        Output new bugs (in new results but not in old results)\n" +
			        "   -fixed      Output fixed bugs (in old results but not in new results)\n" +
			        "   -retained   Output retained bugs (in both old and new results)");
			System.exit(1);
		}

		Project project = new Project();

		String op = argv[0];
		TreeSet<BugInstance> oldBugs = readSet(argv[1], project);
		TreeSet<BugInstance> newBugs = readSet(argv[2], new Project());

		SortedBugCollection result = new SortedBugCollection();

		if (op.equals("-new")) {
			newBugs.removeAll(oldBugs);
			result.addAll(newBugs);
		} else if (op.equals("-fixed")) {
			oldBugs.removeAll(newBugs);
			result.addAll(oldBugs);
		} else if (op.equals("-retained")) {
			oldBugs.retainAll(newBugs);
			result.addAll(oldBugs);
		} else
			throw new IllegalArgumentException("Unknown operation: " + op);

		result.writeXML(System.out, project);
	}

	private static TreeSet<BugInstance> readSet(String filename, Project project) throws Exception {
		SortedBugCollection bugCollection = new SortedBugCollection();
		bugCollection.readXML(filename, project);
		TreeSet<BugInstance> result = new TreeSet<BugInstance>(VersionInsensitiveBugComparator.instance());
		result.addAll(bugCollection.getCollection());
		return result;
	}
}

// vim:ts=4
