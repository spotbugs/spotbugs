/*
 * Bytecode analysis framework
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

import org.apache.bcel.Constants;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

/**
 * A JavaClass and a Method belonging to the class. This is useful for answering
 * a method lookup query which must concretely identify both the class and the
 * method.
 *
 * @author David Hovemeyer
 */
public class JavaClassAndMethod {
    private final JavaClass javaClass;

    private final Method method;

    /**
     * Constructor.
     *
     * @param javaClass
     *            the JavaClass
     * @param method
     *            a Method belonging to the JavaClass
     */
    public JavaClassAndMethod(JavaClass javaClass, Method method) {
        this.javaClass = javaClass;
        this.method = method;
    }

    /**
     * Constructor.
     *
     * @param method
     *            an XMethod specifying a specific method in a specific class
     * @throws ClassNotFoundException
     */
    public JavaClassAndMethod(XMethod method) throws ClassNotFoundException {

        this.javaClass = Repository.lookupClass(method.getClassName());
        for (Method m : javaClass.getMethods()) {
            if (m.getName().equals(method.getName()) && m.getSignature().equals(method.getSignature())
                    && m.isStatic() == method.isStatic()) {
                this.method = m;
                return;
            }
        }
        throw new IllegalArgumentException("Can't find " + method);
    }

    /**
     * Get the JavaClass.
     */
    public JavaClass getJavaClass() {
        return javaClass;
    }

    /**
     * Get the Method.
     */
    public Method getMethod() {
        return method;
    }

    /**
     * Convert to an XMethod.
     */
    public XMethod toXMethod() {
        return XFactory.createXMethod(javaClass, method);
    }

    /**
     * Get the MethodDescriptor that (hopefully) uniqely names this method.
     *
     * @return the MethodDescriptor uniquely naming this method
     */
    public MethodDescriptor toMethodDescriptor() {
        return DescriptorFactory.instance().getMethodDescriptor(getSlashedClassName(), method.getName(), method.getSignature(),
                method.isStatic());
    }

    private String getSlashedClassName() {
        return javaClass.getConstantPool().getConstantString(javaClass.getClassNameIndex(), Constants.CONSTANT_Class);
    }

    @Override
    public int hashCode() {
        return javaClass.hashCode() + method.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        JavaClassAndMethod other = (JavaClassAndMethod) obj;
        return javaClass.equals(other.javaClass) && method.equals(other.method);
    }

    @Override
    public String toString() {
        return SignatureConverter.convertMethodSignature(javaClass, method);
    }
}
