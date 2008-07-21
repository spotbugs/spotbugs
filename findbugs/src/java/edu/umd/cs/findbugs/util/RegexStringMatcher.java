/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2008, University of Maryland
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307, USA
 */

package edu.umd.cs.findbugs.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * StringMatcher that matches based on a regular expression.
 * 
 * @author David Hovemeyer
 */
public class RegexStringMatcher implements StringMatcher {
	
	private Pattern pattern;
	
	/**
	 * Constructor.
	 * 
	 * @param patStr a String defining the regular expression pattern to match
	 */
	public RegexStringMatcher(String patStr) {
		pattern = Pattern.compile(patStr);
	}

	public boolean matches(String s) {
		Matcher m = pattern.matcher(s);
		return m.matches();
	}

	@Override
	public String toString() {
		return pattern.toString();
	}
	
}
