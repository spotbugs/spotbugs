/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003,2004 University of Maryland
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

package edu.umd.cs.findbugs.ba.bcp;

import javax.annotation.Nullable;

import edu.umd.cs.findbugs.ba.vna.ValueNumber;

public class FieldVariable implements Variable {
    private final ValueNumber ref;

    private final String className;

    private final String fieldName;

    private final String fieldSig;

    /**
     * Constructor for static fields.
     *
     * @param className
     *            the class name
     * @param fieldName
     *            the field name
     * @param fieldSig
     *            the field signature
     */
    public FieldVariable(String className, String fieldName, String fieldSig) {
        this(null, className, fieldName, fieldSig);
    }

    /**
     * Constructor for instance fields.
     *
     * @param ref
     *            ValueNumber of the object reference
     * @param className
     *            the class name
     * @param fieldName
     *            the field name
     * @param fieldSig
     *            the field signature
     */
    public FieldVariable(@Nullable ValueNumber ref, String className, String fieldName, String fieldSig) {
        this.ref = ref;
        this.className = className;
        this.fieldName = fieldName;
        this.fieldSig = fieldSig;
    }

    /**
     * Return whether or not this is a static field.
     */
    public boolean isStatic() {
        return ref == null;
    }

    /**
     * Get the class name.
     */
    public String getClassName() {
        return className;
    }

    /**
     * Get the field name.
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Get the field signature.
     */
    public String getFieldSig() {
        return fieldSig;
    }

    @Override
    public boolean sameAs(Variable other) {
        if (!(other instanceof FieldVariable)) {
            return false;
        }
        FieldVariable otherField = (FieldVariable) other;
        if (isStatic() != otherField.isStatic()) {
            return false;
        }
        return (ref == null || ref.equals(otherField.ref)) && className.equals(otherField.className)
                && fieldName.equals(otherField.fieldName) && fieldSig.equals(otherField.fieldSig);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(className);
        buf.append('.');
        buf.append(fieldName);
        return buf.toString();
    }
}

