/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

import edu.umd.cs.findbugs.ba.ClassHash;
import edu.umd.cs.findbugs.ba.MethodHash;

/**
 * A slightly more intellegent way of comparing BugInstances from two versions
 * to see if they are the "same".  Uses class and method hashes to try to
 * handle renamings.
 * 
 * @see edu.umd.cs.findbugs.BugInstance
 * @see edu.umd.cs.findbugs.VersionInsensitiveBugComparator
 * @author David Hovemeyer
 */
public class FuzzyBugComparator implements Comparator<BugInstance> {
	
	/**
	 * Filter ignored BugAnnotations from given Iterator.
	 */
	class FilteringBugAnnotationIterator implements Iterator<BugAnnotation> {
		
		Iterator<BugAnnotation> iter;
		BugAnnotation next;
		
		public FilteringBugAnnotationIterator(Iterator<BugAnnotation> iter) {
			this.iter = iter;
		}
		
		private void findNext() {
			if (next == null) {
				while (iter.hasNext()) {
					BugAnnotation candidate = iter.next();
					if (!ignore(candidate)) {
						next = candidate;
						break;
					}
				}
			}
		}
		
		/* (non-Javadoc)
		 * @see java.util.Iterator#hasNext()
		 */
		public boolean hasNext() {
			findNext();
			return next != null;
		}
		
		/* (non-Javadoc)
		 * @see java.util.Iterator#next()
		 */
		public BugAnnotation next() {
			findNext();
			if (next == null)
				throw new NoSuchElementException();
			BugAnnotation result = next;
			next = null;
			return result;
		}
		
		/* (non-Javadoc)
		 * @see java.util.Iterator#remove()
		 */
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
	
	
	private SortedBugCollection bugCollection;
	
	public FuzzyBugComparator(SortedBugCollection bugCollection) {
		this.bugCollection = bugCollection;
	}
	
	public int compare(BugInstance a, BugInstance b) {
		int cmp;
		
		cmp = a.getType().compareTo(b.getType());
		if (cmp != 0)
			return cmp;
		
		Iterator<BugAnnotation> lhsIter = new FilteringBugAnnotationIterator(a.annotationIterator());
		Iterator<BugAnnotation> rhsIter = new FilteringBugAnnotationIterator(b.annotationIterator());
		
		while (lhsIter.hasNext() && rhsIter.hasNext()) {
			BugAnnotation lhs = lhsIter.next();
			BugAnnotation rhs = rhsIter.next();

			// Annotation classes must match
			cmp = lhs.getClass().getName().compareTo(rhs.getClass().getName());
			if (cmp != 0)
				return cmp;
			
			if (lhs.getClass() == ClassAnnotation.class)
				cmp = compareClasses((ClassAnnotation) lhs, (ClassAnnotation) rhs);
			else if (lhs.getClass() == MethodAnnotation.class)
				cmp = compareMethods((MethodAnnotation) lhs, (MethodAnnotation) rhs);
			else if (lhs.getClass() == SourceLineAnnotation.class)
				cmp = compareSourceLines((SourceLineAnnotation) lhs, (SourceLineAnnotation) rhs);
		}
		
		// TODO
		return 0;
	}

	private static <T> int compareNullElements(T a, T b) {
		if (a != null)
			return 1;
		else if (b != null)
			return -1;
		else
			return 0;
	}
	
	// Compare classes: either exact fully qualified name must match, or class hash must match
	public int compareClasses(ClassAnnotation lhsClass, ClassAnnotation rhsClass) {
		if (lhsClass == null || rhsClass == null) {
			return compareNullElements(lhsClass, rhsClass);
		}
		
		int cmp;
		
		// Compare by class name.  If same, great.
		cmp = lhsClass.compareTo(rhsClass);
		if (cmp == 0)
			return 0;
		
		// Get class hashes
		ClassHash lhsHash = bugCollection.getClassHash(lhsClass.getClassName());
		ClassHash rhsHash = bugCollection.getClassHash(rhsClass.getClassName());
		if (lhsHash == null || rhsHash == null)
			return cmp;
		
		return lhsHash.isSameHash(rhsHash) ? 0 : cmp;
	}
	
	// Compare methods: either exact name and signature must match, or method hash must match
	public int compareMethods(MethodAnnotation lhsMethod, MethodAnnotation rhsMethod) {
		if (lhsMethod == null || rhsMethod == null) {
			return compareNullElements(lhsMethod, rhsMethod);
		}

		// Compare for exact match
		int cmp = lhsMethod.compareTo(rhsMethod);
		if (cmp == 0)
			return 0;
		
		// Get class hashes for primary classes
		ClassHash lhsClassHash = bugCollection.getClassHash(lhsMethod.getClassName());
		ClassHash rhsClassHash = bugCollection.getClassHash(rhsMethod.getClassName());
		if (lhsClassHash == null || rhsClassHash == null)
			return cmp;
		
		// Look up method hashes
		MethodHash lhsHash = lhsClassHash.getMethodHash(lhsMethod.toXMethod());
		MethodHash rhsHash = rhsClassHash.getMethodHash(rhsMethod.toXMethod());
		if (lhsHash == null || rhsHash == null)
			return cmp;
		
		return lhsHash.isSameHash(rhsHash) ? 0 : cmp;
	}
	
	/**
	 * @param lhs
	 * @param rhs
	 * @return
	 */
	public int compareSourceLines(SourceLineAnnotation lhs, SourceLineAnnotation rhs) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	public boolean ignore(BugAnnotation annotation) {
		return false;
	}
}
