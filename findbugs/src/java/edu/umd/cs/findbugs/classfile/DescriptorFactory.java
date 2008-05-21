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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ObjectType;

import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.classfile.analysis.MethodInfo;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.util.MapCache;

/**
 * Factory for creating ClassDescriptors, MethodDescriptors, and FieldDescriptors.
 *
 * @author David Hovemeyer
 */
public class DescriptorFactory {
	private static ThreadLocal<DescriptorFactory> instance =
		new ThreadLocal<DescriptorFactory>() {
		@Override
        protected
		DescriptorFactory initialValue() {
			return  new DescriptorFactory();
		}
	};

	private Map<String, ClassDescriptor> classDescriptorMap;
	private Map<MethodDescriptor, MethodDescriptor> methodDescriptorMap;
	private Map<FieldDescriptor, FieldDescriptor> fieldDescriptorMap;

	private DescriptorFactory() {
		this.classDescriptorMap = new HashMap<String, ClassDescriptor>();
		this.methodDescriptorMap = new HashMap<MethodDescriptor, MethodDescriptor>();
		this.fieldDescriptorMap = new HashMap<FieldDescriptor, FieldDescriptor>();
	}
	MapCache<String, String> stringCache = new MapCache<String, String>(10000);
	public static String canonicalizeString(String s) {
		if (s == null) return null;
		DescriptorFactory instance = instance();
		String result = instance.stringCache.get(s);
		if (result != null) return result;
		instance.stringCache.put(s,s);
		return s;
	}

	/**
	 * Get the singleton instance of the DescriptorFactory.
	 *
	 * @return the singleton instance of the DescriptorFactory
	 */
	public static DescriptorFactory instance() {
		return instance.get();
	}

	public static void clearInstance() {
		instance.remove();
	}

	public Collection<ClassDescriptor> getAllClassDescriptors() {
		return classDescriptorMap.values();
	}

