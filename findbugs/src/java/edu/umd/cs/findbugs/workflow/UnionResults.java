/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004, University of Maryland
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

package edu.umd.cs.findbugs.workflow;

import java.util.Iterator;
import java.util.Set;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.DetectorFactoryCollection;
import edu.umd.cs.findbugs.Project;
import edu.umd.cs.findbugs.SortedBugCollection;

/**
 * Compute the union of two sets of bug results,
 * preserving annotations.
 */
public class UnionResults {

	static {
		DetectorFactoryCollection.instance(); // as a side effect, loads detector plugins
	}
	static public SortedBugCollection union (SortedBugCollection origCollection, SortedBugCollection newCollection) {

		SortedBugCollection result = new SortedBugCollection();

		for (Iterator<BugInstance> i = origCollection.iterator(); i.hasNext();) {
			result.add(i.next());
		}

		for (Iterator<BugInstance> i = newCollection.iterator(); i.hasNext();) {
			BugInstance bugInstance = i.next();
			BugInstance matching = origCollection.getMatching(bugInstance);
			if (matching == null)
				result.add(bugInstance);
			else {
				// If all of the words in the new annotation are already
				// in the old annotation, don't combine the annotations.
				Set<String> oldWords = matching.getTextAnnotationWords();
				Set<String> newWords = bugInstance.getTextAnnotationWords();
				newWords.removeAll(oldWords);
				if (newWords.isEmpty())
					continue;

				// Combine the annotations
				StringBuffer buf = new StringBuffer();
				buf.append(matching.getAnnotationText());
				buf.append('\n');
				buf.append(bugInstance.getAnnotationText());
				bugInstance.setAnnotationText(buf.toString());

				result.remove(matching);
				result.add(bugInstance);
			}
		}

		return result;
	}

	public static void main(String[] argv) throws Exception {


		if (argv.length == 0) {
			System.err.println("Usage: " + UnionResults.class.getName() + " <results1> <results2> ... <resultsn>");
			System.exit(1);
		}

		SortedBugCollection results = new SortedBugCollection();
		results.readXML(argv[0], new Project());
		for(int i = 1; i < argv.length; i++) {
			SortedBugCollection more = new SortedBugCollection();
			more.readXML(argv[i], new Project());
			results = union(results, more);
		}
	
		results.writeXML(System.out, new Project());
	}


}

// vim:ts=3
