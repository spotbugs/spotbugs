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

import java.util.*;

/**
 * An implementation of {@link BugCollection} that keeps the BugInstances
 * sorted by class (using the native comparison ordering of BugInstance's
 * compareTo() method as a tie-breaker).
 *
 * @see BugInstance
 * @author David Hovemeyer
 */
public class SortedBugCollection extends BugCollection {
	private static class BugInstanceComparator implements Comparator<BugInstance> {
		public int compare(BugInstance lhs, BugInstance rhs) {
			ClassAnnotation lca = lhs.getPrimaryClass();
			ClassAnnotation rca = rhs.getPrimaryClass();
			if (lca == null || rca == null)
				throw new IllegalStateException("null class annotation: " + lca + "," + rca);
			int cmp = lca.getClassName().compareTo(rca.getClassName());
			if (cmp != 0)
				return cmp;
			return lhs.compareTo(rhs);
		}
	}

	private static final BugInstanceComparator comparator = new BugInstanceComparator();

	private TreeSet<BugInstance> bugSet;
	private List<String> errorList;
	private TreeSet<String> missingClassSet;
	private String summaryHTML;

	/**
	 * Constructor.
	 * Creates an empty object.
	 */
	public SortedBugCollection() {
		bugSet = new TreeSet<BugInstance>(comparator);
		errorList = new LinkedList<String>();
		missingClassSet = new TreeSet<String>();
		summaryHTML = "";
	}

	public boolean add(BugInstance bugInstance) {
		return bugSet.add(bugInstance);
	}

	public boolean remove(BugInstance bugInstance) {
		return bugSet.remove(bugInstance);
	}

	public Iterator<BugInstance> iterator() {
		return bugSet.iterator();
	}

	public Collection<BugInstance> getCollection() {
		return bugSet;
	}

	public void addError(String message) {
		errorList.add(message);
	}

	public void addMissingClass(String message) {
		missingClassSet.add(message);
	}

	public Iterator<String> errorIterator() {
		return errorList.iterator();
	}

	public Iterator<String> missingClassIterator() {
		return missingClassSet.iterator();
	}

	public boolean contains(BugInstance bugInstance) {
		return bugSet.contains(bugInstance);
	}

	public BugInstance getMatching(BugInstance bugInstance) {
		SortedSet<BugInstance> tailSet = bugSet.tailSet(bugInstance);
		if (tailSet.isEmpty())
			return null;
		BugInstance first = tailSet.first();
		return bugInstance.equals(first) ? first : null;
	}

	public void setSummaryHTML(String html) {
		this.summaryHTML = html;
	}

	public String getSummaryHTML() {
		return summaryHTML;
	}
}

// vim:ts=4
