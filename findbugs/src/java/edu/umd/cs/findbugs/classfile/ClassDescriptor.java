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

package edu.umd.cs.findbugs.classfile;

/**
 * Descriptor identifying a class.
 * 
 * @author David Hovemeyer
 */
public class ClassDescriptor implements Comparable<ClassDescriptor> {
	private String className;
	
	/**
	 * Constructor.
	 * 
	 * @param className class name in VM format, e.g. "java/lang/String"
	 */
	public ClassDescriptor(String className) {
		if (className.indexOf('.') >= 0) {
			throw new IllegalArgumentException("Class name " + className + " not in VM format");
		}
		this.className = className;
	}
	
	/**
	 * @return Returns the class name in VM format, e.g. "java/lang/String"
	 */
	public String getClassName() {
		return className;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(ClassDescriptor o) {
		return className.compareTo(o.className);
	}

	/**
	 * Get the resource name of this class as it would appear in the classpath.
	 * E.g., "java/lang/String.class"
	 * 
	 * @return the resource name
	 */
	public String toResourceName() {
		return className + ".class";
	}
}
