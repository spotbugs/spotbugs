/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, University of Maryland
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

	/**
	 * Compare bug instances by only those criteria which we would expect to
	 * remain constant between versions.
	 */
	private static class VersionInsensitiveBugComparator implements Comparator<BugInstance> {
		public int compare(BugInstance lhs, BugInstance rhs) {
			// Attributes of BugInstance.
			// Compare type and priority.
			// Compare class and method annotations (ignoring line numbers).
			// Compare field annotations.

			int cmp;

			cmp = lhs.getType().compareTo(rhs.getType());
			if (cmp != 0) return cmp;

			cmp = lhs.getPriority() - rhs.getPriority();
			if (cmp != 0) return cmp;

			Iterator<BugAnnotation> lhsIter = lhs.annotationIterator();
			Iterator<BugAnnotation> rhsIter = rhs.annotationIterator();

			while (lhsIter.hasNext() && rhsIter.hasNext()) {
				BugAnnotation lhsAnnotation = lhsIter.next();
				BugAnnotation rhsAnnotation = rhsIter.next();

				// Different annotation types obviously cannot be equal,
				// so just compare by class name.
				if (lhsAnnotation.getClass() != rhsAnnotation.getClass())
					return lhsAnnotation.getClass().getName().compareTo(rhsAnnotation.getClass().getName());

				if (lhsAnnotation.getClass() == ClassAnnotation.class ||
					lhsAnnotation.getClass() == MethodAnnotation.class ||
					lhsAnnotation.getClass() == FieldAnnotation.class) {
					// ClassAnnotations, MethodAnnotations, and FieldAnnotations
					// may all be compared directly.
					cmp = lhsAnnotation.compareTo(rhsAnnotation);
					if (cmp != 0) return cmp;
				} else if (lhsAnnotation.getClass() == SourceLineAnnotation.class) {
					// We assume that source lines may change, but source files will not.
					SourceLineAnnotation lhsSource = (SourceLineAnnotation) lhsAnnotation;
					SourceLineAnnotation rhsSource = (SourceLineAnnotation) rhsAnnotation;
					cmp = lhsSource.getSourceFile().compareTo(rhsSource.getSourceFile());
					if (cmp != 0) return cmp;
				} else if (lhsAnnotation.getClass() == IntAnnotation.class) {
					// Just ignore IntAnnotations.
				} else
					throw new IllegalStateException("Unknown annotation type: " + lhsAnnotation.getClass().getName());
			}

			if (rhsIter.hasNext())
				return -1;
			else if (lhsIter.hasNext())
				return 1;
			else
				return 0;
		}
	}

	/** The instance of the version-insensitive comparator. */
	private static final VersionInsensitiveBugComparator versionInsensitiveBugComparator =
		new VersionInsensitiveBugComparator();

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
		TreeSet<BugInstance> result = new TreeSet<BugInstance>(versionInsensitiveBugComparator);
		result.addAll(bugCollection.getCollection());
		return result;
	}
}

// vim:ts=4
