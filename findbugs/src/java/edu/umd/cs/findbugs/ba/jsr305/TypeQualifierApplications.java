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

import java.lang.annotation.ElementType;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.meta.When;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ch.InheritanceGraphVisitor;
import edu.umd.cs.findbugs.classfile.analysis.AnnotatedObject;
import edu.umd.cs.findbugs.classfile.analysis.AnnotationValue;
import edu.umd.cs.findbugs.classfile.analysis.EnumValue;
import edu.umd.cs.findbugs.util.DualKeyHashMap;

/**
 * Figure out where and how type qualifier annotations are applied.
 * 
 * @author William Pugh
 * @author David Hovemeyer
 */
public class TypeQualifierApplications {
	static final boolean DEBUG = SystemProperties.getBoolean("ctq.applications.debug");
	
	static Map<AnnotatedObject, Collection<AnnotationValue>> objectAnnotations = new HashMap<AnnotatedObject, Collection<AnnotationValue>>();
	static DualKeyHashMap<XMethod, Integer, Collection<AnnotationValue>> parameterAnnotations 
	= new DualKeyHashMap<XMethod, Integer, Collection<AnnotationValue>>();
	
	/**
	 * Get the direct annotations (if any) on given AnnotatedObject.
	 * 
	 * @param m an AnnotatedObject
	 * @return Collection of AnnotationValues representing annotations directly
	 *         applied to this AnnotatedObject
	 */
	 static Collection<AnnotationValue> getDirectAnnotation(AnnotatedObject m) {
		Collection<AnnotationValue> result = objectAnnotations.get(m);
		if (result != null) return result;
		if (m.getAnnotationDescriptors().isEmpty()) return Collections.emptyList();
		result = TypeQualifierResolver.resolveTypeQualifierNicknames(m.getAnnotations());
		if (result.size() == 0) result = Collections.emptyList();
		objectAnnotations.put(m, result);
		return result;
	}
	
	/**
	 * Get the direct annotations (if any) on given method parameter.
	 * 
	 * @param m         a method
	 * @param parameter a parameter (0 == first parameter)
	 * @return Collection of AnnotationValues representing annotations directly
	 *         applied to this parameter
	 */
	static Collection<AnnotationValue> getDirectAnnotation(XMethod m, int parameter) {
		Collection<AnnotationValue> result = parameterAnnotations.get(m, parameter);
		if (result != null) return result;
		if (m.getParameterAnnotationDescriptors(parameter).isEmpty()) return Collections.emptyList();
		result = TypeQualifierResolver.resolveTypeQualifierNicknames(m.getParameterAnnotations(parameter));
		if (result.size() == 0) result = Collections.emptyList();
		parameterAnnotations.put(m, parameter, result);
		return result;
	}
	
	/**
	 * Populate a map of TypeQualifierValues to When values representing
	 * directly-applied type qualifier annotations on given
	 * method parameter. 
	 * 
	 * @param result    Map of TypeQualifierValues to When values
	 * @param o         a method
	 * @param parameter a parameter (0 == first parameter)
	 */
	static void getApplicableApplications(Map<TypeQualifierValue, When> result, XMethod o, int parameter) {
		Collection<AnnotationValue> values = getDirectAnnotation(o, parameter);
		ElementType e = ElementType.PARAMETER;
		for(AnnotationValue v : values) {
			Object a = v.getValue("applyTo");
			if (a instanceof Object[]) {
				for(Object o2 : (Object[]) a) 
					if (o2 instanceof EnumValue) { 
						EnumValue ev = (EnumValue)o2;
						if (ev.desc.getClassName().equals("java/lang/annotation/ElementType") && e.toString().equals(ev.value))
							constructTypeQualifierAnnotation(result, v);
					}
			} else 
				constructTypeQualifierAnnotation(result, v);
		}
	}
	
