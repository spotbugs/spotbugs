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

package edu.umd.cs.findbugs.classfile;

import java.util.HashMap;
import java.util.Map;

import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;

/**
 * Factory for creating ClassDescriptors, MethodDescriptors, and FieldDescriptors.
 * 
 * @author David Hovemeyer
 */
public class DescriptorFactory {
	private static InheritableThreadLocal<DescriptorFactory> instance = 
		new InheritableThreadLocal<DescriptorFactory>();
	
	private Map<String, ClassDescriptor> classDescriptorMap;
	private Map<MethodDescriptor, MethodDescriptor> methodDescriptorMap;
	private Map<FieldDescriptor, FieldDescriptor> fieldDescriptorMap;
	
	private DescriptorFactory() {
		this.classDescriptorMap = new HashMap<String, ClassDescriptor>();
		this.methodDescriptorMap = new HashMap<MethodDescriptor, MethodDescriptor>();
		this.fieldDescriptorMap = new HashMap<FieldDescriptor, FieldDescriptor>();
	}

	/**
	 * Get the singleton instance of the DescriptorFactory. 
	 * 
	 * @return the singleton instance of the DescriptorFactory
	 */
	public static DescriptorFactory instance() {
		DescriptorFactory factory = instance.get();
		if (factory == null) {
			factory = new DescriptorFactory();
			instance.set(factory);
		}
		return factory;
	}
	
	/**
	 * Get a ClassDescriptor for a class name in VM (slashed) format.
	 * 
	 * @param className a class name in VM (slashed) format
	 * @return ClassDescriptor for that class
	 */
	public ClassDescriptor getClassDescriptor(@SlashedClassName String className) {
		assert className.indexOf('.') == -1;
		ClassDescriptor classDescriptor = classDescriptorMap.get(className);
		if (classDescriptor == null) {
			classDescriptor = new ClassDescriptor(className);
			classDescriptorMap.put(className, classDescriptor);
		}
		return classDescriptor;
	}
	
	/**
	 * Get a ClassDescriptor for a class name in dotted format.
	 * 
	 * @param className a class name in dotted format
	 * @return ClassDescriptor for that class
	 */
	public ClassDescriptor getClassDescriptorForDottedClassName(@DottedClassName String dottedClassName) {
		return getClassDescriptor(dottedClassName.replace('.', '/'));
	}

	/**
	 * Get a MethodDescriptor.
	 * 
	 * @param className       name of the class containing the method, in VM format (e.g., "java/lang/String")
	 * @param methodName      name of the method
	 * @param methodSignature signature of the method
	 * @param isStatic        true if method is static, false otherwise
	 * @return MethodDescriptor
	 */
	public MethodDescriptor getMethodDescriptor(@SlashedClassName String className, String name, String signature, boolean isStatic) {
		if (className == null) throw new NullPointerException("className must be nonnull");
		MethodDescriptor methodDescriptor = new MethodDescriptor(className, name, signature, isStatic);
		MethodDescriptor existing = methodDescriptorMap.get(methodDescriptor);
		if (existing == null) {
			methodDescriptorMap.put(methodDescriptor, methodDescriptor);
			existing = methodDescriptor;
		}
		return existing;
	}

	/**
	 * Get a FieldDescriptor.
	 * 
	 * @param className      the name of the class the field belongs to, in VM format (e.g., "java/lang/String")
	 * @param fieldName      the name of the field
	 * @param fieldSignature the field signature (type)
	 * @param isStatic       true if field is static, false if not
	 * @return FieldDescriptor
	 */
	public FieldDescriptor getFieldDescriptor(@SlashedClassName String className, String name, String signature, boolean isStatic) {
		FieldDescriptor fieldDescriptor = new FieldDescriptor(className, name, signature, isStatic);
		FieldDescriptor existing = fieldDescriptorMap.get(fieldDescriptor);
		if (existing == null) {
			fieldDescriptorMap.put(fieldDescriptor, fieldDescriptor);
			existing = fieldDescriptor;
		}
		return existing;
	}
}
