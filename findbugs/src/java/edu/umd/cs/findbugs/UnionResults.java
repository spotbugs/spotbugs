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

package edu.umd.cs.findbugs;

import java.io.IOException;
import java.util.*;

import org.dom4j.DocumentException;

/**
 * Compute the union of two sets of bug results,
 * preserving annotations.
 */
public class UnionResults {
	private SortedBugCollection origCollection;
	private SortedBugCollection newCollection;
	private Project project;

	public UnionResults(String origFilename, String newFilename) throws IOException, DocumentException {
		this(new SortedBugCollection(), new SortedBugCollection(), new Project());
		origCollection.readXML(origFilename, new Project());
		newCollection.readXML(newFilename, this.project);
	}

	public UnionResults(SortedBugCollection origCollection, SortedBugCollection newCollection, Project project) {
		this.origCollection = origCollection;
		this.newCollection = newCollection;
		this.project = project;
	}

	public Project getProject() {
		return project;
	}

	public SortedBugCollection execute() {
		DetectorFactoryCollection.instance(); // as a side effect, loads detector plugins

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
		if (argv.length != 3) {
			System.err.println("Usage: " + UnionResults.class.getName() + " <orig results> <new results> <output file>");
			System.exit(1);
		}

		String origFilename = argv[0];
		String newFilename = argv[1];
		String outputFilename = argv[2];

		UnionResults unionResults = new UnionResults(origFilename, newFilename);
		SortedBugCollection result = unionResults.execute();
		result.writeXML(outputFilename, unionResults.getProject());
	}

	private static SortedSet<BugInstance> createSet(SortedBugCollection bugCollection) {
		SortedSet<BugInstance> set = new TreeSet<BugInstance>();
		set.addAll(bugCollection.getCollection());
		return set;
	}
}

// vim:ts=3
