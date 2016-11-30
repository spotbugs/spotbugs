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

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.InvokeInstruction;

import edu.umd.cs.findbugs.ba.ComparableMethod;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;
import edu.umd.cs.findbugs.util.ClassName;

/**
 * Descriptor uniquely identifying a method in a class.
 *
 * @author David Hovemeyer
 */
public class MethodDescriptor extends FieldOrMethodDescriptor implements ComparableMethod {

    /**
     * Constructor.
     *
     * @param className
     *            name of the class containing the method, in VM format (e.g.,
     *            "java/lang/String")
     * @param methodName
     *            name of the method
     * @param methodSignature
     *            signature of the method
     * @param isStatic
     *            true if method is static, false otherwise
     */
    public MethodDescriptor(@SlashedClassName String className, String methodName, String methodSignature, boolean isStatic) {
        super(className, methodName, methodSignature, isStatic);
    }

    public MethodDescriptor(@SlashedClassName String className, String methodName, String methodSignature) {
        super(className, methodName, methodSignature, false);
    }

    public MethodDescriptor(InvokeInstruction iins, ConstantPoolGen cpg) {
        super(ClassName.toSlashedClassName(iins.getClassName(cpg)), iins.getMethodName(cpg), iins.getSignature(cpg), iins instanceof INVOKESTATIC);
    }

    @Override
    public int compareTo(ComparableMethod o) {
        return FieldOrMethodDescriptor.compareTo(this, (MethodDescriptor)o);
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj instanceof MethodDescriptor) {
            return haveEqualFields((MethodDescriptor) obj);
        }
        return false;
    }
}
