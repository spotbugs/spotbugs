/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.xml.XMLOutput;

/**
 * Match bug instances having one of given codes or patterns.
 * 
 * @author rafal@caltha.pl
 */
public class BugMatcher implements Matcher {
	private static final boolean DEBUG = SystemProperties.getBoolean("filter.debug");

	private final StringSetMatch codes;

	private final StringSetMatch patterns;

	private final StringSetMatch categories;

	/**
	 * Constructor.
	 * 
	 * @param codes
	 *            comma-separated list of bug codes
	 * @param patterns
	 *            coma-separated list of bug patterns.
	 * @param categories
	 *            coma-separated list of bug categories.
	 */
	public BugMatcher(String codes, String patterns, String categories) {
		this.codes = new StringSetMatch(codes);
		this.patterns = new StringSetMatch(patterns);
		this.categories = new StringSetMatch(categories);
	}

	public boolean match(BugInstance bugInstance) {
		boolean result1 =  codes.match(bugInstance.getAbbrev());
		boolean result2 = patterns.match(bugInstance.getType());
		boolean result3 =categories.match(bugInstance.getBugPattern().getCategory());
		if (DEBUG) System.out.println("Matching " + bugInstance.getAbbrev() +"/" + bugInstance.getType() +"/" + bugInstance.getBugPattern().getCategory()+ " with " + this + ", result = " + result1 + "/" + result2 + "/" + result3);
		
		return result1 || result2 || result3;
	}

	@Override
    public int hashCode() {
		return codes.hashCode() + patterns.hashCode() + categories.hashCode();
	}
	@Override
    public boolean equals(Object o) {
		if (!(o instanceof BugMatcher)) return false;
		BugMatcher other = (BugMatcher) o;
		return codes.equals(other.codes) && patterns.equals(other.patterns) && categories.equals(other.categories);
	}
	public void writeXML(XMLOutput xmlOutput, boolean disabled) throws IOException {
		xmlOutput.startTag("Bug");
		if (disabled) xmlOutput.addAttribute("disabled", "true");
		
		addAttribute(xmlOutput, "code", codes);
		addAttribute(xmlOutput, "pattern", patterns);
		addAttribute(xmlOutput, "category", categories);
		xmlOutput.stopTag(true);
	}

	public void addAttribute(XMLOutput xmlOutput, String name, StringSetMatch matches) throws IOException {
		String value = matches.toString();
		if (value.length() != 0)
			xmlOutput.addAttribute(name, value);
	}
	@Override public String toString() {
		StringBuilder buf = new StringBuilder("Bug(");
		if (!codes.isEmpty())
			buf.append("code = \"").append(codes).append("\" ");
		if (!patterns.isEmpty())
			buf.append("pattern = \"").append(patterns).append("\" ");
		if (!categories.isEmpty())
			buf.append("category = \"").append(categories).append("\" ");
		buf.setLength(buf.length()-1);
		buf.append(")");
		return buf.toString();
	}

}
