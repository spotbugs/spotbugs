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

package edu.umd.cs.findbugs.ba.ch;

import javax.annotation.CheckForNull;

import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;

/**
 * Visitor interface for traversals of the inheritance graph.
 *
 * @see Subtypes2
 * @author David Hovemeyer
 */
public interface InheritanceGraphVisitor {
    /**
     * Visit a class vertex in the inheritance graph.
     *
     * @param classDescriptor
     *            ClassDescriptor of the class vertex
     * @param xclass
     *            XClass object containing information about the class; null if
     *            class cannot be resolved
     * @return true if traversal should continue, false otherwise
     */
    public boolean visitClass(ClassDescriptor classDescriptor, @CheckForNull XClass xclass);

    /**
     * Visit an inheritance edge in the inheritance graph.
     *
     * @param sourceDesc
     *            ClassDescriptor of the source class (subtype)
     * @param source
     *            XClass object containing information about source class
     * @param targetDesc
     *            ClassDescriptor of the target class (supertype)
     * @param target
     *            XClass object containing information about target class; null
     *            if class cannot be resolved
     * @return true if traversal should continue, false otherwise
     */
    public boolean visitEdge(ClassDescriptor sourceDesc, XClass source, ClassDescriptor targetDesc, @CheckForNull XClass target);
}
