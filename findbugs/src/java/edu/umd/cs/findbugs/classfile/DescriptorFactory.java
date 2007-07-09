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

/**
 * Factory for creating ClassDescriptors, MethodDescriptors, and FieldDescriptors.
 * 
 * @author David Hovemeyer
 */
public class DescriptorFactory {
	private static InheritableThreadLocal<DescriptorFactory> instance = 
		new InheritableThreadLocal<DescriptorFactory>();
	
	private Map<String, ClassDescriptor> classDescriptorMap;
	
	private DescriptorFactory() {
		this.classDescriptorMap = new HashMap<String, ClassDescriptor>();
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
	public ClassDescriptor getClassDescriptor(String className) {
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
	public ClassDescriptor getClassDescriptorForDottedClassName(String dottedClassName) {
		return getClassDescriptor(dottedClassName.replace('.', '/'));
	}
}
