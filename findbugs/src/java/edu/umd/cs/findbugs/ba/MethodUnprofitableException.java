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

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * Used to signal a method not analyzed because it seemed unprofitable to do so
 * @author pugh
 */
public class MethodUnprofitableException extends CFGBuilderException {
	private static final long serialVersionUID = 1L;
	private final XMethod method;

	/**
	 * Constructor.
	 *
	 * @param method the method that is unprofitable to analyze
	 */
	public MethodUnprofitableException(JavaClassAndMethod method) {
		super("Appears unprofitable to analyze " + method);
		this.method = XFactory.createXMethod(method.getJavaClass(), method.getMethod());
	}

	/**
	 * Constructor.
	 *
	 * @param jClass the class containing the method that is unprofitable to analyze
	 * @param method the method that is unprofitable to analyze
	 */
	public MethodUnprofitableException(JavaClass jClass, Method method) {
		super("Appears unprofitable to analyze " + method);
		this.method = XFactory.createXMethod(jClass, method);
	}
	
	/**
	 * Constructor.
	 * 
	 * @param methodDescriptor the MethodDescriptor indicating the method it is unprofitable to analyze
	 */
	public MethodUnprofitableException(MethodDescriptor methodDescriptor) {
		super("Appears unprofitable to analyze " + methodDescriptor.toString());
		this.method = XFactory.createXMethod(
				methodDescriptor.getClassDescriptor().toDottedClassName(),
				methodDescriptor.getName(),
				methodDescriptor.getSignature(),
				methodDescriptor.isStatic());
	}

	/**
	 * @return the method that is unprofitable to analyze
	 */
	public XMethod getMethod() {
		return method;
	}

}
