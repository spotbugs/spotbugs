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

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnnotationDatabase;
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
	private static final boolean DEBUG = SystemProperties.getBoolean("findbugs.npe.tq.debug");

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
		if (DEBUG) {
			System.out.print("Checking " + m + " param " + param + " for @Nonnull...");
		}
		
		TypeQualifierAnnotation tqa = TypeQualifierApplications.getEffectiveTypeQualifierAnnotation(m, param, nonnullTypeQualifierValue);
		boolean answer = (tqa != null) && tqa.when == When.ALWAYS; 
		
		if (DEBUG) {
			System.out.println(answer ? "yes" : "no");
		}
		
		return answer;
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.INullnessAnnotationDatabase#addDefaultAnnotation(java.lang.String, java.lang.String, edu.umd.cs.findbugs.ba.NullnessAnnotation)
	 */
	public void addDefaultAnnotation(String target, String c, NullnessAnnotation n) {
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.INullnessAnnotationDatabase#addDefaultMethodAnnotation(java.lang.String, edu.umd.cs.findbugs.ba.NullnessAnnotation)
	 */
	public void addDefaultMethodAnnotation(String name, NullnessAnnotation annotation) {
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.INullnessAnnotationDatabase#addFieldAnnotation(java.lang.String, java.lang.String, java.lang.String, boolean, edu.umd.cs.findbugs.ba.NullnessAnnotation)
	 */
	public void addFieldAnnotation(String name, String name2, String sig, boolean isStatic, NullnessAnnotation annotation) {
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.INullnessAnnotationDatabase#addMethodAnnotation(java.lang.String, java.lang.String, java.lang.String, boolean, edu.umd.cs.findbugs.ba.NullnessAnnotation)
	 */
	public void addMethodAnnotation(String name, String name2, String sig, boolean isStatic, NullnessAnnotation annotation) {
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.INullnessAnnotationDatabase#addMethodParameterAnnotation(java.lang.String, java.lang.String, java.lang.String, boolean, int, edu.umd.cs.findbugs.ba.NullnessAnnotation)
	 */
	public void addMethodParameterAnnotation(String name, String name2, String sig, boolean isStatic, int param,
			NullnessAnnotation annotation) {
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.INullnessAnnotationDatabase#loadAuxiliaryAnnotations()
	 */
	public void loadAuxiliaryAnnotations() {
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
