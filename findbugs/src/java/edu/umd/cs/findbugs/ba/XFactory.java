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
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InvokeInstruction;

import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

/**
 * Factory methods for creating XMethod objects.
 * 
 * @author David Hovemeyer
 */
public abstract class XFactory {
	/**
	 * Create an XMethod object from a BCEL Method.
	 * 
	 * @param javaClass the class to which the Method belongs
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
	 * Create an XField object from a BCEL Field.
	 * 
	 * @param javaClass the JavaClass containing the field
	 * @param field     the Field within the JavaClass
	 * @return the created XField
	 */
	public static XField createXField(JavaClass javaClass, Field field) {
		String className = javaClass.getClassName();
		String fieldName = field.getName();
		String fieldSig = field.getSignature();
		int accessFlags = field.getAccessFlags();
		if (field.isStatic())
			return new StaticField(className, fieldName, fieldSig, accessFlags);
		else
			return new InstanceField(className, fieldName, fieldSig, accessFlags);
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
	
	/**
	 * Create an XMethod object from the method currently being visited by
	 * the given PreorderVisitor.
	 * 
	 * @param visitor the PreorderVisitor
	 * @return the XMethod representing the method currently being visited
	 */
	public static XMethod createXMethod(PreorderVisitor visitor) {
		JavaClass javaClass = visitor.getThisClass();
		Method method = visitor.getMethod();
		return createXMethod(javaClass, method);
	}
	
	/**
	 * Create an XField object from the field currently being visited by
	 * the given PreorderVisitor.
	 * 
	 * @param visitor the PreorderVisitor
	 * @return the XField representing the method currently being visited
	 */
	public static XField createXField(PreorderVisitor visitor) {
		JavaClass javaClass = visitor.getThisClass();
		Field field = visitor.getField();
		return createXField(javaClass, field);
	}
	
	/**
	 * Create an XMethod.
	 * Note that the method access flags are set to a plausible, but not
	 * necessarily correct value.
	 * 
	 * @param className  class containing the method
	 * @param methodName method name
	 * @param methodSig  method signature
	 * @param isStatic   true if method is static, false if not
	 * @return the created XMethod
	 */
	public static XMethod createXMethod(String className, String methodName, String methodSig, boolean isStatic) {
		if (isStatic)
			return new StaticMethod(className, methodName, methodSig, Constants.ACC_STATIC);
		else
			return new InstanceMethod(className, methodName, methodSig, 0);
	}
}
