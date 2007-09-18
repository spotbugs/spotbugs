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

import java.lang.annotation.ElementType;

import javax.annotation.meta.When;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Type;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.AnnotationDatabase;
import edu.umd.cs.findbugs.ba.DefaultNullnessAnnotations;
import edu.umd.cs.findbugs.ba.INullnessAnnotationDatabase;
import edu.umd.cs.findbugs.ba.NullnessAnnotation;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.XMethodParameter;
import edu.umd.cs.findbugs.ba.AnnotationDatabase.Target;
import edu.umd.cs.findbugs.ba.jsr305.FindBugsDefaultAnnotations;
import edu.umd.cs.findbugs.ba.jsr305.JSR305NullnessAnnotations;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierAnnotation;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierApplications;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierValue;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.IAnalysisCache;
import edu.umd.cs.findbugs.classfile.MissingClassException;
import edu.umd.cs.findbugs.classfile.analysis.AnnotatedObject;
import edu.umd.cs.findbugs.classfile.analysis.AnnotationValue;
import edu.umd.cs.findbugs.classfile.analysis.ClassInfo;
import edu.umd.cs.findbugs.classfile.analysis.FieldInfo;
import edu.umd.cs.findbugs.classfile.analysis.MethodInfo;
import edu.umd.cs.findbugs.log.Profiler;

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
		Profiler profiler = Profiler.getInstance();
		profiler.start(this.getClass());
		try {
		
		if (DEBUG) {
			System.out.println("getResolvedAnnotation: o=" + o + "...");
		}
		
		TypeQualifierAnnotation tqa = null;
		
		if (o instanceof XMethodParameter) {
			XMethodParameter param = (XMethodParameter) o;

			tqa = TypeQualifierApplications.getEffectiveTypeQualifierAnnotation(
					param.getMethod(), param.getParameterNumber(), nonnullTypeQualifierValue);
		} else if (o instanceof XMethod || o instanceof XField) {
			tqa = TypeQualifierApplications.getEffectiveTypeQualifierAnnotation(
					(AnnotatedObject) o, nonnullTypeQualifierValue);
		}
		
		NullnessAnnotation result = toNullnessAnnotation(tqa);
		if (DEBUG) {
			System.out.println("   ==> " + (result != null ? result.toString() : "not found"));
		}
		return result;
		} finally {
			profiler.end(this.getClass());
		}
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
	
	// NOTE:
	// The way we handle adding default annotations is to actually add AnnotationValues
	// to the corresponding XFoo objects, giving the illusion that the annotations
	// were actually read from the underlying class files.

	/**
	 * Convert a NullnessAnnotation into the ClassDescriptor
	 * of the equivalent JSR-305 nullness type qualifier.
	 * 
	 * @param n a NullnessAnnotation
	 * @return ClassDescriptor of the equivalent JSR-305 nullness type qualifier
	 */
	private ClassDescriptor getNullnessAnnotationClassDescriptor(NullnessAnnotation n) {
		if (n == NullnessAnnotation.CHECK_FOR_NULL) {
			return JSR305NullnessAnnotations.CHECK_FOR_NULL;
		} else if (n == NullnessAnnotation.NONNULL) {
			return JSR305NullnessAnnotations.NONNULL;
		} else if (n == NullnessAnnotation.NULLABLE) {
			return JSR305NullnessAnnotations.NULLABLE;
		} else if (n == NullnessAnnotation.UNKNOWN_NULLNESS) {
			return JSR305NullnessAnnotations.NULLABLE;
		} else {
			throw new IllegalArgumentException("Unknown NullnessAnnotation: " + n);
		}
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.INullnessAnnotationDatabase#addDefaultAnnotation(java.lang.String, java.lang.String, edu.umd.cs.findbugs.ba.NullnessAnnotation)
	 */
	public void addDefaultAnnotation(Target target, String c, NullnessAnnotation n) {
		if (DEBUG) {
			System.out.println("addDefaultAnnotation: target=" + target + ", c=" + c + ", n=" + n);
		}
		
		ClassDescriptor classDesc = DescriptorFactory.instance().getClassDescriptorForDottedClassName(c);
		XClass xclass;
		
		// Get the XClass (really a ClassInfo object)
		try {
			xclass = Global.getAnalysisCache().getClassAnalysis(XClass.class, classDesc);
		} catch (MissingClassException e) {
//			AnalysisContext.currentAnalysisContext().getLookupFailureCallback().reportMissingClass(e.getClassDescriptor());
			return;
		} catch (CheckedAnalysisException e) {
//			AnalysisContext.logError("Error adding built-in nullness annotation", e);
			return;
		}
		
		// Get the default annotation type
		ClassDescriptor defaultAnnotationType;
		if (target == AnnotationDatabase.Target.ANY) {
			defaultAnnotationType = FindBugsDefaultAnnotations.DEFAULT_ANNOTATION;
		} else if (target == AnnotationDatabase.Target.FIELD) {
			defaultAnnotationType = FindBugsDefaultAnnotations.DEFAULT_ANNOTATION_FOR_FIELDS;
		} else if (target == AnnotationDatabase.Target.METHOD) {
			defaultAnnotationType = FindBugsDefaultAnnotations.DEFAULT_ANNOTATION_FOR_METHODS;
		} else if (target == AnnotationDatabase.Target.PARAMETER) {
			defaultAnnotationType = FindBugsDefaultAnnotations.DEFAULT_ANNOTATION_FOR_PARAMETERS;
		} else {
			throw new IllegalArgumentException("Unknown target for default annotation: " + target);
		}

		// Get the JSR-305 nullness annotation type 
		ClassDescriptor nullnessAnnotationType = getNullnessAnnotationClassDescriptor(n);
		
		// Construct an AnnotationValue containing the default annotation
		AnnotationValue annotationValue = new AnnotationValue(defaultAnnotationType);
		AnnotationVisitor v = annotationValue.getAnnotationVisitor();
		v.visit("value", Type.getObjectType(nullnessAnnotationType.getClassName()));
		v.visitEnd();
		
		if (DEBUG) {
			System.out.println("Adding AnnotationValue " + annotationValue + " to class " + xclass);
		}
		
		// Destructively add the annotation to the ClassInfo object
		((ClassInfo)xclass).addAnnotation(annotationValue);
	}

//	/* (non-Javadoc)
//	 * @see edu.umd.cs.findbugs.ba.INullnessAnnotationDatabase#addDefaultMethodAnnotation(java.lang.String, edu.umd.cs.findbugs.ba.NullnessAnnotation)
//	 */
//	public void addDefaultMethodAnnotation(String name, NullnessAnnotation annotation) {
//	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.INullnessAnnotationDatabase#addFieldAnnotation(java.lang.String, java.lang.String, java.lang.String, boolean, edu.umd.cs.findbugs.ba.NullnessAnnotation)
	 */
	public void addFieldAnnotation(String cName, String mName, String mSig, boolean isStatic, NullnessAnnotation annotation) {
		if (DEBUG) {
			System.out.println("addFieldAnnotation: annotate " + cName + "." + mName + " with " + annotation);
		}
		
		XField xfield = XFactory.createXField(cName, mName, mSig, isStatic);
		if (!(xfield instanceof FieldInfo)) {
			if (DEBUG) {
				System.out.println("  Field not found! " + cName +"." + mName + ":" + mSig + " " + isStatic + " " + annotation);
			}
			return;
		}
		
		// Get JSR-305 nullness annotation type
		ClassDescriptor nullnessAnnotationType = getNullnessAnnotationClassDescriptor(annotation);
		
		// Create an AnnotationValue
		AnnotationValue annotationValue = new AnnotationValue(nullnessAnnotationType);
		
		// Destructively add the annotation to the FieldInfo object
		((FieldInfo)xfield).addAnnotation(annotationValue);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.INullnessAnnotationDatabase#addMethodAnnotation(java.lang.String, java.lang.String, java.lang.String, boolean, edu.umd.cs.findbugs.ba.NullnessAnnotation)
	 */
	public void addMethodAnnotation(String cName, String mName, String sig, boolean isStatic, NullnessAnnotation annotation) {
		if (DEBUG) {
			System.out.println("addMethodAnnotation: annotate " + cName + "." + mName + " with " + annotation);
		}
		
		XMethod xmethod = XFactory.createXMethod(cName, mName, sig, isStatic);
		if (xmethod == null || !xmethod.isResolved()) {
			if (DEBUG) {
				System.out.println("  Method not found!");
			}
			return;
		}
		
		// Get JSR-305 nullness annotation type
		ClassDescriptor nullnessAnnotationType = getNullnessAnnotationClassDescriptor(annotation);
		
		// Create an AnnotationValue
		AnnotationValue annotationValue = new AnnotationValue(nullnessAnnotationType);
		
		// Destructively add the annotation to the MethodInfo object
		((MethodInfo)xmethod).addAnnotation(annotationValue);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.INullnessAnnotationDatabase#addMethodParameterAnnotation(java.lang.String, java.lang.String, java.lang.String, boolean, int, edu.umd.cs.findbugs.ba.NullnessAnnotation)
	 */
	public void addMethodParameterAnnotation(String cName, String mName, String sig, boolean isStatic, int param,
			NullnessAnnotation annotation) {
		if (DEBUG) {
			System.out.println("addMethodParameterAnnotation: annotate " + cName + "." + mName + " param " + param + " with " + annotation);
		}
		
		XMethod xmethod = XFactory.createXMethod(cName, mName, sig, isStatic);
		if (xmethod == null || !xmethod.isResolved()) {
			if (DEBUG) {
				System.out.println("  Method not found!");
			}
			return;
		}
		if (!(xmethod instanceof MethodInfo)) {
			if (false) AnalysisContext.logError("Could not fully resolve method " + cName + "." + mName + sig + " to apply annotation " + annotation);
			return;
		}
		// Get JSR-305 nullness annotation type
		ClassDescriptor nullnessAnnotationType = getNullnessAnnotationClassDescriptor(annotation);
		
		// Create an AnnotationValue
		AnnotationValue annotationValue = new AnnotationValue(nullnessAnnotationType);
		
		// Destructively add the annotation to the MethodInfo object
		((MethodInfo) xmethod).addParameterAnnotation(param, annotationValue);
	}

	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.ba.INullnessAnnotationDatabase#loadAuxiliaryAnnotations()
	 */
	public void loadAuxiliaryAnnotations() {
		DefaultNullnessAnnotations.addDefaultNullnessAnnotations(this);
	}

	/**
	 * Convert a Nonnull-based TypeQualifierAnnotation
	 * into a NullnessAnnotation.
	 * 
	 * @param tqa Nonnull-based TypeQualifierAnnotation
	 * @return corresponding NullnessAnnotation
	 */
	private NullnessAnnotation toNullnessAnnotation(@CheckForNull TypeQualifierAnnotation tqa) {
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
