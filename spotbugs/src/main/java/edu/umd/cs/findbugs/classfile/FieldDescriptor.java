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

import edu.umd.cs.findbugs.ba.ComparableField;
import edu.umd.cs.findbugs.internalAnnotations.SlashedClassName;

/**
 * Descriptor uniquely identifying a field in a class.
 *
 * @author David Hovemeyer
 */
public class FieldDescriptor extends FieldOrMethodDescriptor implements ComparableField {

    /**
     * Constructor.
     *
     * @param className
     *            the name of the class the field belongs to
     * @param fieldName
     *            the name of the field
     * @param fieldSignature
     *            the field signature (type)
     * @param isStatic
     *            true if field is static, false if not
     */
    public FieldDescriptor(@SlashedClassName String className, String fieldName, String fieldSignature, boolean isStatic) {
        super(className, fieldName, fieldSignature, isStatic);
    }

    @Override
    public String toString() {
        return (isStatic() ? "static " : "") + getClassDescriptor().getDottedClassName() + "." + getName() + " " + getSignature();
    }

    @Override
    public int compareTo(ComparableField o) {
        return FieldOrMethodDescriptor.compareTo(this, (FieldDescriptor)o);
    }

    @Override
    public final boolean equals(Object obj) {
        if (obj instanceof FieldDescriptor) {
            return haveEqualFields((FieldDescriptor) obj);
        }
        return false;
    }

}
