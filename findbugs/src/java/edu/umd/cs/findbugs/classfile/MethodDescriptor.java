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

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;

/**
 * Descriptor uniquely identifying a method in a class.
 * 
 * @author David Hovemeyer
 */
public class MethodDescriptor
		extends FieldOrMethodDescriptor {
	
	/**
	 * The bridge method signature or null if this method is not bridged
	 */
	private final String bridgeMethodSignature;

	/**
	 * Constructor.
	 * 
	 * @param className       name of the class containing the method, in VM format (e.g., "java/lang/String")
	 * @param methodName      name of the method
	 * @param methodSignature signature of the method
	 * @param isStatic        true if method is static, false otherwise
	 */
	public MethodDescriptor(@SlashedClassName String className, String methodName, String methodSignature, boolean isStatic) {
		this(className, methodName, methodSignature, null, isStatic);
	}
	
	/**
     * Constructor.
    
     * @param className             name of the class containing the method, in VM format (e.g., "java/lang/String")
     * @param methodName            name of the method
     * @param methodSignature       signature of the method
     * @param bridgeMethodSignature the bridge method signature or null
     * @param isStatic              true if method is static, false otherwise
     * @param isBridged             true if method is bridged, false otherwise
     * @deprecated Use {@link #MethodDescriptor(String,String,String,String,boolean)} instead
     */
    public MethodDescriptor(@SlashedClassName String className, String methodName, String methodSignature, @CheckForNull String bridgeMethodSignature, boolean isStatic, boolean isBridged) {
        this(className, methodName, methodSignature, bridgeMethodSignature, isStatic);
    }

	/**
	 * Constructor.

	 * @param className             name of the class containing the method, in VM format (e.g., "java/lang/String")
	 * @param methodName            name of the method
	 * @param methodSignature       signature of the method
	 * @param bridgeMethodSignature the bridge method signature or null
	 * @param isStatic              true if method is static, false otherwise
	 */
	public MethodDescriptor(@SlashedClassName String className, String methodName, String methodSignature,  @CheckForNull String bridgeMethodSignature, boolean isStatic) {
		super(className, methodName, methodSignature, isStatic);
		assert methodSignature.charAt(0) == '(';
		assert methodSignature.indexOf(')') > 0;
		this.bridgeMethodSignature = DescriptorFactory.canonicalizeString(bridgeMethodSignature);
	}


    public final boolean isBridged() {
    	return bridgeMethodSignature != null;
    }
    
    public final String getBridgeSignature() {
        return bridgeMethodSignature;
    }
}
