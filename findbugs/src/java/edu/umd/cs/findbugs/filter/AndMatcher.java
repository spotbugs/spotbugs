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
import java.util.Iterator;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.xml.XMLOutput;

public class AndMatcher extends CompoundMatcher {

	boolean anyMatches = false;
	public boolean anyMatches() {
		return anyMatches;
	}
	public boolean match(BugInstance bugInstance) {
		Iterator<Matcher> i = childIterator();
		while (i.hasNext()) {
			Matcher child = i.next();
			if (!child.match(bugInstance))
				return false;
		}
		anyMatches = true;
		return true;

	}
	public void writeXML(XMLOutput xmlOutput, boolean disabled)  throws IOException {
		if (numberChildren() == 1) {
			// System.out.println("One child: " + this);
			childIterator().next().writeXML(xmlOutput, disabled);
			return;
		}
		xmlOutput.startTag("And");
		if (disabled) xmlOutput.addAttribute("disabled", "true");
		xmlOutput.stopTag(false);;	
		super.writeChildrenXML(xmlOutput);
		xmlOutput.closeTag("And");
	}
	public String toString() {
		return "And(" + super.toString() +")";
	}

}

// vim:ts=4
