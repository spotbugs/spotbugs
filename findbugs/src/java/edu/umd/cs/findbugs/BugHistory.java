/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005, University of Maryland
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
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Set;
import java.util.TreeSet;

import org.dom4j.DocumentException;

/**
 * Analyze bug results to find new, fixed, and retained bugs
 * between versions of the same program.  Uses VersionInsensitiveBugComparator
 * (or FuzzyBugComparator)
 * to determine when two BugInstances are the "same".
 * The new BugCollection returned is a deep copy of one of the input collections
 * (depending on the operation performed), with only a subset of the original
 * BugInstances retained.  Because it is a deep copy, it may be freely modified.
 * 
 * @author David Hovemeyer
 */
public class BugHistory {
	/**
	 * A set operation between two bug collections.
	 */
	public interface SetOperation {
		/**
		 * Perform the set operation.
		 * 
		 * @param result         Set to put the resulting BugInstances in
		 * @param origCollection original BugCollection
		 * @param newCollection  new BugCollection
		 * @returns the input bug collection the results are taken from
		 */
		public SortedBugCollection perform(Set<BugInstance> result,
				SortedBugCollection origCollection, SortedBugCollection newCollection);
	}
	
	/**
	 * Get the warnings which were <em>added</em>,
	 * meaning that they were not part of the original BugCollection.
	 * The BugInstances returned are from the new BugCollection.
	 */
	public static final SetOperation ADDED_WARNINGS = new SetOperation(){
		public SortedBugCollection perform(Set<BugInstance> result,
				SortedBugCollection origCollection, SortedBugCollection newCollection) {
			result.addAll(newCollection.getCollection());
			result.removeAll(origCollection.getCollection());
			return newCollection;
		}
	};
	
	/**
	 * Get the warnings which were <em>retained</em>,
	 * meaning that they occur in both the original and new BugCollections.
	 * The BugInstances returned are from the new BugCollection.
	 */
	public static final SetOperation RETAINED_WARNINGS = new SetOperation(){
		public SortedBugCollection perform(Set<BugInstance> result,
				SortedBugCollection origCollection, SortedBugCollection newCollection) {
			result.addAll(newCollection.getCollection());
			result.retainAll(origCollection.getCollection());
			return newCollection;
		}
	};
	
	/**
	 * Get the warnings which were <em>removed</em>,
	 * meaning that they occur in the original BugCollection but not in
	 * the new BugCollection.
	 * The BugInstances returned are from the original BugCollection.
	 */
	public static final SetOperation REMOVED_WARNINGS = new SetOperation(){
		public SortedBugCollection perform(Set<BugInstance> result,
				SortedBugCollection origCollection, SortedBugCollection newCollection) {
			result.addAll(origCollection.getCollection());
			result.removeAll(newCollection.getCollection());
			return origCollection;
		}
	};
	
	private SortedBugCollection origCollection, newCollection;
	private boolean useFuzzyComparator;
	private Comparator<BugInstance> comparator;
	
	/**
	 * Contructor.
	 * 
	 * @param origCollection the original BugCollection
	 * @param newCollection  the new BugCollection
	 */
	public BugHistory(SortedBugCollection origCollection, SortedBugCollection newCollection) {
		this.origCollection = origCollection;
		this.newCollection = newCollection;
	}
	
	/**
	 * Set whether or not to use the FuzzyBugComparator.
	 * 
	 * @param useFuzzyComparator true if we should use FuzzyBugComparator, false for
	 *         VersionInsensitiveBugComparator
	 */
	public void setUseFuzzyComparator(boolean useFuzzyComparator) {
		this.useFuzzyComparator = useFuzzyComparator;
	}

	/**
	 * Perform a SetOperation.
	 * 
	 * @param operation the SetOperation
	 * @return the BugCollection resulting from performing the SetOperation
	 */
	public SortedBugCollection performSetOperation(SetOperation operation) {
		// Create a result set which uses the version-insensitive/fuzzy bug comparator.
		// This will help figure out which bug instances are the "same"
		// between versions.
		TreeSet<BugInstance> result = new TreeSet<BugInstance>(getComparator());
		
		// Perform the operation, keeping track of which input BugCollection
		// should be cloned for metadata.
		SortedBugCollection originator = operation.perform(result, origCollection, newCollection);
		
		// Clone the actual BugInstances selected by the set operation.
		Collection<BugInstance> selected = new LinkedList<BugInstance>();
		BugCollection.cloneAll(selected, result);
		
		// Duplicate the collection from which the results came,
		// in order to copy all metadata, such as analysis errors,
		// class/method hashes, etc.
		SortedBugCollection resultCollection = originator.duplicate();
		
		// Replace with just the cloned instances of the subset selected by the set operation.
		resultCollection.clearBugInstances();
		resultCollection.addAll(selected);
		
		return resultCollection;
	}
	
	/**
	 * Get the Comparator used to compare BugInstances from different BugCollections.
	 */
	private Comparator<BugInstance> getComparator() {
		if (comparator == null) {
			if (useFuzzyComparator) {
				FuzzyBugComparator fuzzyComparator = new FuzzyBugComparator();
				fuzzyComparator.registerBugCollection(origCollection);
				fuzzyComparator.registerBugCollection(newCollection);
				comparator = fuzzyComparator;
			} else {
				comparator = VersionInsensitiveBugComparator.instance();
			}
		}
		return comparator;
	}

	public static void main(String[] argv) throws Exception {
		if (argv.length < 3) {
			printUsage();
		}

		Project project = new Project();
		
		boolean useFuzzyComparator = false;
		int argCount = 0;
		if (argv[argCount].equals("-fuzzy")) {
			if (argv.length < 4) {
				printUsage();
			}
			useFuzzyComparator = true;
			++argCount;
		}

		String op = argv[argCount++];
		SortedBugCollection origCollection = readCollection(argv[argCount++], project);
		SortedBugCollection newCollection = readCollection(argv[argCount++], new Project());

		SortedBugCollection result = null;
		BugHistory bugHistory = new BugHistory(origCollection, newCollection); 

		if (op.equals("-new") || op.equals("-added")) {
			result = bugHistory.performSetOperation(ADDED_WARNINGS);
		} else if (op.equals("-fixed") || op.equals("-removed")) {
			result = bugHistory.performSetOperation(REMOVED_WARNINGS);
		} else if (op.equals("-retained")) {
			result = bugHistory.performSetOperation(RETAINED_WARNINGS);
		} else
			throw new IllegalArgumentException("Unknown operation: " + op);

		result.writeXML(System.out, project);
	}

	/**
	 * Print usage and exit.
	 */
	private static void printUsage() {
		System.err.println("Usage: " + BugHistory.class.getName() +
		        " [options] <operation> <old results> <new results>\n" +
		        "Options:\n" +
		        "   -fuzzy      Use fuzzy bug comparison\n" +
		        "Operations:\n" +
		        "   -added      Output added bugs (in new results but not in old results)\n" +
		        "   -new        Synonym for -added\n" +
		        "   -removed    Output removed bugs (in old results but not in new results)\n" +
		        "   -fixed      Synonym for -removed\n" +
		        "   -retained   Output retained bugs (in both old and new results)");
		System.exit(1);
	}
	
	private static SortedBugCollection readCollection(String fileName, Project project)
			throws IOException, DocumentException {
		SortedBugCollection result = new SortedBugCollection();
		result.readXML(fileName, project);
		return result;
	}
}

// vim:ts=4
