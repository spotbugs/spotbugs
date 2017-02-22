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

package edu.umd.cs.findbugs.ba.type;

import org.apache.bcel.generic.Type;

/**
 * Special "top" type. It is the identity element for the type merge operation.
 *
 * @author David Hovemeyer
 * @see TypeAnalysis
 * @see TypeFrame
 * @see TypeMerger
 */
public class TopType extends Type implements ExtendedTypes {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    private static final Type theInstance = new TopType();

    private TopType() {
        super(T_TOP, "<top>");
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    @Override
    public boolean equals(Object o) {
        return o == this;
    }

    /**
     * Get the single instance of the top type.
     */
    public static Type instance() {
        return theInstance;
    }
}

