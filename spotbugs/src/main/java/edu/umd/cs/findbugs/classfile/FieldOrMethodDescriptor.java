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

import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;

/**
 * Common superclass for FieldDescriptor and MethodDescriptor.
 *
 * @author David Hovemeyer
 */
public abstract class FieldOrMethodDescriptor implements FieldOrMethodName {

    private final @SlashedClassName
    String slashedClassName;

    private final String name;

    private final String signature;

    private final boolean isStatic;

    private int cachedHashCode;

    private final int nameSigHashCode;

    public FieldOrMethodDescriptor(@SlashedClassName String slashedClassName, String name, String signature, boolean isStatic) {
        assert slashedClassName.indexOf('.') == -1 : "class name not in VM format: " + slashedClassName;

        this.slashedClassName = DescriptorFactory.canonicalizeString(slashedClassName);
        this.name = DescriptorFactory.canonicalizeString(name);
        this.signature = DescriptorFactory.canonicalizeString(signature);
        this.isStatic = isStatic;
        this.nameSigHashCode = getNameSigHashCode(this.name, this.signature);
    }

    public static int getNameSigHashCode(String name, String signature) {
        return name.hashCode() * 3119 + signature.hashCode() * 131;
    }

    public int getNameSigHashCode() {
        return nameSigHashCode;
    }

    /**
     *
     *
     * @return Returns the class name
     */
    public @SlashedClassName
    String getSlashedClassName() {
        return slashedClassName;
    }

    /**
     * @return a ClassDescriptor for the method's class
     */
    @Override
    public ClassDescriptor getClassDescriptor() {
        return DescriptorFactory.createClassDescriptor(slashedClassName);
    }

    /**
     * @return Returns the method name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * @return Returns the method signature
     */
    @Override
    public String getSignature() {
        return signature;
    }

    /**
     * @return Returns true if method is static, false if not
     */
    @Override
    public boolean isStatic() {
        return isStatic;
    }

    protected int compareTo(FieldOrMethodName o) {
        int cmp;
        cmp = this.getClassDescriptor().compareTo(o.getClassDescriptor());
        if (cmp != 0) {
            return cmp;
        }
        cmp = this.name.compareTo(o.getName());
        if (cmp != 0) {
            return cmp;
        }
        cmp = this.signature.compareTo(o.getSignature());
        if (cmp != 0) {
            return cmp;
        }
        return (this.isStatic ? 1 : 0) - (o.isStatic() ? 1 : 0);
    }


    protected boolean haveEqualFields(FieldOrMethodDescriptor other) {
        return this.isStatic == other.isStatic && this.slashedClassName.equals(other.slashedClassName)
                && this.name.equals(other.name) && this.signature.equals(other.signature);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public final int hashCode() {
        if (cachedHashCode == 0) {
            cachedHashCode = slashedClassName.hashCode() * 7919 + nameSigHashCode + (isStatic ? 1 : 0);
        }
        return cachedHashCode;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return (isStatic ? "static " : "") + getClassDescriptor().getDottedClassName() + "." + name + signature;
    }

    public static int compareTo(FieldOrMethodDescriptor thas, FieldOrMethodDescriptor that) {
        int result = thas.slashedClassName.compareTo(that.slashedClassName);
        if (result != 0) {
            return result;
        }
        result = thas.name.compareTo(that.name);
        if (result != 0) {
            return result;
        }
        result = thas.signature.compareTo(that.signature);
        if (result != 0) {
            return result;
        }
        result = (thas.isStatic ? 1 : 0) - (that.isStatic ? 1 : 0);
        return result;
    }
}
