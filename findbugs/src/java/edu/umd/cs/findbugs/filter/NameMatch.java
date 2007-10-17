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

import edu.umd.cs.findbugs.annotations.CheckForNull;

/**
 * Matches a String value against a predefined specification.
 * 
 * Matching can be done in three modes depending on ctor matchSpec argument.
 * 
 * If matchSpec is null, match will succeed for any value (including empty String and null)
 * 
 * If matchSpec starts with ~ character it will be treated as java.util.regex.Pattern, with the ~
 * character ommited. The pattern will be matched against whole value (ie Matcher.match(), not Matcher.find())
 * 
 * If matchSpec is a non-null String with any other initial charcter, exact matching using String.equals(String)
 * will be performed.
 * 
 * @author rafal@caltha.pl
 */
public class NameMatch {

	private String spec;
	private @CheckForNull String exact;

	private @CheckForNull Pattern pattern;

	@Override
    public int hashCode() {
		return spec.hashCode();
	}
	public boolean isUniversal() {
		return spec.equals("~.*");
	}
	@Override
    public boolean equals(Object o) {
		if (!(o instanceof NameMatch)) return false;
		return spec.equals(((NameMatch)o).spec);
	}
	public String getValue() {
		if (exact != null) return exact;
		return pattern.toString();
	}
	public NameMatch(String matchSpec) {
		spec = matchSpec;
		if (matchSpec != null) {			
			if (matchSpec.startsWith("~")) {
				pattern = Pattern.compile(matchSpec.substring(1));
			} else {
				exact = matchSpec;
			}
		}
	}

	public boolean match(String value) {
		if (exact != null) 
			return exact.equals(value);
		if (pattern != null)
			return pattern.matcher(value).matches();
		return true;
	}

	@Override
	public String toString() {
		if (exact != null) 
			return "exact(" + exact + ")";	
		if (pattern != null) 
			return "regex(" + pattern.toString() + ")";
		return "any()";
	}
	public String getSpec() {
		return spec;
	}
}