	/**
	 * Populate a map of TypeQualifierValues to When values representing
	 * directly-applied type qualifier annotations on given
	 * AnnotatedObject. 
	 * 
	 * @param result Map of TypeQualifierValues to When values
	 * @param o      an AnnotatedObject
	 * @param e      ElementType representing kind of annotated object
	 */
	public static void getApplicableApplications(Map<TypeQualifierValue, When> result, AnnotatedObject o, ElementType e) {
		Collection<AnnotationValue> values = getDirectAnnotation(o);
		for(AnnotationValue v : values) {
			Object a = v.getValue("applyTo");
			if (a instanceof Object[]) {
				for(Object o2 : (Object[]) a) 
					if (o2 instanceof EnumValue) { 
						EnumValue ev = (EnumValue)o2;
						if (ev.desc.getClassName().equals("java/lang/annotation/ElementType") && e.toString().equals(ev.value))
							constructTypeQualifierAnnotation(result, v);
					}
			} else if (o.getElementType().equals(e))
				constructTypeQualifierAnnotation(result, v);
		}
	}
	
	/**
	 * Resolve a raw AnnotationValue into a TypeQualifierValue/When pair,
	 * using given map to remember previously resolved instances.
	 * 
	 * @param map Map of resolved TypeQualifierValue/When pairs
	 * @param v   a raw AnnotationValue
	 */
	private static void constructTypeQualifierAnnotation(Map<TypeQualifierValue, When> map, AnnotationValue v) {
		assert map != null;
		assert v != null;
		EnumValue whenValue = (EnumValue) v.getValue("when");
		When when = whenValue == null ? When.ALWAYS : When.valueOf(whenValue.value);
		TypeQualifierValue tqv = TypeQualifierValue.getValue(v.getAnnotationClass(), v.getValue("value"));
		if (whenValue == null) {
			// The TypeQualifierValue does not have an explicit when value.
			// In this case, the type qualifier requires strict checking.
			tqv.setIsStrict();
		}
		map.put(tqv, when);
		if (DEBUG && whenValue == null) {
			System.out.println("When value unspecified for type qualifier value " + tqv);
		}
	}

	/**
	 * Populate map of TypeQualifierValues to When values
	 * for given AnnotatedObject,
	 * taking into account annotations
	 * applied to outer scopes (e.g., enclosing classes and packages.)
	 * 
	 * @param result map of TypeQualifierValues to When values
	 * @param o      an AnnotatedObject
	 * @param e      ElementType representing kind of AnnotatedObject
	 */
	static void getApplicableScopedApplications(Map<TypeQualifierValue, When> result, AnnotatedObject o, ElementType e) {
		AnnotatedObject outer = o.getContainingScope();
		if (outer != null) 
			getApplicableScopedApplications(result, outer, e);
		getApplicableApplications(result, o, e);
	}

	/**
	 * Get the collection of resolved TypeQualifierAnnotations for
	 * a given AnnotatedObject,
	 * taking into account annotations
	 * applied to outer scopes (e.g., enclosing classes and packages.)
	 * 
	 * @param o an AnnotatedObject
	 * @param e ElementType representing kind of AnnotatedObject
	 * @return Collection of resolved TypeQualifierAnnotations
	 */
	static Collection<TypeQualifierAnnotation> getApplicableScopedApplications(AnnotatedObject o, ElementType e) {
		Map<TypeQualifierValue, When> result = new HashMap<TypeQualifierValue, When>();
		getApplicableScopedApplications(result, o, e);
		return  TypeQualifierAnnotation.getValues(result);
	}
	
	/**
	 * Populate map of TypeQualifierValues to When values
	 * for given method parameter,
	 * taking into account annotations
	 * applied to outer scopes (e.g., enclosing classes and packages.)
	 * 
	 * @param result    map of TypeQualifierValues to When values
	 * @param o         a method
	 * @param parameter a parameter (0 == first parameter)
	 */
	static void getApplicableScopedApplications(Map<TypeQualifierValue, When> result, XMethod o, int parameter) {
		ElementType e = ElementType.PARAMETER;
		getApplicableScopedApplications(result, o, e);
		getApplicableApplications(result, o, parameter);
	}

	/**
	 * Get the collection of resolved TypeQualifierAnnotations for
	 * a given parameter,
	 * taking into account annotations
	 * applied to outer scopes (e.g., enclosing classes and packages.)
	 * 
	 * @param o         a method
	 * @param parameter a parameter (0 == first parameter)
	 * @return Collection of resolved TypeQualifierAnnotations
	 */
	static Collection<TypeQualifierAnnotation> getApplicableScopedApplications(XMethod o, int parameter) {
		Map<TypeQualifierValue, When> result = new HashMap<TypeQualifierValue, When>();
		ElementType e = ElementType.PARAMETER;
		getApplicableScopedApplications(result, o, e);
		getApplicableApplications(result, o, parameter);
		return TypeQualifierAnnotation.getValues(result);
	}

