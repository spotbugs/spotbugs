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

import java.util.regex.Pattern;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.FieldAnnotation;

/**
 * @author rafal@caltha.pl
 */
public class FieldMatcher implements Matcher {
	private NameMatch name;
	private String signature;

	public FieldMatcher(String name) {
		this.name = new NameMatch(name);
	}

	public FieldMatcher(String name, String type) {
		this.name = new NameMatch(name);
		this.signature = SignatureUtil.createFieldSignature(type);
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
}
