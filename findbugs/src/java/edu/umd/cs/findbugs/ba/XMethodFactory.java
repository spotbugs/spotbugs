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

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InvokeInstruction;

/**
 * Factory methods for creating XMethod objects.
 * 
 * @author David Hovemeyer
 */
public abstract class XMethodFactory {
	/**
	 * Create an XMethod object from a BCEL Method.
	 * 
	 * @param classContext the ClassContext of the class to which the Method belongs
	 * @param method       the Method
	 * @return an XMethod representing the Method
	 */
	public static XMethod createXMethod(JavaClass javaClass, Method method) {
		String className = javaClass.getClassName();
		String methodName = method.getName();
		String methodSig = method.getSignature();
		int accessFlags = method.getAccessFlags();
		if (method.isStatic())
			return new StaticMethod(className, methodName, methodSig, accessFlags);
		else
			return new InstanceMethod(className, methodName, methodSig, accessFlags);
	}
	
	/**
	 * Create an XMethod object from an InvokeInstruction.
	 * 
	 * @param invokeInstruction the InvokeInstruction
	 * @param cpg               ConstantPoolGen from the class containing the instruction
	 * @return XMethod representing the method called by the InvokeInstruction
	 */
	public static XMethod createXMethod(InvokeInstruction invokeInstruction, ConstantPoolGen cpg) {
		String className = invokeInstruction.getClassName(cpg);
		String methodName = invokeInstruction.getName(cpg);
		String methodSig = invokeInstruction.getSignature(cpg);
		
		// NOTE: access flags are ignored when comparing
		// XMethods.  So it's OK that we make up values here.
		
		return (invokeInstruction.getOpcode() == Constants.INVOKESTATIC)
			? new StaticMethod(className, methodName, methodSig, Constants.ACC_STATIC | Constants.ACC_PUBLIC)
			: new InstanceMethod(className, methodName, methodSig, Constants.ACC_PUBLIC);
	}
}
