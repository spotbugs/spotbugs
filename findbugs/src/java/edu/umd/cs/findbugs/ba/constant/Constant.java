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
package edu.umd.cs.findbugs.ba.constant;

import javax.annotation.Nullable;

/**
 * Abstract dataflow value representing a value which may or may not be a
 * constant.
 *
 * @see edu.umd.cs.findbugs.ba.constant.ConstantAnalysis
 * @author David Hovemeyer
 */
public class Constant {
    private final Object value;

    /**
     * Single instance representing all non-constant values.
     */
    public static final Constant NOT_CONSTANT = new Constant(null);

    /**
     * Constructor for a constant value.
     *
     * @param value
     *            the constant value; must be a String, Integer, etc.
     */
    public Constant(@Nullable Object value) {
        this.value = value;
    }

    public Object getConstantValue() {
        return value;
    }
    /**
     * Return whether or not this value is a constant.
     *
     * @return true if the value is a constant, false if not
     */
    public boolean isConstant() {
        return value != null;
    }

    /**
     * Return whether or not this value is a constant String.
     *
     * @return true if the value is a constant String, false if not
     */
    public boolean isConstantString() {
        return isConstant() && (value instanceof String);
    }

    /**
     * Get the constant String value of this value.
     *
     * @return the constant String value
     */
    public String getConstantString() {
        return (String) value;
    }

    /**
     * Return whether or not this value is a constant int/Integer.
     *
     * @return true if the value is a constant int/Integer, false if not
     */
    public boolean isConstantInteger() {
        return isConstant() && (value instanceof Integer);
    }

    /**
     * Get the constant int value of this value.
     *
     * @return the constant int value
     */
    public int getConstantInt() {
        return ((Integer) value).intValue();
    }

    /**
     * Merge two Constants.
     *
     * @param a
     *            a StaticConstant
     * @param b
     *            another StaticConstant
     * @return the merge (dataflow meet) of the two Constants
     */
    public static Constant merge(Constant a, Constant b) {
        if (!a.isConstant() || !b.isConstant()) {
            return NOT_CONSTANT;
        }
        if (a.value.getClass() != b.value.getClass() || !a.value.equals(b.value)) {
            return NOT_CONSTANT;
        }
        return a;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        Constant other = (Constant) obj;
        if (other.value == this.value) {
            return true;
        } else if (other.value == null || this.value == null) {
            return false;
        } else {
            return this.value.equals(other.value);
        }
    }

    @Override
    public int hashCode() {
        return (value == null) ? 123 : value.hashCode();
    }

    @Override
    public String toString() {
        if (!isConstant()) {
            return "-";
        } else {
            return "<" + value.toString() + ">";
        }
    }
}
