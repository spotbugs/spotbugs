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

import java.io.IOException;
import java.util.*;

import org.dom4j.DocumentException;

/**
 * Merge a saved results file (containing annotations) with a new results file.
 * This is useful when re-running FindBugs after changing the detectors
 * (e.g., to fix false positives).  All of the annotations from the original
 * run for bugs still present in the new run are preserved in the output file
 * (whose bugs are identical to the new run).  Note that some annotations
 * can be lost, if those bugs are not present in the new run.
 *
 * @author David Hovemeyer
 */
public class MergeResults {
	private static final boolean VERSION_INSENSITIVE = Boolean.getBoolean("mergeResults.vi");
	private static final boolean UPDATE_CATEGORIES = Boolean.getBoolean("mergeResults.update");

	private SortedBugCollection origCollection, newCollection;
	private Project project;

	private int numPreserved;
	private int numAlreadyAnnotated;
	private int numLost;
	private int numLostWithAnnotations;

	public MergeResults(String origFilename, String newFilename) throws IOException, DocumentException {
		this(new SortedBugCollection(), new SortedBugCollection(), new Project());
		origCollection.readXML(origFilename, new Project());
		newCollection.readXML(newFilename, this.project);
	}

	public MergeResults(SortedBugCollection origCollection, SortedBugCollection newCollection, Project project) {
		this.origCollection = origCollection;
		this.newCollection = newCollection;
		this.project = project;
	}

	public SortedBugCollection getOrigCollection() {
		return origCollection;
	}

	public SortedBugCollection getNewCollection() {
		return newCollection;
	}

	public Project getProject() {
		return project;
	}

	public int getNumPreserved() {
		return numPreserved;
	}

	public int getNumAlreadyAnnotated() {
		return numAlreadyAnnotated;
	}

	public int getNumLost() {
		return numLost;
	}

	public int getNumLostWithAnnotations() {
		return numLostWithAnnotations;
	}

	protected boolean preserveUnconditionally(BugInstance bugInstance) {
		return false;
	}

	protected void lostWithAnnotation(BugInstance bugInstance) {
	}

	public static void main(String[] argv) throws Exception {
		if (argv.length != 3) {
			System.err.println("Usage: " + MergeResults.class.getName() + " <orig results> <new results> <output file>");
			System.exit(1);
		}

		if (VERSION_INSENSITIVE) {
			System.out.println("Using version-insensitive bug comparator");
		}

		String origResultsFile = argv[0];
		String newResultsFile = argv[1];
		String outputFile = argv[2];

		MergeResults mergeResults = new MergeResults(origResultsFile, newResultsFile) {
			final Set<String> updateCategorySet = new HashSet<String>();

			protected boolean preserveUnconditionally(BugInstance bugInstance) {
				return UPDATE_CATEGORIES && !updateCategorySet.contains(bugInstance.getAbbrev());
			}

			protected void lostWithAnnotation(BugInstance bugInstance) {
				System.out.println("Losing a bug with an annotation:");
				System.out.println(bugInstance.getMessage());
				SourceLineAnnotation srcLine = bugInstance.getPrimarySourceLineAnnotation();
				if (srcLine != null)
					System.out.println("\t" + srcLine.toString());
				System.out.println(bugInstance.getAnnotationText());
			}

			public void execute() {
				if (UPDATE_CATEGORIES) {
					DetectorFactoryCollection.instance(); // as a side effect, loads detector plugins
					for (Iterator<BugInstance> i = getNewCollection().iterator(); i.hasNext();) {
						// All bugs not in categories contained in the
						// original set will be preserved unconditionally.
						updateCategorySet.add(i.next().getAbbrev());
					}
					System.out.println("Updating only categories: " + updateCategorySet);
				}
				super.execute();
			}
		};

		mergeResults.execute();

		System.out.println(mergeResults.getNumPreserved() + " preserved, " +
		        mergeResults.getNumAlreadyAnnotated() + " already annotated, " +
		        mergeResults.getNumLost() + " lost (" +
		        mergeResults.getNumLostWithAnnotations() + " lost with annotations)");

		SortedBugCollection result = mergeResults.getNewCollection();
		result.writeXML(outputFile, mergeResults.getProject());
	}

	public void execute() {
		DetectorFactoryCollection.instance(); // as a side effect, loads detector plugins

		SortedSet<BugInstance> origSet = createSet(origCollection);
		SortedSet<BugInstance> newSet = createSet(newCollection);

		Iterator<BugInstance> i = origSet.iterator();
		while (i.hasNext()) {
			BugInstance orig = i.next();

			if (preserveUnconditionally(orig)) {
				// This original bug is not in a category that we are updating.
				// Therefore, it is copied into the results unconditionally.
				numPreserved++;
				newCollection.add(orig);
			} else {
				// This original bug is in an updated category.
				// So, to be preserved, it must also be in the new set.

				if (newSet.contains(orig)) {
					SortedSet<BugInstance> tailSet = newSet.tailSet(orig);
					BugInstance matching = tailSet.first();
					if (matching.getAnnotationText().equals("")) {
						// Copy annotation text from original results to results
						matching.setAnnotationText(orig.getAnnotationText());
						numPreserved++;
					} else {
						numAlreadyAnnotated++;
					}
				} else {
					numLost++;
					if (!orig.getAnnotationText().equals("")) {
						lostWithAnnotation(orig);
						numLostWithAnnotations++;
					}
				}
			}
		}
	}

	private static SortedSet<BugInstance> createSet(BugCollection bugCollection) {
		TreeSet<BugInstance> set = VERSION_INSENSITIVE
		        ? new TreeSet<BugInstance>(VersionInsensitiveBugComparator.instance())
		        : new TreeSet<BugInstance>();
		set.addAll(bugCollection.getCollection());
		return set;
	}
}

// vim:ts=4
