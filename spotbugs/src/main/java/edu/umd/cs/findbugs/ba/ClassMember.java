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
package edu.umd.cs.findbugs.ba;

import java.io.Serializable;

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.classfile.FieldOrMethodName;
import edu.umd.cs.findbugs.internalAnnotations.DottedClassName;

/**
 * Common super-interface for class members (fields and methods).
 *
 * @see edu.umd.cs.findbugs.ba.XField
 * @see edu.umd.cs.findbugs.ba.XMethod
 * @author David Hovemeyer
 */
public interface ClassMember extends Serializable, AccessibleEntity,  FieldOrMethodName {

    /**
     * Get the full (dotted) name of the class (if the object represents a
     * class) or the class the entity is defined in (if a field or method).
     */
    public @DottedClassName
    String getClassName();

    /**
     * Get the (dotted) name of the package in which the entity is defined.
     */
    public @DottedClassName
    String getPackageName();

    /**
     * Get the name of the field/method.
     */
    @Override
    public String getName();

    /**
     * Get the signature representing the field/method's type.
     */
    @Override
    public String getSignature();

    /**
     * Get the signature representing the field/method's type, including generic
     * type
     */
    public @CheckForNull
    String getSourceSignature();

    /**
     * Did we find a declaration of this entity?
     */
    public boolean isResolved();

}
