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
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.xml.XMLAttributeList;
import edu.umd.cs.findbugs.xml.XMLOutput;

/**
 * @author rafal@caltha.pl
 */
public class FieldMatcher implements Matcher {
	final private NameMatch name;
	@CheckForNull final private String signature;

	public FieldMatcher(String name) {
		this.name = new NameMatch(name);
		this.signature = null;
	}

	public FieldMatcher(String name, String type) {
		if (name == null) {
			if (type == null)
				throw new FilterException("Field element must have either name or type attribute");
			else
				name = "~.*"; // any name
		
		}
		this.name = new NameMatch(name);
		if (type != null)
			this.signature = SignatureUtil.createFieldSignature(type);
		else this.signature = null;
	}

	public boolean match(BugInstance bugInstance) {
		FieldAnnotation fieldAnnotation = bugInstance.getPrimaryField();
		if(fieldAnnotation == null) {
			return false;
		}
		if(!name.match(fieldAnnotation.getFieldName())) {
			return false;
		}
		if (signature != null && !signature.equals(fieldAnnotation.getFieldSignature()))
			return false;
		return true;
	}
	public void writeXML(XMLOutput xmlOutput, boolean disabled) throws IOException {
		XMLAttributeList attributes = new XMLAttributeList().addAttribute("name", name.getSpec()).addAttribute("signature",signature);
		if (disabled) attributes.addAttribute("disabled", "true");
		xmlOutput.openCloseTag("Field", attributes);
	}
}
