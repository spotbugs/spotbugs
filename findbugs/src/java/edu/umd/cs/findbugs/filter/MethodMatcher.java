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
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;

public class MethodMatcher extends MemberMatcher implements Matcher {
	public MethodMatcher(String name) {
		super(name);
	}

	public MethodMatcher(String name, String params, String returns) {
		super(name,  SignatureUtil.createMethodSignature(params, returns));
	}

	public boolean match(BugInstance bugInstance) {
		MethodAnnotation methodAnnotation = bugInstance.getPrimaryMethod();
		if (methodAnnotation == null)
			return false;
		if (!name.match(methodAnnotation.getMethodName()))
			return false;
		if (signature != null && !signature.equals(methodAnnotation.getMethodSignature()))
			return false;
		return true;
	}
	public void writeXML(XMLOutput xmlOutput, boolean disabled) throws IOException {
		XMLAttributeList attributes = new XMLAttributeList().addAttribute("name", name.getSpec()).addAttribute("signature",signature);
		if (disabled) attributes.addAttribute("disabled", "true");
		xmlOutput.openCloseTag("Method", attributes);
	}
}

// vim:ts=4
