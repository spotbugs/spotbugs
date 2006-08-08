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
 * Descriptor uniquely identifying a field in a class.
 * 
 * @author David Hovemeyer
 */
public class FieldDescriptor
		extends FieldOrMethodDescriptor
		implements Comparable<FieldDescriptor> {

	/**
	 * Constructor.
	 * 
	 * @param className      the name of the class the field belongs to
	 * @param fieldName      the name of the field
	 * @param fieldSignature the field signature (type)
	 * @param isStatic       true if field is static, false if not
	 */
	public FieldDescriptor(String className, String fieldName, String fieldSignature, boolean isStatic) {
		super(className, fieldName, fieldSignature, isStatic);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(FieldDescriptor o) {
		return super.compareTo(o);
	}

}
