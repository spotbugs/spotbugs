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

import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;

/**
 * Visitor interface for simple traversals of the inheritance graph.
 *
 * @see Subtypes2
 * @author Tobias Baum
 */
public interface SupertypeTraversalVisitor {
    /**
     * Visit a class vertex in the inheritance graph.
     * Only called for classes that could be resolved, not called for java.lang.Object.
     *
     * @param classDescriptor
     *            ClassDescriptor of the class vertex
     * @param xclass
     *            XClass object containing information about the class; never null
     *
     * @return true if traversal should continue, false otherwise
     */
    public boolean visitClass(ClassDescriptor classDescriptor, XClass xclass);

}