	/**
	 * Get the Collection of resolved TypeQualifierAnnotations representing
	 * directly applied and default (outer scope) type qualifier annotations
	 * for given AnnotatedObject.
	 * 
	 * <p>FIXME: does not properly account for inherited annotations.</p>
	 * 
	 * @param o an AnnotatedObject
	 * @return Collection of TypeQualifierAnnotations applicable to the AnnotatedObject
	 */
	public static Collection<TypeQualifierAnnotation> getApplicableApplications(AnnotatedObject o) {
		return getApplicableScopedApplications(o, o.getElementType());
	}

	/**
	 * Get the Collection of resolved TypeQualifierAnnotations representing
	 * directly applied and default (outer scope) type qualifier annotations
	 * for given method parameter.
	 * 
	 * <p>FIXME: does not properly account for inherited annotations.</p>
	 * 
	 * @param o         a method
	 * @param parameter a parameter (0 == first parameter)
	 * @return Collection of TypeQualifierAnnotations applicable to the method parameter
	 */
	public static Collection<TypeQualifierAnnotation> getApplicableApplications(XMethod o, int parameter) {
		return getApplicableScopedApplications(o, parameter);
	}
	
	/*
	 * XXX: is there a more efficient way to do this?
	 */
	static TypeQualifierAnnotation findMatchingTypeQualifierAnnotation(
			Collection<TypeQualifierAnnotation> typeQualifierAnnotations,
			TypeQualifierValue typeQualifierValue) {
		for (TypeQualifierAnnotation typeQualifierAnnotation : typeQualifierAnnotations) {
			if (typeQualifierAnnotation.typeQualifier.equals(typeQualifierValue)) {
				return typeQualifierAnnotation;
			}
		}
		return null;
	}
	
	/**
	 * Look up the type qualifier annotation(s) on given AnnotatedObject.
	 * If the AnnotatedObject is an instance method,
	 * annotations applied to supertype methods which the method overrides
	 * are considered.
	 * 
	 * @param o                  an AnnotatedObject
	 * @param typeQualifierValue a TypeQualifierValue specifying the kind of annotation we want to look up
	 * @return TypeQualifierAnnotationLookupResult summarizing the relevant TypeQualifierAnnotation(s)
	 */
	public static TypeQualifierAnnotationLookupResult lookupTypeQualifierAnnotationConsideringSupertypes(
			AnnotatedObject o,
			TypeQualifierValue typeQualifierValue) {
		if (o instanceof XMethod && !((XMethod)o).isStatic()) {
			// Instance method: accumulate return value annotations in supertypes, if any
			XMethod xmethod = (XMethod) o;
			ReturnTypeAnnotationAccumulator accumulator = new ReturnTypeAnnotationAccumulator(typeQualifierValue, xmethod);
			return accumulateSupertypeAnnotations(accumulator);
		} else {
			// Annotated object is not an instance method, so we don't have to check supertypes
			// for inherited annotations
			TypeQualifierAnnotationLookupResult result = new TypeQualifierAnnotationLookupResult();
			TypeQualifierAnnotation tqa = getDirectOrDefaultTypeQualifierAnnotation(o, typeQualifierValue);
			if (tqa != null) {
				result.addPartialResult(new TypeQualifierAnnotationLookupResult.PartialResult(o, tqa));
			}
			return result;
		}
	}
	
	static TypeQualifierAnnotation getDirectOrDefaultTypeQualifierAnnotation(AnnotatedObject o,
			TypeQualifierValue typeQualifierValue) {
		return findMatchingTypeQualifierAnnotation(getApplicableApplications(o), typeQualifierValue);
	}

