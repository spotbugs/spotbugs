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
import edu.umd.cs.findbugs.classfile.ClassDescriptor;

/**
 * Accumulate type qualifier annotations on method,
 * taking supertype methods into account. 
 * 
 * @author David Hovemeyer
 */
public abstract class AbstractMethodAnnotationAccumulator implements InheritanceGraphVisitor {
	private final TypeQualifierValue typeQualifierValue;
	private final XMethod xmethod;

	protected AbstractMethodAnnotationAccumulator(TypeQualifierValue typeQualifierValue, XMethod xmethod) {
		this.typeQualifierValue= typeQualifierValue;
		this.xmethod = xmethod;
	}
	
	/**
	 * @return Returns the typeQualifierValue.
	 */
	public TypeQualifierValue getTypeQualifierValue() {
		return typeQualifierValue;
	}

	/**
	 * @return Returns the xmethod.
	 */
	public XMethod getXmethod() {
		return xmethod;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.ch.InheritanceGraphVisitor#visitClass(edu.umd.cs.findbugs.classfile.ClassDescriptor, edu.umd.cs.findbugs.ba.XClass)
	 */
	public boolean visitClass(ClassDescriptor classDescriptor, XClass xclass) {
		assert xclass != null;

		// See if this class has a matching method
		XMethod xm = xclass.findMethod(xmethod.getName(), xmethod.getSignature(), false);
		if (xm == null) {
			// No - end this branch of the search
			return false;
		}

		// See if matching method is annotated
		TypeQualifierAnnotation tqa = lookupAnnotation(xm);
		if (tqa == null) {
			// continue search in supertype
			return true;
		} else {
			// This branch of search ends here.
			// Add partial result.
			getResult().addPartialResult(new TypeQualifierAnnotationLookupResult.PartialResult(xm, tqa));
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.ch.InheritanceGraphVisitor#visitEdge(edu.umd.cs.findbugs.classfile.ClassDescriptor, edu.umd.cs.findbugs.ba.XClass, edu.umd.cs.findbugs.classfile.ClassDescriptor, edu.umd.cs.findbugs.ba.XClass)
	 */
	public boolean visitEdge(ClassDescriptor sourceDesc, XClass source, ClassDescriptor targetDesc, XClass target) {
		return (target != null);
	}

	public abstract TypeQualifierAnnotationLookupResult getResult();
	protected abstract TypeQualifierAnnotation lookupAnnotation(XMethod xm);
}
