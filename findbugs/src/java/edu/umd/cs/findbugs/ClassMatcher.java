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

public class ClassMatcher implements Matcher {
	private static final boolean DEBUG = Boolean.getBoolean("filter.debug");

	private String className;

	public ClassMatcher(String className) {
		this.className = className;
	}

	public boolean match(BugInstance bugInstance) {
		ClassAnnotation primaryClassAnnotation = bugInstance.getPrimaryClass();
		String bugClassName = primaryClassAnnotation.getClassName();
		if (DEBUG) System.out.println("Compare " + bugClassName + " with " + className);

		return bugClassName.equals(className);
	}
}

// vim:ts=4
