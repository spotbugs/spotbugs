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

package edu.umd.cs.findbugs.ba.npe;

import javax.annotation.meta.When;

import edu.umd.cs.findbugs.ba.INullnessAnnotationDatabase;
import edu.umd.cs.findbugs.ba.NullnessAnnotation;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.XMethodParameter;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierAnnotation;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierApplications;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierValue;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.analysis.AnnotatedObject;

/**
 * Implementation of INullnessAnnotationDatabase that
 * is based on JSR-305 type qualifiers.
 * 
 * @author David Hovemeyer
 */
public class TypeQualifierNullnessAnnotationDatabase implements INullnessAnnotationDatabase {

	private final TypeQualifierValue nonnullTypeQualifierValue;
	
	public TypeQualifierNullnessAnnotationDatabase() {
		ClassDescriptor nonnullClassDesc = DescriptorFactory.instance().getClassDescriptor("javax/annotation/Nonnull");
		this.nonnullTypeQualifierValue = TypeQualifierValue.getValue(nonnullClassDesc, null);
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.INullnessAnnotationDatabase#getResolvedAnnotation(java.lang.Object, boolean)
	 */
	public NullnessAnnotation getResolvedAnnotation(Object o, boolean getMinimal) {
		TypeQualifierAnnotation tqa = null;
		
		if (o instanceof XMethodParameter) {
			XMethodParameter param = (XMethodParameter) o;

			tqa = TypeQualifierApplications.getEffectiveTypeQualifierAnnotation(
					param.getMethod(), param.getParameterNumber(), nonnullTypeQualifierValue);
		} else if (o instanceof XMethod || o instanceof XField) {
			tqa = TypeQualifierApplications.getEffectiveTypeQualifierAnnotation(
					(AnnotatedObject) o, nonnullTypeQualifierValue);
		}
		
		return toNullnessAnnotation(tqa);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.INullnessAnnotationDatabase#parameterMustBeNonNull(edu.umd.cs.findbugs.ba.XMethod, int)
	 */
	public boolean parameterMustBeNonNull(XMethod m, int param) {
		TypeQualifierAnnotation tqa = TypeQualifierApplications.getEffectiveTypeQualifierAnnotation(m, param, nonnullTypeQualifierValue);
		
		if (tqa == null) {
			return false;
		}
		
		return tqa.when == When.ALWAYS; 
	}

	/**
	 * Convert a Nonnull-based TypeQualifierAnnotation
	 * into a NullnessAnnotation.
	 * 
	 * @param tqa Nonnull-based TypeQualifierAnnotation
	 * @return corresponding NullnessAnnotation
	 */
	private NullnessAnnotation toNullnessAnnotation(TypeQualifierAnnotation tqa) {
		if (tqa == null) {
			return null;
		}
		
		switch (tqa.when) {
		case ALWAYS:
			return NullnessAnnotation.NONNULL;
		case MAYBE:
			return NullnessAnnotation.CHECK_FOR_NULL;
		case NEVER:
			return NullnessAnnotation.CHECK_FOR_NULL; // FIXME: is this right?
		case UNKNOWN:
			return NullnessAnnotation.UNKNOWN_NULLNESS;
		}
		
		throw new IllegalStateException();
	}
}
