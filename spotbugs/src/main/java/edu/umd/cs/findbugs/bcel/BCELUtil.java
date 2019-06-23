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

import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.FieldOrMethod;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.classfile.Synthetic;
import org.apache.bcel.generic.FieldGenOrMethodGen;
import org.apache.bcel.generic.ObjectType;

import edu.umd.cs.findbugs.bytecode.MemberUtils;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;
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
     * @param jclass
     *            a JavaClass
     * @param method
     *            a Method belonging to the JavaClass
     * @return a MethodDescriptor identifying the method
     */
    public static MethodDescriptor getMethodDescriptor(JavaClass jclass, Method method) {
        return DescriptorFactory.instance().getMethodDescriptor(jclass.getClassName().replace('.', '/'), method.getName(),
                method.getSignature(), method.isStatic());
    }

    /**
     * Construct a ClassDescriptor from a JavaClass.
     *
     * @param jclass
     *            a JavaClass
     * @return a ClassDescriptor identifying that JavaClass
     */
    public static ClassDescriptor getClassDescriptor(JavaClass jclass) {
        return DescriptorFactory.instance().getClassDescriptor(ClassName.toSlashedClassName(jclass.getClassName()));
    }

    private static final int JDK15_MAJOR = 48;

    private static final int JDK15_MINOR = 0;

    /**
     * Checks if classfile was compiled for pre 1.5 target
     */
    public static boolean preTiger(JavaClass jclass) {
        return jclass.getMajor() < JDK15_MAJOR || (jclass.getMajor() == JDK15_MAJOR && jclass.getMinor() < JDK15_MINOR);

    }

    public static ObjectType getObjectTypeInstance(@DottedClassName String className) {
        return ObjectType.getInstance(className);
    }

    /**
     * Checks if the given member is synthetic
     *
     * @param m The member to be checked
     * @return True if the member is synthetic, false otherwise
     * @deprecated You probably don't care for synthetic members, but want to
     *             know if the developer added it (think of lambdas), use
     *             {@link MemberUtils#isUserGenerated(FieldOrMethod)} instead
     */
    @Deprecated
    public static boolean isSynthetic(FieldOrMethod m) {
        if (m.isSynthetic()) {
            return true;
        }

        for (Attribute a : m.getAttributes()) {
            if (a instanceof Synthetic) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSynthetic(JavaClass j) {
        if (j.isSynthetic()) {
            return true;
        }

        for (Attribute a : j.getAttributes()) {
            if (a instanceof Synthetic) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSynthetic(FieldGenOrMethodGen m) {
        if (m.isSynthetic()) {
            return true;
        }
        for (Attribute a : m.getAttributes()) {
            if (a instanceof Synthetic) {
                return true;
            }
        }
        return false;
    }
}
