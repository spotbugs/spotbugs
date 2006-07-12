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

import java.util.StringTokenizer;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.MethodAnnotation;

public class MethodMatcher implements Matcher {
	private NameMatch name;
	private String signature;

	public MethodMatcher(String name) {
		this.name = new NameMatch(name);
	}

	public MethodMatcher(String name, String params, String returns) {
		this.name = new NameMatch(name);
		this.signature = createSignature(params, returns);
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

	private static String createSignature(String params, String returns) {
		StringBuffer buf = new StringBuffer();

		buf.append('(');
		StringTokenizer tok = new StringTokenizer(params, " \t\n\r\f,");
		while (tok.hasMoreTokens()) {
			String param = tok.nextToken();
			buf.append(toSignature(param));
		}
		buf.append(')');
		buf.append(toSignature(returns));

		return buf.toString();
	}

	private static String toSignature(String type) {
		if (type.equals("boolean"))
			return "Z";
		else if (type.equals("byte"))
			return "B";
		else if (type.equals("char"))
			return "C";
		else if (type.equals("short"))
			return "S";
		else if (type.equals("int"))
			return "I";
		else if (type.equals("long"))
			return "J";
		else if (type.equals("float"))
			return "F";
		else if (type.equals("double"))
			return "D";
		else if (type.equals("void"))
			return "V";
		else
			return "L" + type.replace('.', '/') + ";";
	}
}

// vim:ts=4
