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

public class MergeResults {
	public static void main(String[] argv) throws Exception {

		if (argv.length != 2) {
			System.err.println("Usage: " + MergeResults.class.getName() + " <orig results> <new results>");
			System.exit(1);
		}

		String origResultsFile = argv[0];
		String newResultsFile = argv[1];

		HashMap<String, String> classToSourceFileMap = new HashMap<String, String>();

		SortedBugCollection origCollection = new SortedBugCollection();
		SortedBugCollection newCollection = new SortedBugCollection();

		origCollection.readXML(origResultsFile, new HashMap<String,String>());
		newCollection.readXML(newResultsFile, classToSourceFileMap);

		Iterator<BugInstance> i = origCollection.iterator();
		while (i.hasNext()) {
			BugInstance orig = i.next();
			if (newCollection.contains(orig)) {
				BugInstance matching = newCollection.getMatching(orig);
				matching.setAnnotationText(orig.getAnnotationText());
			}
		}

		newCollection.writeXML(System.out, classToSourceFileMap);

	}
}

// vim:ts=4
