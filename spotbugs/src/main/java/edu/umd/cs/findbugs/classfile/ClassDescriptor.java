/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006-2018, University of Maryland
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
import java.util.regex.Pattern;

import javax.annotation.CheckForNull;

import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.ba.XClass;
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

    private final @SlashedClassName String className;

    private static final Pattern ANONYMOUS_CLASS_NAME = Pattern.compile(".*\\$[0-9]*$");

    public static final ClassDescriptor[] EMPTY_ARRAY = new ClassDescriptor[0];

    /**
     * Constructor.
     *
     * @param className
     *            class name in VM format, e.g. "java/lang/String"
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

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(ClassDescriptor o) {
        return className.compareTo(o.className);
    }


    public boolean matches(Class<?> c) {
        return getDottedClassName().equals(c.getName());
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
     *
     * @deprecated use {@link #getDottedClassName()} instead.
     */
    @Deprecated
    public @DottedClassName String toDottedClassName() {
        return getDottedClassName();
    }

    /**
     * Get the name of the class in dotted format.
     *
     * @return the name of the class in dotted format
     */
    public @DottedClassName String getDottedClassName() {
        return ClassName.toDottedClassName(className);
    }

    public XClass getXClass() throws CheckedAnalysisException {
        return Global.getAnalysisCache().getClassAnalysis(XClass.class, this);
    }

    /**
     * Get the name of the package in dotted format.
     *
     * @return the name of the package in dotted format
     */
    public @DottedClassName String getPackageName() {
        return ClassName.extractPackageName(ClassName.toDottedClassName(className));
    }

    /**
     * Get the simple name of the class
     *
     * @return the simple name of the class
     */
    public String getSimpleName() {
        return ClassName.extractSimpleName(ClassName.toDottedClassName(className));
    }

    public String getSignature() {
        if (isArray()) {
            return className;
        }
        return "L" + className + ";";
    }

    public boolean isArray() {
        return className.charAt(0) == '[';
    }

    /**
     * Create a class descriptor from a resource name.
     *
     * @param resourceName
     *            the resource name
     * @return the class descriptor
     * @deprecated Use
     *             {@link DescriptorFactory#createClassDescriptorFromResourceName(String)}
     *             instead
     */
    @Deprecated
    public static ClassDescriptor fromResourceName(String resourceName) {
        return DescriptorFactory.createClassDescriptorFromResourceName(resourceName);
    }

    /**
     * Create a class descriptor from a field signature
     *
     * @deprecated Use
     *             {@link DescriptorFactory#createClassDescriptorFromFieldSignature(String)}
     *             instead
     *
     */
    @Deprecated
    public static @CheckForNull ClassDescriptor fromFieldSignature(String signature) {
        return DescriptorFactory.createClassDescriptorFromFieldSignature(signature);
    }

    /**
     * Determine whether or not the given resource name refers to a class.
     *
     * @param resourceName
     *            the resource name
     * @return true if the resource is a class, false otherwise
     * @deprecated Use {@link DescriptorFactory#isClassResource(String)} instead
     */
    @Deprecated
    public static boolean isClassResource(String resourceName) {
        return DescriptorFactory.isClassResource(resourceName);
    }

    /**
     * @deprecated Use
     *             {@link DescriptorFactory#createClassDescriptorFromSignature(String)}
     *             instead
     */
    @Deprecated
    public static ClassDescriptor createClassDescriptorFromSignature(String signature) {
        return DescriptorFactory.createClassDescriptorFromSignature(signature);
    }

    /**
     * @deprecated Use {@link DescriptorFactory#createClassDescriptor(String)}
     *             instead
     */
    @Deprecated
    public static ClassDescriptor createClassDescriptor(@SlashedClassName String className) {
        return DescriptorFactory.createClassDescriptor(className);
    }

    /**
     * @deprecated Use {@link DescriptorFactory#createClassDescriptor(String[])}
     *             instead
     */
    @Deprecated
    public static ClassDescriptor[] createClassDescriptor(String[] classNames) {
        return DescriptorFactory.createClassDescriptor(classNames);
    }

    /**
     * @deprecated Use
     *             {@link DescriptorFactory#createClassDescriptorFromDottedClassName(String)}
     *             instead
     */
    @Deprecated
    public static ClassDescriptor createClassDescriptorFromDottedClassName(String dottedClassName) {
        return DescriptorFactory.createClassDescriptorFromDottedClassName(dottedClassName);
    }

    /**
     * @deprecated Use
     *             {@link DescriptorFactory#createClassDescriptor(JavaClass)}
     *             instead
     */
    @Deprecated
    public static ClassDescriptor createClassDescriptor(JavaClass c) {
        return DescriptorFactory.createClassDescriptor(c);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return className;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public final boolean equals(Object obj) {
        if (!(obj instanceof ClassDescriptor)) {
            return false;
        }

        // All instances of ClassDescriptor should be considered
        // equal if they represent the same class,
        // even if compared to an object of a different runtime class.
        return getClassName().equals(((ClassDescriptor) obj).getClassName());
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return className.hashCode();
    }

    /**
     * Throw a ClassNotFoundException to indicate that class named by given
     * ClassDescriptor cannot be found. The exception message is formatted in a
     * way that can be decoded by ClassNotFoundExceptionParser.
     *
     * @param classDescriptor
     *            ClassDescriptor naming a class that cannot be found
     * @throws ClassNotFoundException
     * @see edu.umd.cs.findbugs.ba.ClassNotFoundExceptionParser
     */
    public static void throwClassNotFoundException(ClassDescriptor classDescriptor) throws ClassNotFoundException {
        throw new ClassNotFoundException("Class " + classDescriptor.toDottedClassName() + " cannot be resolved");
    }

    public boolean isAnonymousClass() {
        return ANONYMOUS_CLASS_NAME.matcher(className).matches();
    }
}
