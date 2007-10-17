/*
 * FindBugs - Find bugs in Java programs
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
package edu.umd.cs.findbugs.filter;

import java.io.IOException;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;

/**
 * Matcher to select BugInstances with a particular first version.
 */
public class FirstVersionMatcher extends VersionMatcher implements Matcher {
	public FirstVersionMatcher(String versionAsString, String relOpAsString) {
		this(Long.parseLong(versionAsString), RelationalOp.byName(relOpAsString));
	}
	public FirstVersionMatcher(String versionAsString, RelationalOp relOp) {
		this(Long.parseLong(versionAsString), relOp);
	}
	
	public FirstVersionMatcher(long version, RelationalOp relOp) {
		super(version,relOp);
}
	public String toString() {
		return "FirstVersion(version" + relOp + version +")";
	}
	public boolean match(BugInstance bugInstance) {
		return  relOp.check(bugInstance.getFirstVersion(), version);
	}
	public void writeXML(XMLOutput xmlOutput, boolean disabled) throws IOException {
		XMLAttributeList attributes = new XMLAttributeList().addAttribute("value", Long.toString(version)).addAttribute("relOp",relOp.getName());
		if (disabled) attributes.addAttribute("disabled", "true");
		xmlOutput.openCloseTag("FirstVersion", attributes);
	}
}