	public void purge(Collection<ClassDescriptor> unusable) {
		for(ClassDescriptor c : unusable)
		 classDescriptorMap.remove(c.getClassName());
	}
	/**
	 * Get a ClassDescriptor for a class name in VM (slashed) format.
	 *
	 * @param className a class name in VM (slashed) format
	 * @return ClassDescriptor for that class
	 */
	public ClassDescriptor getClassDescriptor(@SlashedClassName String className) {
		assert className.indexOf('.') == -1;
		className = canonicalizeString(className);
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
	 * @param dottedClassName a class name in dotted format
	 * @return ClassDescriptor for that class
	 */
	public ClassDescriptor getClassDescriptorForDottedClassName(@DottedClassName String dottedClassName) {
		return getClassDescriptor(dottedClassName.replace('.', '/'));
	}

	public MethodDescriptor getMethodDescriptor(JavaClass jClass, Method method) {
		return getMethodDescriptor(ClassName.toSlashedClassName(jClass.getClassName()), method.getName(), method.getSignature(), method.isStatic());
	}

	/**
	 * Get a MethodDescriptor.
	 *
	 * @param className       name of the class containing the method, in VM format (e.g., "java/lang/String")
	 * @param name      name of the method
	 * @param signature signature of the method
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

	public void profile() {
		int total = 0;
		int keys = 0;
		int values = 0;
		int bad = 0;
		for(Map.Entry<MethodDescriptor, MethodDescriptor> e : methodDescriptorMap.entrySet()) {
			total++;
			if (e.getKey() instanceof MethodInfo) keys++;
			else if (total - keys < 10)
				System.out.println(e.getKey());
			if (e.getValue() instanceof MethodInfo) values++;
		}
		System.out.printf("Descriptor factory: %d/%d/%d\n", keys, values, total);

	}
	public void canonicalize(MethodDescriptor m) {
		MethodDescriptor existing = methodDescriptorMap.get(m);
		if (m != existing) {
			methodDescriptorMap.remove(m);
			methodDescriptorMap.put(m, m);
		}

	}
	public void canonicalize(FieldDescriptor m) {
		FieldDescriptor existing = fieldDescriptorMap.get(m);
		if (m != existing) {
			fieldDescriptorMap.remove(m);
			fieldDescriptorMap.put(m, m);
		}

	}
	public MethodDescriptor getMethodDescriptor(MethodAnnotation ma) {
		return getMethodDescriptor(ClassName.toSlashedClassName(ma.getClassName()), ma.getMethodName(), ma.getMethodSignature(), ma.isStatic());
	}
	/**
	 * Get a FieldDescriptor.
	 *
	 * @param className      the name of the class the field belongs to, in VM format (e.g., "java/lang/String")
	 * @param name      the name of the field
	 * @param signature the field signature (type)
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
	public FieldDescriptor getFieldDescriptor(FieldAnnotation ma) {
		return getFieldDescriptor(ClassName.toSlashedClassName(ma.getClassName()), ma.getFieldName(), ma.getFieldSignature(), ma.isStatic());
	}
	/**
     * Get a ClassDescriptor for the class described by given ObjectType object.
     * 
     * @param type an ObjectType
     * @return a ClassDescriptor for the class described by the ObjectType
     */
    public static ClassDescriptor getClassDescriptor(ObjectType type) {
    	return instance().getClassDescriptorForDottedClassName(type.getClassName());
    }

	public static ClassDescriptor createClassDescriptor(JavaClass c) {
        return DescriptorFactory.createClassDescriptorFromDottedClassName(c.getClassName());
    }

	/**
     * Create a class descriptor from a resource name.
     *
     * @param resourceName the resource name
     * @return the class descriptor
     */
    public static ClassDescriptor createClassDescriptorFromResourceName(String resourceName) {
    	if (!isClassResource(resourceName)) {
    		throw new IllegalArgumentException("Resource " + resourceName + " is not a class");
    	}
    	return createClassDescriptor(resourceName.substring(0, resourceName.length() - 6));
    }

	/**
     * Create a class descriptor from a field signature
     *
     */
    public static @CheckForNull ClassDescriptor createClassDescriptorFromFieldSignature(String signature) {
    	int start = signature.indexOf('L');
    	if (start < 0) {
    		return null;
    	}
    	int end = signature.indexOf(';', start);
    	if (end < 0) {
    		return null;
    	}
    	return createClassDescriptor(signature.substring(start+1, end));
    }

	/**
     * Determine whether or not the given resource name refers to a class.
     *
     * @param resourceName the resource name
     * @return true if the resource is a class, false otherwise
     */
    public static boolean isClassResource(String resourceName) {
    	// This could be more sophisticated.
    	return resourceName.endsWith(".class");
    }

	public static ClassDescriptor createClassDescriptorFromSignature(String signature) {
    	int first = 0;
    	while (signature.charAt(first) == '[') first++;
    	signature = signature.substring(first);
    	if (signature.endsWith(";"))
    		signature = signature.substring(1, signature.length()-1);
    	return createClassDescriptor(signature);
    }

	public static ClassDescriptor createClassDescriptor(Class aClass) {
		String className = ClassName.fromSignature(aClass.getName().replace('.','/'));
    	return instance().getClassDescriptor(className);
    }

	public static ClassDescriptor createClassDescriptor(@SlashedClassName String className) {
		className = ClassName.fromSignature(className);
    	return instance().getClassDescriptor(className);
    }

	public static ClassDescriptor[] createClassDescriptor(String[] classNames) {
    	ClassDescriptor[] result = new ClassDescriptor[classNames.length];
    	for(int i = 0; i < classNames.length; i++)
    		result[i] = createClassDescriptor(classNames[i]);
        return result;
    }

	public static ClassDescriptor createClassDescriptorFromDottedClassName(String dottedClassName) {
        return createClassDescriptor(dottedClassName.replace('.','/'));
    }
}
