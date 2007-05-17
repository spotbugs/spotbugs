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
import java.util.Arrays;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.xml.XMLOutput;

/**
 * Match bug instances having one of given codes or patterns.
 * 
 * @author rafal@caltha.pl
 */
public class DesignationMatcher implements Matcher {
	private StringSetMatch designations;



	/**
	 * Constructor.
	 * 
	 * @param designations
	 *            comma-separated list of designations
	 * @param patterns
	 *            coma-separated list of bug patterns.
	 * @param categories
	 *            coma-separated list of bug categories.
	 */
	public DesignationMatcher(String designations) {
		this.designations = new StringSetMatch(designations);
	}

	public boolean match(BugInstance bugInstance) {
		return designations.match(bugInstance.getUserDesignationKey());
	}

	public void writeXML(XMLOutput xmlOutput, boolean disabled) throws IOException {
		xmlOutput.startTag("Designation");
		if (disabled) xmlOutput.addAttribute("disabled","true");
		addAttribute(xmlOutput, "designation", designations);
		xmlOutput.stopTag(true);
	}

	public void addAttribute(XMLOutput xmlOutput, String name, StringSetMatch matches) throws IOException {
		String value = matches.toString();
		if (value.length() != 0)
			xmlOutput.addAttribute(name, value);
	}

}
