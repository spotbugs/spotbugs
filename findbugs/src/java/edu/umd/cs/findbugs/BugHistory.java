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

public class BugHistory {

	/**
	 * Compare bug instances by only those criteria which we would expect to
	 * remain constant between versions.
	 */
	private static class BugComparator implements Comparator<BugInstance> {
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

	public static void main(String[] argv) {
		if (argv.length != 2) {
			System.err.println("Usage: " + BugHistory.class.getName() + " <old results> <new results>");
			System.exit(1);
		}

		String oldResults = argv[0];
		String newResults = argv[1];
	}
}

// vim:ts=4
