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

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.ObjectType;

import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.FieldDescriptor;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.util.ClassName;

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
	public static MethodDescriptor getMethodDescriptor(JavaClass jclass, Method method) {
		return DescriptorFactory.instance().getMethodDescriptor(
				jclass.getClassName().replace('.', '/'), method.getName(), method.getSignature(), method.isStatic());
	}

	/**
	 * Get a MethodDescriptor describing the method called by
	 * given InvokeInstruction.
	 * 
	 * @param inv the InvokeInstruction
	 * @param cpg ConstantPoolGen of class containing instruction
	 * @return MethodDescriptor describing the called method
	 */
	public static MethodDescriptor getCalledMethodDescriptor(InvokeInstruction inv, ConstantPoolGen cpg) {
		String calledClassName = inv.getClassName(cpg).replace('.', '/');
		String calledMethodName = inv.getMethodName(cpg);
		String calledMethodSig = inv.getSignature(cpg);
		boolean isStatic = inv.getOpcode() == Constants.INVOKESTATIC;
		
		return DescriptorFactory.instance().getMethodDescriptor(calledClassName, calledMethodName, calledMethodSig, isStatic);
	}

	/**
	 * Get FieldDescriptor describing the field accessed by given FieldInstruction.
	 * 
     * @param fins a FieldInstruction
     * @param cpg  ConstantPoolGen for the method containing the FieldInstruction 
     * @return FieldDescriptor describing the field accessed by given FieldInstruction
     */
    public static FieldDescriptor getAccessedFieldDescriptor(FieldInstruction fins, ConstantPoolGen cpg) {
    	String className = fins.getClassName(cpg);
    	String fieldName = fins.getName(cpg);
    	String fieldSig = fins.getSignature(cpg);
    	boolean isStatic = (fins.getOpcode() == Constants.GETSTATIC || fins.getOpcode() == Constants.PUTSTATIC);
    	return DescriptorFactory.instance().getFieldDescriptor(className, fieldName, fieldSig, isStatic);
    }

	/**
	 * Construct a ClassDescriptor from a JavaClass.
	 * 
	 * @param jclass a JavaClass
	 * @return a ClassDescriptor identifying that JavaClass
	 */
	public static ClassDescriptor getClassDescriptor(JavaClass jclass) {
		return DescriptorFactory.instance().getClassDescriptor(
				ClassName.toSlashedClassName(jclass.getClassName()));
	}

	private static final int JDK15_MAJOR = 48;
	private static final int JDK15_MINOR = 0;

	/**
     * Checks if classfile was compiled for pre 1.5 target
     */
    public static boolean preTiger(JavaClass jclass) {
    	return jclass.getMajor() < JDK15_MAJOR ||
    			(jclass.getMajor() == JDK15_MAJOR && jclass.getMinor() < JDK15_MINOR);
    
    }

	/**
	 * Get a ClassDescriptor for the class described by given ObjectType object.
	 * 
     * @param type an ObjectType
     * @return a ClassDescriptor for the class described by the ObjectType
     */
    public static ClassDescriptor getClassDescriptor(ObjectType type) {
    	return DescriptorFactory.instance().getClassDescriptorForDottedClassName(type.getClassName());
    }
    
    /**
     * Throw a ClassNotFoundException to indicate that class named
     * by given ClassDescriptor cannot be found.
     * The exception message is formatted in a way that can
     * be decoded by ClassNotFoundExceptionParser.
     * 
     * @param classDescriptor ClassDescriptor naming a class that cannot be found
     * @throws ClassNotFoundException
     * @see edu.umd.cs.findbugs.ba.ClassNotFoundExceptionParser
     */
    public static void throwClassNotFoundException(ClassDescriptor classDescriptor) throws ClassNotFoundException {
    	throw new ClassNotFoundException("Class " + classDescriptor.toDottedClassName() + " cannot be resolved");
    }
}
