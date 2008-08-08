/*
 * Bytecode Analysis Framework
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

import java.util.Collection;

import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.analysis.AnnotatedObject;
import edu.umd.cs.findbugs.classfile.analysis.AnnotationValue;

/**
 * An XMethod represents symbolic information about a particular method.
 * 
 * <p>
 * If the resolved() method returns true, then any information queried
 * from this object can be assumed to be accurate.
 * If the resolved() method returns false, then FindBugs can't
 * find the method and any information other than name/signature/etc.
 * cannot be trusted.
 * </p>
 * 
 * @author David Hovemeyer
 * @author Bill Pugh
 */
public interface XMethod extends ClassMember, AnnotatedObject {
	public boolean isNative();
	public boolean isAbstract();
	public boolean isSynchronized();
	public int getNumParams();

	/**
     * @return the MethodDescriptor identifying this object
     */
    public MethodDescriptor getMethodDescriptor();
    
    /**
     * 
     * @return the exceptions this method is declared to throw
     */
    public String[] getThrownExceptions();
    
    /**
     * @return does this method unconditionally throw an exception? 
     */
    public boolean isUnconditionalThrower();
    /**
     * @return does this method unconditionally throw an UnsupportedOperationException? 
     */
    public boolean isUnsupported();

	/**
     * @return true if method's return type is a reference type, false otherwise
     */
    public boolean isReturnTypeReferenceType();

	/**
	 * Get ClassDescriptors (annotation classes) of annotations applied
	 * directly to this method's parameters. 
	 * 
	 * @param param parameter number (0 for first parameter)
	 * @return ClassDescriptors of annotations applied directly to this method's parameters
	 */
    public Collection<ClassDescriptor> getParameterAnnotationDescriptors(int param);

    /**
	 * Get the AnnotationValue of annotation applied directly to given parameter.
	 * 
	 * @param param parameter number (0 for first parameter)
	 * @param desc  ClassDescriptor of the annotation class
	 * @return AnnotationValue annotating the parameter,
	 *         or null if parameter is not annotated with this kind of annotation
	 */
    public AnnotationValue getParameterAnnotation(int param, ClassDescriptor desc);

    /**
	 * Get collection of all AnnotationValues applied directly
	 * to given parameter.
	 * 
	 * @param param parameter number (0 for first parameter)
	 * @return Collection of all AnnotationValues applied directly
	 *         to given parameter
	 */
    public Collection<AnnotationValue> getParameterAnnotations(int param);

    /**
	 * Get ClassDescriptors (annotation classes) of annotations applied
	 * directly to this method. 
	 * 
	 * @return ClassDescriptors of annotations applied directly to this method
	 */
    public Collection<ClassDescriptor> getAnnotationDescriptors();

    /**
	 * Get the AnnotationValue of annotation applied directly to the method.
	 * 
	 * @param desc  ClassDescriptor of the annotation class
	 * @return AnnotationValue annotating the method,
	 *         or null if method is not annotated with this kind of annotation
	 */
    public AnnotationValue getAnnotation(ClassDescriptor desc);

    /**
	 * Get collection of all AnnotationValues applied directly
	 * to the method.
	 * 
	 * @return Collection of all AnnotationValues applied directly
	 *         to the method
	 */
    public Collection<AnnotationValue> getAnnotations();
}
