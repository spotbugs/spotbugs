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
import javax.annotation.Nullable;

import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.graph.AbstractVertex;

/**
 * Vertex class - represents a class or interface in the InheritanceGraph. Edges
 * connect subtypes to supertypes.
 *
 * @author David Hovemeyer
 */
class ClassVertex extends AbstractVertex<InheritanceEdge, ClassVertex> {
    private static final int FINISHED = 1;

    private static final int APPLICATION_CLASS = 2;

    private static final int INTERFACE = 4;

    private final ClassDescriptor classDescriptor;

    private final @CheckForNull
    XClass xclass;

    private int flags;

    private ClassVertex directSuperclass;

    @Override
    public String toString() {
        return classDescriptor.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ClassVertex)) {
            return false;
        }
        return classDescriptor.equals(((ClassVertex) o).classDescriptor);
    }

    @Override
    public int hashCode() {
        return classDescriptor.hashCode();
    }

    private ClassVertex(ClassDescriptor classDescriptor, XClass xclass) {
        this.classDescriptor = classDescriptor;
        this.xclass = xclass;
        this.flags = 0;
        if (xclass.isInterface()) {
            setInterface();
        }
    }

    private ClassVertex(ClassDescriptor classDescriptor, boolean isInterfaceEdge) {
        this.classDescriptor = classDescriptor;
        this.xclass = null;
        this.flags = 0;
        if (isInterfaceEdge) {
            setInterface();
        }
    }

    /**
     * Factory method for resolved ClassVertex objects.
     *
     * @param classDescriptor
     *            ClassDescriptor naming the class or interface
     * @param xclass
     *            object containing information about a class or interface
     * @return ClassVertex
     */
    public static ClassVertex createResolvedClassVertex(ClassDescriptor classDescriptor, XClass xclass) {
        return new ClassVertex(classDescriptor, xclass);
    }

    /**
     * Factory method for ClassVertex objects representing missing classes.
     *
     * @param classDescriptor
     *            ClassDescriptor naming the missing class or interface
     * @param isInterface
     *            true if missing class is an interface, false otherwise
     * @return ClassVertex
     */
    public static ClassVertex createMissingClassVertex(ClassDescriptor classDescriptor, boolean isInterface) {
        return new ClassVertex(classDescriptor, isInterface);
    }

    /**
     * @return Returns the classDescriptor.
     */
    public ClassDescriptor getClassDescriptor() {
        return classDescriptor;
    }

    /**
     * @return Returns the xClass.
     */
    public @Nullable
    XClass getXClass() {
        return xclass;
    }

    /**
     * Return true if this ClassVertex corresponds to a resolved class, or false
     * if the class could not be found.
     */
    public boolean isResolved() {
        return xclass != null;
    }

    /**
     * @param finished
     *            The finished to set.
     */
    public void setFinished(boolean finished) {
        setFlag(FINISHED, finished);
    }

    /**
     * @return Returns the finished.
     */
    public boolean isFinished() {
        return isFlagSet(FINISHED);
    }

    /**
     * Mark this ClassVertex as representing an application class.
     */
    public void markAsApplicationClass() {
        setFlag(APPLICATION_CLASS, true);
    }

    /**
     * @return true if this ClassVertex represents an application class, false
     *         otherwise
     */
    public boolean isApplicationClass() {
        return isFlagSet(APPLICATION_CLASS);
    }

    /**
     * Mark this ClassVertex as representing an interface.
     */
    private void setInterface() {
        setFlag(INTERFACE, true);
    }

    /**
     * @return true if this ClassVertex represents an interface, false otherwise
     */
    public boolean isInterface() {
        return isFlagSet(INTERFACE);
    }

    /**
     * Set the ClassVertex representing the direct superclass.
     *
     * @param target
     *            ClassVertex representing the direct superclass.
     */
    public void setDirectSuperclass(ClassVertex target) {
        this.directSuperclass = target;
    }

    /**
     * @return Returns the directSuperclass.
     */
    public ClassVertex getDirectSuperclass() {
        return directSuperclass;
    }

    private void setFlag(int flag, boolean enable) {
        if (enable) {
            flags |= flag;
        } else {
            flags &= ~flag;
        }
    }

    private boolean isFlagSet(int flag) {
        return (flags & flag) != 0;
    }
}
