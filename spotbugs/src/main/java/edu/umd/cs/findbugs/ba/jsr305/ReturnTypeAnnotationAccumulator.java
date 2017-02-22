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

package edu.umd.cs.findbugs.ba.jsr305;

import edu.umd.cs.findbugs.ba.XMethod;

/**
 * Accumulate relevant return type annotations for a given method by traversing
 * its supertypes.
 *
 * @author David Hovemeyer
 */
class ReturnTypeAnnotationAccumulator extends AbstractMethodAnnotationAccumulator {
    private final TypeQualifierAnnotationLookupResult result;

    private boolean overrides = false;

    /**
     * Constructor.
     *
     * @param typeQualifierValue
     *            TypeQualifierValue specifying the kind of annotation we want
     *            to look up
     * @param xmethod
     *            method whose effective return type annotation we want
     */
    public ReturnTypeAnnotationAccumulator(TypeQualifierValue<?> typeQualifierValue, XMethod xmethod) {
        super(typeQualifierValue, xmethod);
        this.result = new ReturnTypeAnnotationLookupResult();
    }

    @Override
    public TypeQualifierAnnotationLookupResult getResult() {
        return result;
    }

    /**
     * Returns true if the method overrides/implements a method in a superclass
     * or interface
     */
    @Override
    public boolean overrides() {
        return overrides;
    }

    @Override
    protected TypeQualifierAnnotation lookupAnnotation(XMethod xm) {
        overrides = true;
        return TypeQualifierApplications.getEffectiveTypeQualifierAnnotation(xm, getTypeQualifierValue());
    }

}
