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

import edu.umd.cs.findbugs.model.MovedClassMap;

/**
 * Very sloppy bug comparator: if the warnings are of the same type,
 * and in the same class/method/field, assume they are the same.
 * 
 * @author David Hovemeyer
 */
public class SloppyBugComparator implements  WarningComparator {
	
	private MovedClassMap classNameRewriter;
	
	/**
	 * Constructor.
	 */
	public SloppyBugComparator() {
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.WarningComparator#setClassNameRewriter(edu.umd.cs.findbugs.model.MovedClassMap)
	 */
	public void setClassNameRewriter(MovedClassMap classNameRewriter) {
		this.classNameRewriter = classNameRewriter;
	}
	
	private int compareAllowingNull(BugAnnotation lhs, BugAnnotation rhs) {
		if (lhs == null || rhs == null) {
			if (lhs == null && rhs == null)
				return 0;
			else
				return (lhs == null) ? -1 : 1;
		}
		return lhs.compareTo(rhs);
	}

	/**
	 * Compare class annotations.
	 * 
	 * @param lhs left hand class annotation
	 * @param rhs right hand class annotation
	 * @return comparison of the class annotations
	 */
	private int compareClassesAllowingNull(ClassAnnotation lhs, ClassAnnotation rhs) {
		if (lhs == null || rhs == null) {
			if (lhs == null && rhs == null) {
				return 0;
			} else {
				return (lhs == null) ? -1 : 1;
			}
		}
		
		String lhsClassName = rewriteClassName(lhs.getClassName());
		String rhsClassName = rewriteClassName(rhs.getClassName());
		
		return lhsClassName.compareTo(rhsClassName);
	}
	
	/**
	 * If a class name rewriter is present, rewrite given class name.
	 * Otherwise, just return it as-is.
	 * 
	 * @param className a class name
	 * @return rewritten class name
	 */
	private String rewriteClassName(String className) {
		return classNameRewriter != null
				? classNameRewriter.rewriteClassName(className)
				: className;
	}

	/* (non-Javadoc)
	 * @see java.util.Comparator#compare(T, T)
	 */
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.WarningComparator#compare(edu.umd.cs.findbugs.BugInstance, edu.umd.cs.findbugs.BugInstance)
	 */
	public int compare(BugInstance lhs, BugInstance rhs) {
		int cmp;
		
		// Bug types must match
		cmp = lhs.getType().compareTo(rhs.getType());
		if (cmp != 0)
			return cmp;
		
		// Primary class must match
		cmp = compareClassesAllowingNull(lhs.getPrimaryClass(), rhs.getPrimaryClass());
		if (cmp != 0)
			return cmp;
		
		// Primary method must match (if any)
		cmp = compareAllowingNull(lhs.getPrimaryMethod(), rhs.getPrimaryMethod());
		if (cmp != 0)
			return cmp;
		
		// Primary field must match (if any)
		cmp = compareAllowingNull(lhs.getPrimaryField(), rhs.getPrimaryField());
		if (cmp != 0)
			return cmp;
		
		// Assume they're the same
		return 0;
	}
}
