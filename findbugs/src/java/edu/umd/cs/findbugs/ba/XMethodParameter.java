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

/**
 * @author pugh
 */
public class XMethodParameter implements Comparable<XMethodParameter>{
	/**
	 * Create a new Method parameter reference
	 * @param m the method of which this is a parameter to
	 * @param p the parameter index (0 for first parameter)
	 */
	public XMethodParameter(XMethod m, int p) {
		method = m;
		parameter = p;
	}
	private final XMethod method;
	private final int parameter;
	public XMethod getMethod() {
		return method;
	}
	public int getParameterNumber() {
		return parameter;
	}
	@Override
		 public boolean equals(Object o) {
		if (!(o instanceof XMethodParameter)) return false;
		XMethodParameter mp2 = (XMethodParameter) o;
		return parameter == mp2.parameter && method.equals(mp2.method);
	}
	@Override
		 public int hashCode() {
		return method.hashCode() + parameter;
	}
	public int compareTo(XMethodParameter mp2) {
		int result = method.compareTo(mp2.method);
		if (result != 0) return result;
		return parameter - mp2.parameter;
	}
	@Override
		 public String toString() {
		return "parameter " + parameter + " of " + method;
	}
}
