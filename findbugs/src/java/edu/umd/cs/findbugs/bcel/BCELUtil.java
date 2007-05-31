/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2007 University of Maryland
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

package edu.umd.cs.findbugs.bcel;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * Utility methods for detectors and analyses using BCEL.
 * 
 * @author David Hovemeyer
 */
public abstract class BCELUtil {
	/**
	 * Construct a MethodDescriptor from JavaClass and method.
	 * 
	 * @param jclass a JavaClass
	 * @param method a Method belonging to the JavaClass
	 * @return a MethodDescriptor identifying the method
	 */
	public static final MethodDescriptor getMethodDescriptor(JavaClass jclass, Method method) {
		return new MethodDescriptor(
				jclass.getClassName().replace('.', '/'), method.getName(), method.getSignature(), method.isStatic());
	}

	/**
	 * Construct a ClassDescriptor from a JavaClass.
	 * 
     * @param jclass a JavaClass
     * @return a ClassDescriptor identifying that JavaClass
     */
    public static ClassDescriptor getClassDescriptor(JavaClass jclass) {
    	return new ClassDescriptor(jclass.getClassName().replace('.', '/'));
    }
}
