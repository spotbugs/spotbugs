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

package edu.umd.cs.findbugs.ba;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.analysis.AnnotatedObject;
import edu.umd.cs.findbugs.classfile.analysis.AnnotationValue;

/**
 * Interface for object representing information about a class.
 *
 * @author David Hovemeyer
 */
public interface XClass extends Comparable<ClassDescriptor>, AccessibleEntity, AnnotatedObject {

    /**
     * Get ClassDescriptor of this class's immediate superclass.
     *
     * @return ClassDescriptor of this class's immediate superclass, or null if
     *         this class has no immediate superclass
     */
    public @CheckForNull
    ClassDescriptor getSuperclassDescriptor();

    /**
     * Get ClassDescriptors of interfaces directly implemented by this class.
     *
     * @return ClassDescriptors of interfaces directly implemented by this class
     */
    public ClassDescriptor[] getInterfaceDescriptorList();

    /**
     * Get the ClassDescriptor of the immediate enclosing class, or null if this
     * XClass is not a nested or inner class.
     *
     * @return the ClassDescriptor of the immediate enclosing class, or null if
     *         this XClass is not a nested or inner class
     */
    public ClassDescriptor getImmediateEnclosingClass();

    /**
     * @return true if the class is an interface, false otherwise
     */
    public boolean isInterface();

    /**
     * @return true if the class is an abstract
     */
    public boolean isAbstract();

    /**
     *
     * @return the Source attribute
     */
    public @CheckForNull
    String getSource();

    @Override
    public Collection<ClassDescriptor> getAnnotationDescriptors();

    @Override
    public AnnotationValue getAnnotation(ClassDescriptor desc);

    /**
     * Find an XMethod matching given parameters.
     *
     * @param methodName
     *            name of the method
     * @param methodSig
     *            signature of the method
     * @param isStatic
     *            true if the method is static, false if not
     * @return matching XMethod, or null if there is no matching XMethod
     */
    public XMethod findMethod(String methodName, String methodSig, boolean isStatic);

    /**
     * Find XMethod matching given MethodDescriptor.
     *
     * @param descriptor
     *            a MethodDescriptor
     * @return matching XMethod, or null if there is no matching method
     */
    public XMethod findMethod(MethodDescriptor descriptor);

    /**
     * Find XMethod matching the name and signature of the supplied method
     * MethodDescriptor. The class descriptor of the argument is ignored.
     *
     * @param descriptor
     *            a MethodDescriptor
     * @return matching XMethod, or null if there is no matching method
     */
    public XMethod findMatchingMethod(MethodDescriptor descriptor);

    /**
     * Find an XField matching given parameters.
     *
     * @param name
     *            name of the field
     * @param signature
     *            signature of the field
     * @param isStatic
     *            true if field is static, false if not
     * @return XField, or null if there is no matching XField
     */
    public XField findField(String name, String signature, boolean isStatic);

    public List<? extends XField> getXFields();

    public List<? extends XMethod> getXMethods();

    public String getSourceSignature();

    public boolean usesConcurrency();

    public boolean hasStubs();

    /**
     * @return Returns the called class descriptors.
     */
    public Set<ClassDescriptor> getCalledClassDescriptors();
}
