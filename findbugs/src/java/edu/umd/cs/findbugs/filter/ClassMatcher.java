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

package edu.umd.cs.findbugs.filter;

import java.io.IOException;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;

public class ClassMatcher implements Matcher {
	private static final boolean DEBUG = SystemProperties.getBoolean("filter.debug");

	private NameMatch className;

	@Override
	public String toString() {
		return "Class(class=\"" + className.getValue() +"\")";
	}

	public ClassMatcher(String className) {
		this.className = new NameMatch(className);
	}

	public boolean match(BugInstance bugInstance) {
		ClassAnnotation primaryClassAnnotation = bugInstance.getPrimaryClass();
		String bugClassName = primaryClassAnnotation.getClassName();
		boolean result =  className.match(bugClassName);
		if (DEBUG) System.out.println("Matching " + bugClassName + " with " + className + ", result = " + result);
		return result;
	}
	public void writeXML(XMLOutput xmlOutput, boolean disabled) throws IOException {
		XMLAttributeList attributes = new XMLAttributeList().addAttribute("name", className.getSpec());
		if (disabled) attributes.addAttribute("disabled", "true");
		xmlOutput.openCloseTag("Class", attributes);
	}
}

// vim:ts=4
