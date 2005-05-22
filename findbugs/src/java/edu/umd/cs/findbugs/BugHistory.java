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
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.dom4j.DocumentException;

/**
 * Analyze bug results to find new, fixed, and retained bugs
 * between versions of the same program.  Uses VersionInsensitiveBugComparator
 * to determine when two BugInstances are the "same".
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
		 * <em>Important Note</em>: BugInstances should be cloned before putting them into
		 * the result Set. The BugHistory.cloneAll() static method may be used for this purpose.
		 * 
		 * @param result         Set to put the resulting BugInstances in
		 * @param origCollection original BugCollection
		 * @param newCollection  new BugCollection
		 */
		public void perform(Set<BugInstance> result,
				SortedBugCollection origCollection, SortedBugCollection newCollection);
	}
	
	/**
	 * Get the warnings which were <em>added</em>,
	 * meaning that they were not part of the original BugCollection.
	 * The BugInstances returned are from the new BugCollection.
	 */
	public static final SetOperation ADDED_WARNINGS = new SetOperation(){
		public void perform(Set<BugInstance> result,
				SortedBugCollection origCollection, SortedBugCollection newCollection) {
			cloneAll(result, newCollection.getCollection());
			result.removeAll(origCollection.getCollection());
		}
	};
	
	/**
	 * Get the warnings which were <em>retained</em>,
	 * meaning that they occur in both the original and new BugCollections.
	 * The BugInstances returned are from the new BugCollection.
	 */
	public static final SetOperation RETAINED_WARNINGS = new SetOperation(){
		public void perform(Set<BugInstance> result,
				SortedBugCollection origCollection, SortedBugCollection newCollection) {
			cloneAll(result, newCollection.getCollection());
			result.retainAll(origCollection.getCollection());
		}
	};
	
	/**
	 * Get the warnings which were <em>removed</em>,
	 * meaning that they occur in the original BugCollection but not in
	 * the new BugCollection.
	 * The BugInstances returned are from the original BugCollection.
	 */
	public static final SetOperation REMOVED_WARNINGS = new SetOperation(){
		public void perform(Set<BugInstance> result,
				SortedBugCollection origCollection, SortedBugCollection newCollection) {
			cloneAll(result, origCollection.getCollection());
			result.removeAll(newCollection.getCollection());
		}
	};
	
	private SortedBugCollection origCollection, newCollection;
	
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
	 * Perform a SetOperation.
	 * 
	 * @param operation the SetOperation
	 * @return the BugCollection resulting from performing the SetOperation
	 */
	public SortedBugCollection performSetOperation(SetOperation operation) {
		TreeSet<BugInstance> result = new TreeSet<BugInstance>(VersionInsensitiveBugComparator.instance());

		operation.perform(result, origCollection, newCollection);
		
		SortedBugCollection resultCollection = new SortedBugCollection();
		resultCollection.addAll(result);
		
		return resultCollection;
	}
	
	/**
	 * Clone all of the BugInstance objects in the source Collection
	 * and add them to the destination Collection.
	 * 
	 * @param dest   the destination Collection
	 * @param source the source Collection
	 */
	public static void cloneAll(Collection<BugInstance> dest, Collection<BugInstance> source) {
		for (Iterator<BugInstance> i = source.iterator(); i.hasNext(); ) {
			BugInstance obj = i.next();
			dest.add((BugInstance) obj.clone());
		}
	}

	public static void main(String[] argv) throws Exception {
		if (argv.length != 3) {
			System.err.println("Usage: " + BugHistory.class.getName() +
			        " <operation> <old results> <new results>\n" +
			        "Operations:\n" +
			        "   -added      Output added bugs (in new results but not in old results)\n" +
			        "   -new        Synonym for -added\n" +
			        "   -removed    Output removed bugs (in old results but not in new results)\n" +
			        "   -fixed      Synonym for -removed\n" +
			        "   -retained   Output retained bugs (in both old and new results)");
			System.exit(1);
		}

		Project project = new Project();

		String op = argv[0];
		SortedBugCollection origCollection = readCollection(argv[1], project);
		SortedBugCollection newCollection = readCollection(argv[2], new Project());

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
	
	private static SortedBugCollection readCollection(String fileName, Project project)
			throws IOException, DocumentException {
		SortedBugCollection result = new SortedBugCollection();
		result.readXML(fileName, project);
		return result;
	}
}

// vim:ts=4
