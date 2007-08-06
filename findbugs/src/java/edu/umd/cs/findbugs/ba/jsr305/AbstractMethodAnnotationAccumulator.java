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

import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ch.InheritanceGraphVisitor;
import edu.umd.cs.findbugs.ba.ch.OverriddenMethodsVisitor;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;

/**
 * Accumulate type qualifier annotations on method,
 * taking supertype methods into account. 
 * 
 * @author David Hovemeyer
 */
public abstract class AbstractMethodAnnotationAccumulator extends OverriddenMethodsVisitor {
	private final TypeQualifierValue typeQualifierValue;

	protected AbstractMethodAnnotationAccumulator(TypeQualifierValue typeQualifierValue, XMethod xmethod) {
		super(xmethod);
		this.typeQualifierValue= typeQualifierValue;
	}
	
	/**
	 * @return Returns the typeQualifierValue.
	 */
	public TypeQualifierValue getTypeQualifierValue() {
		return typeQualifierValue;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.ch.OverriddenMethodsVisitor#visitOverriddenMethod(edu.umd.cs.findbugs.ba.XMethod)
	 */
	@Override
	protected boolean visitOverriddenMethod(XMethod xmethod) {
		
		// If xmethod is the method where the visitation begins,
		// then we don't want to try to compute the effective annotation
		// (since that would cause an infinite recursion).
		// Instead, continue to supertype methods.
		// XXX: hack for now
		if ((this instanceof ReturnTypeAnnotationAccumulator) &&
				xmethod == getXmethod()) {
			return true;
		}
		
		// See if matching method is annotated
		TypeQualifierAnnotation tqa = lookupAnnotation(xmethod);
		if (tqa == null) {
			// continue search in supertype
			return true;
		} else {
			// This branch of search ends here.
			// Add partial result.
			getResult().addPartialResult(new TypeQualifierAnnotationLookupResult.PartialResult(xmethod, tqa));
			return false;
		}
	}

	public abstract TypeQualifierAnnotationLookupResult getResult();
	protected abstract TypeQualifierAnnotation lookupAnnotation(XMethod xm);
}
