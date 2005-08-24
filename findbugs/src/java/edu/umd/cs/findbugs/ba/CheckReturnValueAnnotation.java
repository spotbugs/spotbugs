/*
 * FindBugs - Find Bugs in Java programs
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

package edu.umd.cs.findbugs.ba;

import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * @author pugh
 */
public class CheckReturnValueAnnotation extends AnnotationEnumeration {

	public final static CheckReturnValueAnnotation CHECK_RETURN_VALUE = new CheckReturnValueAnnotation(
			"CheckReturnValue", 1);

	public final static CheckReturnValueAnnotation OK_TO_IGNORE_RETURN_VALUE = new CheckReturnValueAnnotation(
			"OkToIgnoreReturnValue", 2);

	public final static CheckReturnValueAnnotation UNKNOWN_CHECKRETURNVALUE = new CheckReturnValueAnnotation(
			"UnknownCheckReturnValue", 0);

	private final static CheckReturnValueAnnotation[] myValues = { UNKNOWN_CHECKRETURNVALUE,
		CHECK_RETURN_VALUE, OK_TO_IGNORE_RETURN_VALUE };
	
	@CheckForNull public static CheckReturnValueAnnotation parse(String s) {
		for(CheckReturnValueAnnotation v : myValues) 
			if (s.endsWith(v.name)) return v;

		return null;
	}
	public static CheckReturnValueAnnotation[] values() {
		return myValues.clone();
	}

	private CheckReturnValueAnnotation(String s, int i) {
		super(s,i);
		
	}

	

}
