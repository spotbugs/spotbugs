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

import java.util.*;

/**
 * Graph IS2 (inconsistent synchronization) false positive rate
 * as a function of cutoff percent for number of unsynchronized accesses.
 * In theory, the smaller the number of unsynchronized accesses,
 * the more likely it is that any particular unsynchronized access
 * is a bug.
 */
public class GraphIS2FalsePositives extends QueryBugAnnotations {
	private static final int SERIOUS = 0;
	private static final int HARMLESS = 1;
	private static final int FALSE = 2;
	private static final int MISSED = 3;
	private static final int NON_SERIOUS_AVOIDED = 4;
	private static final int NUM_STATS = 5;

	private int syncPercent;
	private int[] stats = new int[NUM_STATS];
	private int total;

	public static void main(String[] argv) throws Exception {
		DetectorFactoryCollection.instance(); // load plugins
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
		Arrays.fill(stats, 0);
		this.total = 0;
	}

	private void emitDataPoint() {
		System.out.print(syncPercent + "\t");
		for (int i = 0; i < NUM_STATS; ++i)
			System.out.print(stats[i] + "\t");
		System.out.println(total);
	}

	protected void match(BugInstance bugInstance, String filename) throws Exception {
		if (!bugInstance.getAbbrev().equals("IS2"))
			return;

		int bugSyncPercent = -1;
		for (Iterator<BugAnnotation> i = bugInstance.annotationIterator(); i.hasNext();) {
			BugAnnotation annotation = i.next();
			if (!(annotation instanceof IntAnnotation))
				continue;
			if (annotation.getDescription().equals("INT_SYNC_PERCENT")) {
				bugSyncPercent = ((IntAnnotation) annotation).getValue();
				break;
			}
		}
		if (bugSyncPercent < 0)
			throw new IllegalStateException();
		//return;

		boolean wouldBeReported = bugSyncPercent >= syncPercent;

		int judgment;

		Set<String> words = bugInstance.getTextAnnotationWords();
		if (words.contains("BUG")) {
			/* Bug instance is at least technically a bug */
			if (words.contains("HARMLESS")) {
				/* Harmless bug */
				judgment = wouldBeReported ? HARMLESS : NON_SERIOUS_AVOIDED;
			} else {
				/* Serious, juicy bug */
				judgment = wouldBeReported ? SERIOUS : MISSED;
			}
		} else {
			/* False positive; bad if reported */
			judgment = wouldBeReported ? FALSE : NON_SERIOUS_AVOIDED;
		}

		++stats[judgment];
		++total;
	}
}

// vim:ts=4
