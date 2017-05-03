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
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;

/**
 * This class implements a best-effort visitation of all methods overridden by a
 * given derived instance method. Objects extending this class can be used with
 * the
 * {@link Subtypes2#traverseSupertypes(ClassDescriptor, InheritanceGraphVisitor)}
 * method.
 *
 * @author David Hovemeyer
 */
public abstract class OverriddenMethodsVisitor implements SupertypeTraversalVisitor {
    private final XMethod xmethod;

    /**
     * Constructor.
     *
     * @param xmethod
     *            a derived method
     */
    public OverriddenMethodsVisitor(XMethod xmethod) {
        assert !xmethod.isStatic();
        this.xmethod = xmethod;
    }

    /**
     * @return Returns the xmethod.
     */
    public XMethod getXmethod() {
        return xmethod;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * edu.umd.cs.findbugs.ba.ch.InheritanceGraphVisitor#visitClass(edu.umd.
     * cs.findbugs.classfile.ClassDescriptor, edu.umd.cs.findbugs.ba.XClass)
     */
    @Override
    public boolean visitClass(ClassDescriptor classDescriptor, XClass xclass) {
        assert xclass != null;
        String methodSignature = xmethod.getSignature();
        XMethod bridgedFrom = xmethod.bridgeFrom();
        // See if this class has an overridden method

        XMethod xm = xclass.findMethod(xmethod.getName(), methodSignature, false);

        if (xm == null && bridgedFrom != null && !classDescriptor.equals(xmethod.getClassDescriptor())) {
            methodSignature = bridgedFrom.getSignature();
            xm = xclass.findMethod(xmethod.getName(), methodSignature, false);
        }

        if (xm != null) {
            return visitOverriddenMethod(xm) || bridgedFrom != null;
        } else {
            // Even though this particular class doesn't contain the method
            // we're
            // looking for, a superclass might, so we need to keep going.
            return true;
        }
    }

    /**
     * Downcall method: will be called for each method overridden by the derived
     * method object passed to the constructor. Note that this method will be
     * called <em>for</em> the original derived method, since this is useful for
     * some applications.
     *
     * @param xmethod
     *            a method which is overridden by the original derived method,
     *            or is the original derived method
     * @return true if the traversal should continue into superclasses, false
     *         otherwise
     */
    protected abstract boolean visitOverriddenMethod(XMethod xmethod);
}
