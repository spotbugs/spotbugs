/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004 University of Maryland
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

import java.util.Set;

/**
 * Graph IS2 (inconsistent synchronization) false positive rate
 * as a function of cutoff percent for number of unsynchronized accesses.
 * In theory, the smaller the number of unsynchronized accesses,
 * the more likely it is that any particular unsynchronized access
 * is a bug.
 */
public class GraphIS2FalsePositives extends QueryBugAnnotations {
	private int syncPercent;
	private int numSerious;
	private int numHarmless;
	private int numFalse;
	private int total;

	public static void main(String[] argv) throws Exception {
		new GraphIS2FalsePositives().execute(argv);
	}

	public void execute(String[] argv) throws Exception {
		if (argv.length != 3) {
			System.err.println("Usage: " + GraphIS2FalsePositives.class.getName() +
				" <min sync pct> <max sync pct> <filename>");
			System.exit(1);
		}

		int minSyncPercent = Integer.parseInt(argv[0]);
		int maxSyncPercent = Integer.parseInt(argv[1]);
		String filename = argv[2];

		// Match bug instances that have been annotated
		// to specify whether they are real bugs
		// (in our estimation)
		addKeyword("BUG");
		addKeyword("NOT_BUG");

		// Read in the bug collection
		SortedBugCollection bugCollection = new SortedBugCollection();
		bugCollection.readXML(filename, new Project());

		for (int i = minSyncPercent; i <= maxSyncPercent; ++i) {
			setParams(i);
			scan(bugCollection, filename);
			emitDataPoint();
		}
	}

	private void setParams(int syncPercent) {
		this.syncPercent = syncPercent;
		numSerious = numHarmless = numFalse = total = 0;
	}

	private void emitDataPoint() {
		System.out.println(syncPercent + "\t" + numSerious + "\t" + numHarmless + "\t" + numFalse + "\t" + total);
	}

	protected void match(BugInstance bugInstance, String filename) throws Exception {
		Set<String> words = bugInstance.getTextAnnotationWords();
		if (words.contains("BUG")) {
			if (words.contains("HARMLESS"))
				++numHarmless;
			else
				++numSerious;
		} else
			++numFalse;

		++total;
	}
}

// vim:ts=4
