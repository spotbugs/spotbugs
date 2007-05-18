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
 * Matcher to select BugInstances with a particular last version.
 */
public class LastVersionMatcher implements Matcher {
	
	public final static  LastVersionMatcher DEAD_BUG_MATCHER = new LastVersionMatcher(-1, RelationalOp.EQ);
	final private int version;
	final private RelationalOp relOp;

	@Override
    public int hashCode() {
		return version+relOp.hashCode();
	}
	@Override
    public boolean equals(Object o) {
		if (!(o instanceof LastVersionMatcher)) return false;
		LastVersionMatcher m = (LastVersionMatcher)o;
		return version == m.version && relOp.equals(m.relOp);
	}
	public LastVersionMatcher(String versionAsString, String relOpAsString) {
		this(Integer.parseInt(versionAsString), RelationalOp.byName(relOpAsString));
	}
	public LastVersionMatcher(String versionAsString, RelationalOp relOp) {
		this(Integer.parseInt(versionAsString), relOp);
	}
	public LastVersionMatcher(int version, RelationalOp relOp) {
		this.version = version;
		this.relOp = relOp;
}
	public boolean match(BugInstance bugInstance) {
		return relOp.check(bugInstance.getLastVersion(), version);
	}
	public void writeXML(XMLOutput xmlOutput, boolean disabled) throws IOException {
		XMLAttributeList attributes = new XMLAttributeList().addAttribute("value", Integer.toString(version)).addAttribute("relOp",relOp.getName());
		if (disabled) attributes.addAttribute("disabled", "true");
		xmlOutput.openCloseTag("LastVersion", attributes);
	}
	
	@Override
    public String toString() {
		if (version == -1 && relOp == RelationalOp.NEQ) return "active bug";
		else if (version == -1 && relOp == RelationalOp.EQ) return "dead bug";
		return "lastVersion " + relOp + " " + version;
	}
}
