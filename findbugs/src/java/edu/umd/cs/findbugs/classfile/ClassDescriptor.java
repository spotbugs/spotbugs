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

import java.io.Serializable;

import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import edu.umd.cs.findbugs.util.ClassName;

/**
 * Descriptor identifying a class.
 * 
 * @author David Hovemeyer
 */
public class ClassDescriptor implements Comparable<ClassDescriptor>, Serializable {
	private static final long serialVersionUID = 1L;
	private final String className;

	/**
	 * Constructor.
	 * 
	 * @param className class name in VM format, e.g. "java/lang/String"
	 */
	 protected ClassDescriptor(@SlashedClassName String className) {
		if (className.indexOf('.') >= 0) {
			throw new IllegalArgumentException("Class name " + className + " not in VM format");
		}
		if (!ClassName.isValidClassName(className)) {
			throw new IllegalArgumentException("Invalid class name " + className);
		}
		this.className = className;
	}

	/**
	 * @return Returns the class name in VM format, e.g. "java/lang/String"
	 */
	public final @SlashedClassName String getClassName() {
		return className;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(ClassDescriptor o) {
		return className.compareTo(o.className);
	}

	/**
	 * Get the resource name of this class as it would appear in the classpath.
	 * E.g., "java/lang/String.class"
	 * 
	 * @return the resource name
	 */
	public String toResourceName() {
		return className + ".class";
	}

	/**
	 * Get the name of the class in dotted format.
	 * 
	 * @return the name of the class in dotted format
	 */
	public @DottedClassName String toDottedClassName() {
		return ClassName.toDottedClassName(className);
	}
	/**
	 * Get the name of the class in dotted format.
	 * 
	 * @return the name of the class in dotted format
	 */
	public  @DottedClassName  String getDottedClassName() {
		return ClassName.toDottedClassName(className);
	}

	/**
	 * Get the name of the package in dotted format.
	 * 
	 * @return the name of the package in dotted format
	 */
	public  @DottedClassName  String getPackageName() {
		return ClassName.extractPackageName(ClassName.toDottedClassName(className));
	}

	/**
	 * Get the simple name of the class 
	 * 
	 * @return the simple name of the class
	 */
	public   String getSimpleName() {
		return ClassName.extractSimpleName(ClassName.toDottedClassName(className));
	}
	/**
	 * Create a class descriptor from a resource name.
	 * 
	 * @param resourceName the resource name
	 * @return the class descriptor
	 */
	public static ClassDescriptor fromResourceName(String resourceName) {
		if (!isClassResource(resourceName)) {
			throw new IllegalArgumentException("Resource " + resourceName + " is not a class");
		}
		return createClassDescriptor(resourceName.substring(0, resourceName.length() - 6));
	}
	
	/**
	 * Create a class descriptor from a field signature
	 * 
	 */
	public static @CheckForNull ClassDescriptor fromFieldSignature(String signature) {
		int start = signature.indexOf('L');
		if (start < 0) {
			return null;
		}
		signature = signature.substring(start);
		int end = signature.indexOf(';');
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
	public static ClassDescriptor createClassDescriptor(@SlashedClassName String className) {
		return DescriptorFactory.instance().getClassDescriptor(className);
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
	public static ClassDescriptor createClassDescriptor(JavaClass c) {
	    return createClassDescriptorFromDottedClassName(c.getClassName());
    }
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return className;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public final boolean equals(Object obj) {
		if (obj == null || !(obj instanceof ClassDescriptor)) {
			return false;
		}
		
		// All instances of ClassDescriptor should be considered
		// equal if they represent the same class,
		// even if compared to an object of a different runtime class.
		return getClassName().equals(((ClassDescriptor)obj).getClassName());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return className.hashCode();
	}
}
