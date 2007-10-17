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
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;


public class LocalMatcher implements Matcher {
	private NameMatch name;

	public LocalMatcher(String name) {
		this.name = new NameMatch(name);
	}

	public LocalMatcher(String name, String type) {
		this.name = new NameMatch(name);
	}

	public String toString() {
		return "Local(name="+name+")";
	}
	public boolean match(BugInstance bugInstance) {
		LocalVariableAnnotation localAnnotation = bugInstance.getPrimaryLocalVariableAnnotation();
		if(localAnnotation == null) {
			return false;
		}
		if(!name.match(localAnnotation.getName())) {
			return false;
		}
		return true;
	}
	public void writeXML(XMLOutput xmlOutput, boolean disabled) throws IOException {
		XMLAttributeList attributes = new XMLAttributeList().addAttribute("name", name.getSpec());
		if (disabled) attributes.addAttribute("disabled", "true");
		xmlOutput.openCloseTag("Local", attributes);
	}
}
