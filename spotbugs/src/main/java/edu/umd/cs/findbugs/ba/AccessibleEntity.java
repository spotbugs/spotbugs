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

package edu.umd.cs.findbugs.ba;

import edu.umd.cs.findbugs.classfile.ClassDescriptor;

/**
 * Common superinterface for code entities having access flags: classes, fields,
 * and methods.
 *
 * @author David Hovemeyer
 */
public interface AccessibleEntity {

    /**
     * Get the entity's access flags.
     */
    public int getAccessFlags();

    /**
     * Is the entity static?
     */
    public boolean isStatic();

    /**
     * Is the entity final?
     */
    public boolean isFinal();

    /**
     * Is the entity public?
     */
    public boolean isPublic();

    /**
     * Is the entity protected?
     */
    public boolean isProtected();

    /**
     * Is the entity private?
     */
    public boolean isPrivate();

    /**
     * Is the entity synthetic?
     */
    public boolean isSynthetic();

    /**
     * Is the entity deprecated?
     */
    public boolean isDeprecated();

    /**
     * Get the ClassDescriptor representing the class (if entity is a class) or
     * the class containing the entity (if a field or method).
     */
    public ClassDescriptor getClassDescriptor();

}