	/**
	 * Look up the type qualifier annotation(s) on given method parameter.
	 * If the method is an instance method,
	 * annotations applied to supertype methods which the method overrides
	 * are considered.
	 * 
	 * @param xmethod            a method
	 * @param parameter          parameter (0 == first parameter)
	 * @param typeQualifierValue a TypeQualifierValue specifying the kind of annotation we want to look up
	 * @return TypeQualifierAnnotationLookupResult summarizing the relevant TypeQualifierAnnotation(s)
	 */
	public static TypeQualifierAnnotationLookupResult lookupTypeQualifierAnnotationConsideringSupertypes(
			XMethod o,
			int parameter,
			TypeQualifierValue typeQualifierValue) {
		
		if (o instanceof XMethod && !((XMethod) o).isStatic()) {
			XMethod xmethod = (XMethod) o;
			ParameterAnnotationAccumulator accumulator = new ParameterAnnotationAccumulator(typeQualifierValue, xmethod, parameter);
			return accumulateSupertypeAnnotations(accumulator);
		} else {
			TypeQualifierAnnotationLookupResult result = new TypeQualifierAnnotationLookupResult();
			TypeQualifierAnnotation tqa = getDirectOrDefaultTypeQualifierAnnotation(o, parameter, typeQualifierValue);
			if (tqa != null) {
				result.addPartialResult(new TypeQualifierAnnotationLookupResult.PartialResult(o, tqa));
			}
			return result;
		}
	}
	
	static TypeQualifierAnnotation getDirectOrDefaultTypeQualifierAnnotation(XMethod o, int parameter,
			TypeQualifierValue typeQualifierValue) {
		return findMatchingTypeQualifierAnnotation(getApplicableApplications(o, parameter), typeQualifierValue);
	}

	private static TypeQualifierAnnotationLookupResult accumulateSupertypeAnnotations(
			AbstractMethodAnnotationAccumulator accumulator) {
		try {
			XMethod xmethod = accumulator.getXmethod();
			AnalysisContext.currentAnalysisContext().getSubtypes2().traverseSupertypes(xmethod.getClassDescriptor(), accumulator);
		} catch (ClassNotFoundException e) {
			AnalysisContext.currentAnalysisContext().getLookupFailureCallback().reportMissingClass(e);
		}
		return accumulator.getResult();
	}

	/**
	 * Get the applicable TypeQualifierAnnotation matching given
	 * TypeQualifierValue for given AnnotatedObject.
	 * Returns null if there is no applicable annotation of the
	 * given TypeQualifierValue for this object.
	 * 
	 * @param o                  an AnnotatedObject
	 * @param typeQualifierValue a TypeQualifierValue 
	 * @return the TypeQualifierAnnotation matching the AnnotatedObject/TypeQualifierValue,
	 *         or null if there is no matching TypeQualifierAnnotation
	 */
	public static @CheckForNull TypeQualifierAnnotation getApplicableApplicationConsideringSupertypes(AnnotatedObject o, TypeQualifierValue typeQualifierValue) {
		if (DEBUG) {
			System.out.println("Looking up application of " + typeQualifierValue + " on " + o);
		}
		TypeQualifierAnnotationLookupResult lookupResult = lookupTypeQualifierAnnotationConsideringSupertypes(o, typeQualifierValue);
		if (DEBUG) {
			System.out.println("  => Answer: " + lookupResult);
		}
		return lookupResult.getEffectiveTypeQualifierAnnotation();
	}
	
	/**
	 * Get the applicable TypeQualifierAnnotation matching given
	 * TypeQualifierValue for given method parameter.
	 * Returns null if there is no applicable annotation of the
	 * given TypeQualifierValue for this parameter.
	 * 
	 * @param o                  an XMethod
	 * @param parameter          parameter number (0 for first declared parameter)
	 * @param typeQualifierValue a TypeQualifierValue 
	 * @return the TypeQualifierAnnotation matching the parameter,
	 *         or null if there is no matching TypeQualifierAnnotation
	 */
	public static @CheckForNull TypeQualifierAnnotation getApplicableApplicationConsideringSupertypes(XMethod o, int parameter, TypeQualifierValue typeQualifierValue) {
		if (DEBUG) {
			System.out.println("Looking up application of " + typeQualifierValue + " on " + o + " parameter " + parameter);
		}
		TypeQualifierAnnotationLookupResult lookupResult = lookupTypeQualifierAnnotationConsideringSupertypes(o, parameter, typeQualifierValue);
		if (DEBUG) {
			System.out.println("  => Answer: " + lookupResult);
		}
		return lookupResult.getEffectiveTypeQualifierAnnotation();
	}
}
