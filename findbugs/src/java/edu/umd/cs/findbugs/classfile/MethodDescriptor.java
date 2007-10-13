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

import edu.umd.cs.findbugs.ba.SignatureConverter;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;

/**
 * Descriptor uniquely identifying a method in a class.
 * 
 * @author David Hovemeyer
 */
public class MethodDescriptor
		extends FieldOrMethodDescriptor {
	/**
	 * Constructor.
	 * 
	 * @param className       name of the class containing the method, in VM format (e.g., "java/lang/String")
	 * @param methodName      name of the method
	 * @param methodSignature signature of the method
	 * @param isStatic        true if method is static, false otherwise
	 */
	public MethodDescriptor(@SlashedClassName String className, String methodName, String methodSignature, boolean isStatic) {
		super(className, methodName, methodSignature, isStatic);
		assert methodSignature.charAt(0) == '(';
		assert methodSignature.indexOf(')') > 0;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(MethodDescriptor o) {
		return super.compareTo(o);
	}

}
