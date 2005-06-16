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
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

import edu.umd.cs.findbugs.ba.ClassHash;
import edu.umd.cs.findbugs.ba.MethodHash;

/**
 * A slightly more intellegent way of comparing BugInstances from two versions
 * to see if they are the "same".  Uses class and method hashes to try to
 * handle renamings, at least for simple cases.
 * 
 * @see edu.umd.cs.findbugs.BugInstance
 * @see edu.umd.cs.findbugs.VersionInsensitiveBugComparator
 * @author David Hovemeyer
 */
public class FuzzyBugComparator implements Comparator<BugInstance> {
	
	/**
	 * Filter ignored BugAnnotations from given Iterator.
	 */
	private static class FilteringBugAnnotationIterator implements Iterator<BugAnnotation> {
		
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
		
		// Bug types must match exactly.
		// TODO: might want to experiment with just matching abbreviation:
		// maybe annotations provide enough context
		// to detect when bug patterns are renamed.
		cmp = a.getType().compareTo(b.getType());
		if (cmp != 0)
			return cmp;
		
		Iterator<BugAnnotation> lhsIter = new FilteringBugAnnotationIterator(a.annotationIterator());
		Iterator<BugAnnotation> rhsIter = new FilteringBugAnnotationIterator(b.annotationIterator());
		
		while (lhsIter.hasNext() && rhsIter.hasNext()) {
			BugAnnotation lhs = lhsIter.next();
			BugAnnotation rhs = rhsIter.next();

			// Annotation classes must match exactly
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
	 * Compare source line annotations.
	 * FIXME: decide if there is an easy way to handle changes in bytecode offsets.
	 * 
	 * @param lhs a SourceLineAnnotation
	 * @param rhs another SourceLineAnnotation
	 * @return comparison of lhs and rhs
	 */
	public int compareSourceLines(SourceLineAnnotation lhs, SourceLineAnnotation rhs) {
		return lhs.getStartBytecode() - rhs.getStartBytecode();
	}
	
	// See "FindBugsAnnotationDescriptions.properties"
	private static final HashSet<String> significantDescriptionSet = new HashSet<String>();
	static {
		// Classes, methods, and fields are significant.
		significantDescriptionSet.add("CLASS_DEFAULT");
		significantDescriptionSet.add("CLASS_EXCEPTION");
		significantDescriptionSet.add("CLASS_REFTYPE");
		significantDescriptionSet.add("INTERFACE_TYPE");
		significantDescriptionSet.add("METHOD_DEFAULT");
		significantDescriptionSet.add("METHOD_CALLED");
		significantDescriptionSet.add("METHOD_DANGEROUS_TARGET"); // but do NOT use safe targets
		significantDescriptionSet.add("METHOD_DECLARED_NONNULL");
		significantDescriptionSet.add("FIELD_DEFAULT");
		significantDescriptionSet.add("FIELD_ON");
		significantDescriptionSet.add("FIELD_SUPER");
		significantDescriptionSet.add("FIELD_MASKED");
		significantDescriptionSet.add("FIELD_MASKING");
		// Many int annotations are NOT significant: e.g., sync %, biased locked %, bytecode offset, etc.
		// The null parameter annotations, however, are definitely significant.
		significantDescriptionSet.add("INT_NULL_ARG");
		significantDescriptionSet.add("INT_MAYBE_NULL_ARG");
		significantDescriptionSet.add("INT_NONNULL_PARAM");
		// Only DEFAULT source line annotations are significant.
		significantDescriptionSet.add("SOURCE_LINE_DEFAULT");
	}
	
	public static boolean ignore(BugAnnotation annotation) {
		return !significantDescriptionSet.contains(annotation.getDescription());
	}
}
