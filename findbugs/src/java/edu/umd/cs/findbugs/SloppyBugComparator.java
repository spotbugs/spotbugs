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

import java.io.Serializable;

import edu.umd.cs.findbugs.model.ClassNameRewriter;
import edu.umd.cs.findbugs.model.ClassNameRewriterUtil;
import edu.umd.cs.findbugs.model.IdentityClassNameRewriter;

/**
 * Very sloppy bug comparator: if the warnings are of the same type,
 * and in the same class/method/field, assume they are the same.
 * 
 * @author David Hovemeyer
 */
public class SloppyBugComparator implements  WarningComparator, Serializable {
	
	private static final boolean DEBUG = SystemProperties.getBoolean("sloppyComparator.debug");
	
	private static final long serialVersionUID = 1L;
	
	private ClassNameRewriter classNameRewriter = IdentityClassNameRewriter.instance();
	
	/**
	 * Constructor.
	 */
	public SloppyBugComparator() {
	}
	
	public void setClassNameRewriter(ClassNameRewriter classNameRewriter) {
		this.classNameRewriter = classNameRewriter;
	}
	
	private int compareNullElements(Object lhs, Object rhs) {
		if (lhs == null && rhs == null)
			return 0;
		else
			return (lhs == null) ? -1 : 1;
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
			return compareNullElements(lhs, rhs);
		}
		
		String lhsClassName = classNameRewriter.rewriteClassName(lhs.getClassName());
		String rhsClassName = classNameRewriter.rewriteClassName(rhs.getClassName());
		
		if (DEBUG) System.err.println("Comparing " + lhsClassName + " and " + rhsClassName);
		
		int cmp = lhsClassName.compareTo(rhsClassName);
		if (DEBUG) System.err.println("\t==> " + cmp);
		return cmp;
	}
	
	private int compareMethodsAllowingNull(MethodAnnotation lhs, MethodAnnotation rhs) {
		if (lhs == null || rhs == null) {
			return compareNullElements(lhs, rhs);
		}

		lhs = convertMethod(lhs);
		rhs = convertMethod(rhs);
		
		return lhs.compareTo(rhs);
	}
	
	private int compareFieldsAllowingNull(FieldAnnotation lhs, FieldAnnotation rhs) {
		if (lhs == null || rhs == null) {
			return compareNullElements(lhs, rhs);
		}

		lhs = convertField(lhs);
		rhs = convertField(rhs);
		
		if (DEBUG) System.err.println("Compare fields: " + lhs + " and " + rhs);
		
		return lhs.compareTo(rhs);
	}

	private MethodAnnotation convertMethod(MethodAnnotation methodAnnotation) {
		return ClassNameRewriterUtil.convertMethodAnnotation(classNameRewriter, methodAnnotation);
	}
	
	private FieldAnnotation convertField(FieldAnnotation fieldAnnotation) {
		return ClassNameRewriterUtil.convertFieldAnnotation(classNameRewriter, fieldAnnotation);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.WarningComparator#compare(edu.umd.cs.findbugs.BugInstance, edu.umd.cs.findbugs.BugInstance)
	 */
	public int compare(BugInstance lhs, BugInstance rhs) {
		
		int cmp;
		
		// Bug abbrevs must match
		BugPattern lhsPattern = lhs.getBugPattern();
		BugPattern rhsPattern = rhs.getBugPattern();
		String lhsAbbrev, rhsAbbrev;
		if (lhsPattern == null || rhsPattern == null) {
			lhsAbbrev = getAbbrevFromBugType(lhs.getType());
			rhsAbbrev = getAbbrevFromBugType(rhs.getType());
		} else {
			lhsAbbrev = lhsPattern.getAbbrev();
			rhsAbbrev = rhsPattern.getAbbrev();
		}
		cmp = lhsAbbrev.compareTo(rhsAbbrev);
		if (cmp != 0) {
			if (DEBUG) System.err.println("bug abbrevs do not match");
			return cmp;
		}
		
		// Primary class must match
		cmp = compareClassesAllowingNull(lhs.getPrimaryClass(), rhs.getPrimaryClass());
		if (cmp != 0)
			return cmp;
		
		// Primary method must match (if any)
		cmp = compareMethodsAllowingNull(lhs.getPrimaryMethod(), rhs.getPrimaryMethod());
		if (cmp != 0) {
			if (DEBUG) System.err.println("primary methods do not match");
			return cmp;
		}
		
		// Primary field must match (if any)
		cmp = compareFieldsAllowingNull(lhs.getPrimaryField(), rhs.getPrimaryField());
		if (cmp != 0) {
			if (DEBUG) System.err.println("primary fields do not match");
			return cmp;
		}
		
		// Assume they're the same
		return 0;
	}

	/**
	 * @param type
	 * @return
	 */
	private static String getAbbrevFromBugType(String type) {
		int bar = type.indexOf('_');
		return (bar >= 0) ? type.substring(0, bar) : "";	
	}
}
