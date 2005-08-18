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
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.dom4j.DocumentException;

import edu.umd.cs.findbugs.config.CommandLine;

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
	public static final int DEFAULT_COMPARATOR = 0;
	public static final int VERSION_INSENSITIVE_COMPARATOR = 1;
	public static final int FUZZY_COMPARATOR = 2;
	public static final int SLOPPY_COMPARATOR = 3;

	private SortedBugCollection origCollection, newCollection;
	private Project project;
	private int comparatorType;
	private Comparator<BugInstance> comparator;

	private int numPreserved;
	private int numAlreadyAnnotated;
	private int numLost;
	private int numLostWithAnnotations;

	public MergeResults(String origFilename, String newFilename) throws IOException, DocumentException {
		this(new SortedBugCollection(), new SortedBugCollection(), new Project());
		origCollection.readXML(origFilename, new Project());
		newCollection.readXML(newFilename, this.project);
		
		this.comparatorType = DEFAULT_COMPARATOR;
	}

	public MergeResults(SortedBugCollection origCollection, SortedBugCollection newCollection, Project project) {
		this.origCollection = origCollection;
		this.newCollection = newCollection;
		this.project = project;
		this.comparatorType = DEFAULT_COMPARATOR;
	}
	
	public Comparator<BugInstance> getComparator() {
		if (comparator == null) {
			switch (comparatorType) {
			case DEFAULT_COMPARATOR:
				comparator = new SortedBugCollection.BugInstanceComparator();
				break;
			case VERSION_INSENSITIVE_COMPARATOR:
				comparator = VersionInsensitiveBugComparator.instance();
				break;
			case FUZZY_COMPARATOR:
				FuzzyBugComparator fuzzyComparator = new FuzzyBugComparator();
				fuzzyComparator.registerBugCollection(origCollection);
				fuzzyComparator.registerBugCollection(newCollection);
				comparator  = fuzzyComparator;
				break;
			case SLOPPY_COMPARATOR:
				comparator = new SloppyBugComparator();
				break;
			default:
				throw new IllegalStateException("Unknown comparator type: " + comparatorType);
			}
		}
		return comparator;
	}
	
	/**
	 * @param comparatorType The comparatorType to set.
	 */
	public void setComparatorType(int comparatorType) {
		this.comparatorType = comparatorType;
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
	
	static class MergeResultsCommandLine extends CommandLine {
		int comparatorType = DEFAULT_COMPARATOR;
		boolean updateCategories = false;
		
		MergeResultsCommandLine() {
			addSwitch("-vi", "use version-insensitive bug comparator");
			addSwitch("-fuzzy", "use fuzzy bug comparator");
			addSwitch("-updateCategories", "only update bug categories contained in new results");
			addSwitch("-sloppy", "use the sloppy bug comparator");
		}

		/**
		 * @return Returns the comparatorType.
		 */
		public int getComparatorType() {
			return comparatorType;
		}
		
		/**
		 * @return true if only bug categories from new results should be updated
		 */
		public boolean updateCategories() {
			return updateCategories;
		}
		
		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.config.CommandLine#handleOption(java.lang.String, java.lang.String)
		 */
		//@Override
		protected void handleOption(String option, String optionExtraPart) throws IOException {
			if (option.equals("-vi")) {
				comparatorType = VERSION_INSENSITIVE_COMPARATOR;
			} else if (option.equals("-fuzzy")) {
				comparatorType = FUZZY_COMPARATOR;
			} else if (option.equals("-updateCategories")) {
				updateCategories = true;
			} else if (option.equals("-sloppy")) {
				comparatorType = SLOPPY_COMPARATOR;
			} else {
				throw new IllegalArgumentException("Unexpected option: " + option);
			}
		}

		/* (non-Javadoc)
		 * @see edu.umd.cs.findbugs.config.CommandLine#handleOptionWithArgument(java.lang.String, java.lang.String)
		 */
		//@Override
		protected void handleOptionWithArgument(String option, String argument) throws IOException {
			throw new IllegalArgumentException("Unexpected option " + option);
		}
		
	}

	private SortedSet<BugInstance> createSet(BugCollection bugCollection) {
		TreeSet<BugInstance> set = new TreeSet<BugInstance>(getComparator());
		set.addAll(bugCollection.getCollection());
		return set;
	}

	public void execute() {
		DetectorFactoryCollection.instance(); // as a side effect, loads detector plugins

		SortedSet<BugInstance> origSet = createSet(origCollection);
		SortedSet<BugInstance> newSet = createSet(newCollection);

		for (BugInstance orig : origSet) {
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

	public static void main(String[] argv) throws Exception {
//		if (argv.length != 3) {
//			System.err.println("Usage: " + MergeResults.class.getName() + " <orig results> <new results> <output file>");
//			System.exit(1);
//		}

//		if (VERSION_INSENSITIVE) {
//			System.out.println("Using version-insensitive bug comparator");
//		}
//
//		String origResultsFile = argv[0];
//		String newResultsFile = argv[1];
//		String outputFile = argv[2];
		
		final MergeResultsCommandLine commandLine = new MergeResultsCommandLine();
		int argCount = commandLine.parse(argv);
		if (argv.length - argCount != 3) {
			System.out.println("Usage: " + MergeResults.class.getName() + " [options] <orig results> <new results> <output file>");
			System.out.println("Options:");
			commandLine.printUsage(System.out);
			System.exit(1);
		}

		String origResultsFile = argv[argCount++];
		String newResultsFile = argv[argCount++];
		String outputFile = argv[argCount++];

		MergeResults mergeResults = new MergeResults(origResultsFile, newResultsFile) {
			final Set<String> updateCategorySet = new HashSet<String>();

			protected boolean preserveUnconditionally(BugInstance bugInstance) {
				return commandLine.updateCategories() && !updateCategorySet.contains(bugInstance.getAbbrev());
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
				if (commandLine.updateCategories()) {
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
		
		mergeResults.setComparatorType(commandLine.getComparatorType());

		mergeResults.execute();

		System.out.println(mergeResults.getNumPreserved() + " preserved, " +
		        mergeResults.getNumAlreadyAnnotated() + " already annotated, " +
		        mergeResults.getNumLost() + " lost (" +
		        mergeResults.getNumLostWithAnnotations() + " lost with annotations)");

		SortedBugCollection result = mergeResults.getNewCollection();
		result.writeXML(outputFile, mergeResults.getProject());
	}
}

// vim:ts=4
