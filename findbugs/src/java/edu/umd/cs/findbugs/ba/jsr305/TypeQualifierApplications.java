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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.meta.When;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ch.InheritanceGraphVisitor;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
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
	
	/** Type qualifier annotations applied directly to methods/fields/classes/etc. */
	private static Map<AnnotatedObject, Collection<AnnotationValue>> directObjectAnnotations =
		new HashMap<AnnotatedObject, Collection<AnnotationValue>>();
	
	/** Type qualifier annotations applied directly to method parameters. */
	private static DualKeyHashMap<XMethod, Integer, Collection<AnnotationValue>> directParameterAnnotations =
		new DualKeyHashMap<XMethod, Integer, Collection<AnnotationValue>>();

	/**
	 * Result from computing the "effective"
	 * TypeQualifierAnnotation on some AnnotatedObject or method parameter.
	 */
	private static class EffectiveTypeQualifierAnnotation {
		@CheckForNull TypeQualifierAnnotation typeQualifierAnnotation;
		
		public EffectiveTypeQualifierAnnotation(TypeQualifierAnnotation typeQualifierAnnotation) {
			this.typeQualifierAnnotation = typeQualifierAnnotation;
		}
	}

	/**
	 * Map of TypeQualifierValues to maps containing, for each AnnotatedObject,
	 * the effective TypeQualifierAnnotation (if any) for that AnnotatedObject.
	 */
	private static Map<TypeQualifierValue, Map<AnnotatedObject, EffectiveTypeQualifierAnnotation>> effectiveObjectAnnotations =
		new HashMap<TypeQualifierValue, Map<AnnotatedObject,EffectiveTypeQualifierAnnotation>>();

	/**
	 * Map of TypeQualifierValues to maps containing, for each XMethod/parameter,
	 * the effective TypeQualifierAnnotation (if any) for that XMethod/parameter.
	 */
	private static Map<TypeQualifierValue, DualKeyHashMap<XMethod, Integer, EffectiveTypeQualifierAnnotation>> effectiveParameterAnnotations =
		new HashMap<TypeQualifierValue, DualKeyHashMap<XMethod,Integer,EffectiveTypeQualifierAnnotation>>();

	// FindBugs-specific default-annotation annotations.
	// I.e.:
	//   @DefaultAnnotationForParameters({Nonnull.class})
	//   public class MyClass {
	//      ...
	//   }
	
	private static final ClassDescriptor DEFAULT_ANNOTATION =
		DescriptorFactory.instance().getClassDescriptor("edu/umd/cs/findbugs/annotation/DefaultAnnotation");
	private static final ClassDescriptor DEFAULT_ANNOTATION_FOR_FIELDS =
		DescriptorFactory.instance().getClassDescriptor("edu/umd/cs/findbugs/annotation/DefaultAnnotationForFields");
	private static final ClassDescriptor DEFAULT_ANNOTATION_FOR_METHODS =
		DescriptorFactory.instance().getClassDescriptor("edu/umd/cs/findbugs/annotation/DefaultAnnotationForMethods");
	private static final ClassDescriptor DEFAULT_ANNOTATION_FOR_PARAMETERS =
		DescriptorFactory.instance().getClassDescriptor("edu/umd/cs/findbugs/annotation/DefaultAnnotationForParameters");
	
	/**
	 * Get the direct annotations (if any) on given AnnotatedObject.
	 * 
	 * @param m an AnnotatedObject
	 * @return Collection of AnnotationValues representing annotations directly
	 *         applied to this AnnotatedObject
	 */
	 private static Collection<AnnotationValue> getDirectAnnotation(AnnotatedObject m) {
		Collection<AnnotationValue> result = directObjectAnnotations.get(m);
		if (result != null) return result;
		if (m.getAnnotationDescriptors().isEmpty()) return Collections.emptyList();
		result = TypeQualifierResolver.resolveTypeQualifierNicknames(m.getAnnotations());
		if (result.size() == 0) result = Collections.emptyList();
		directObjectAnnotations.put(m, result);
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
	private static Collection<AnnotationValue> getDirectAnnotation(XMethod m, int parameter) {
		Collection<AnnotationValue> result = directParameterAnnotations.get(m, parameter);
		if (result != null) return result;
		if (m.getParameterAnnotationDescriptors(parameter).isEmpty()) return Collections.emptyList();
		result = TypeQualifierResolver.resolveTypeQualifierNicknames(m.getParameterAnnotations(parameter));
		if (result.size() == 0) result = Collections.emptyList();
		directParameterAnnotations.put(m, parameter, result);
		return result;
	}
	
	/**
	 * Populate a Set of TypeQualifierAnnotations representing
	 * directly-applied type qualifier annotations on given
	 * method parameter. 
	 * 
	 * @param result    Set of TypeQualifierAnnotations
	 * @param o         a method
	 * @param parameter a parameter (0 == first parameter)
	 */
	private static void getDirectApplications(Set<TypeQualifierAnnotation> result, XMethod o, int parameter) {
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
	 * Populate a Set of TypeQualifierAnnotations representing
	 * directly-applied type qualifier annotations on given
	 * AnnotatedObject. 
	 * 
	 * @param result Set of TypeQualifierAnnotations
	 * @param o      an AnnotatedObject
	 * @param e      ElementType representing kind of annotated object
	 */
	private static void getDirectApplications(Set<TypeQualifierAnnotation> result, AnnotatedObject o, ElementType e) {
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
	 * Resolve a raw AnnotationValue into a TypeQualifierAnnotation,
	 * using given set to remember previously resolved instances.
	 * 
	 * @param set Set of resolved TypeQualifierAnnotations
	 * @param v   a raw AnnotationValue
	 */
	private static void constructTypeQualifierAnnotation(Set<TypeQualifierAnnotation> set, AnnotationValue v) {
		assert set != null;
		assert v != null;
		EnumValue whenValue = (EnumValue) v.getValue("when");
		When when = whenValue == null ? When.ALWAYS : When.valueOf(whenValue.value);
		TypeQualifierValue tqv = TypeQualifierValue.getValue(v.getAnnotationClass(), v.getValue("value"));
		if (whenValue == null) {
			// The TypeQualifierValue does not have an explicit when value.
			// In this case, the type qualifier requires strict checking.
			tqv.setIsStrict();
		}
		set.add(TypeQualifierAnnotation.getValue(tqv, when));
		if (DEBUG && whenValue == null) {
			System.out.println("When value unspecified for type qualifier value " + tqv);
		}
	}

	/**
	 * Populate Set of TypeQualifierAnnotations
	 * for given AnnotatedObject,
	 * taking into account annotations
	 * applied to outer scopes (e.g., enclosing classes and packages.)
	 * 
	 * @param result Set of TypeQualifierAnnotations
	 * @param o      an AnnotatedObject
	 * @param e      ElementType representing kind of AnnotatedObject
	 */
	private static void getApplicableScopedApplications(Set<TypeQualifierAnnotation> result, AnnotatedObject o, ElementType e) {
		AnnotatedObject outer = o.getContainingScope();
		if (outer != null) 
			getApplicableScopedApplications(result, outer, e);
		getDirectApplications(result, o, e);
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
	private static Collection<TypeQualifierAnnotation> getApplicableScopedApplications(AnnotatedObject o, ElementType e) {
		Set<TypeQualifierAnnotation> result = new HashSet<TypeQualifierAnnotation>();
		getApplicableScopedApplications(result, o, e);
		return result;
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
	private static Collection<TypeQualifierAnnotation> getApplicableScopedApplications(XMethod o, int parameter) {
		Set<TypeQualifierAnnotation> result = new HashSet<TypeQualifierAnnotation>();
		ElementType e = ElementType.PARAMETER;
		getApplicableScopedApplications(result, o, e);
		getDirectApplications(result, o, parameter);
		return result;
	}

	/**
	 * Get the Collection of resolved TypeQualifierAnnotations representing
	 * directly applied and default (outer scope) type qualifier annotations
	 * for given AnnotatedObject.
	 * 
	 * <p>NOTE: does not properly account for inherited annotations
	 * on instance methods.
	 * It is ok to call this method to find out generally-relevant TypeQualifierAnnotations,
	 * but not to find the effective TypeQualifierAnnotation.</p>
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
	 * <p>NOTE: does not properly account for inherited annotations
	 * on instance method parameters.
	 * It is ok to call this method to find out generally-relevant TypeQualifierAnnotations,
	 * but not to find the effective TypeQualifierAnnotation.</p>
	 * 
	 * @param o         a method
	 * @param parameter a parameter (0 == first parameter)
	 * @return Collection of TypeQualifierAnnotations applicable to the method parameter
	 */
	public static Collection<TypeQualifierAnnotation> getApplicableApplications(XMethod o, int parameter) {
		return getApplicableScopedApplications(o, parameter);
	}

	/**
	 * Look up a TypeQualifierAnnotation matching given TypeQualifierValue.
	 * 
	 * @param typeQualifierAnnotations a Collection of TypeQualifierAnnotations 
	 * @param typeQualifierValue       a TypeQualifierValue
	 * @return matching TypeQualifierAnnotation, or null if none
	 */
	private static @CheckForNull TypeQualifierAnnotation findMatchingTypeQualifierAnnotation(
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
	 * Check to see if one of the FindBugs-specific default annotation mechanisms
	 * is used on given AnnotatedObject to define a default value for
	 * given TypeQualifierValue. 
	 * 
     * @param o                  an AnnotatedObject
     * @param typeQualifierValue a TypeQualifierValue
	 * @param elementType        type of annotated element
     * @return default TypeQualifierAnnotation, or null if none 
     */
    private static @CheckForNull TypeQualifierAnnotation getFindBugsDefaultAnnotation(AnnotatedObject o, TypeQualifierValue typeQualifierValue, ElementType elementType) {
    	TypeQualifierAnnotation result;
    	
    	if ((result = checkFindBugsDefaultAnnotation(DEFAULT_ANNOTATION, o, typeQualifierValue)) != null) {
    		return result;
    	}

    	switch (elementType) {
    	case FIELD:
    		result = checkFindBugsDefaultAnnotation(DEFAULT_ANNOTATION_FOR_FIELDS, o, typeQualifierValue);
    	case METHOD:
    		result = checkFindBugsDefaultAnnotation(DEFAULT_ANNOTATION_FOR_METHODS, o, typeQualifierValue);
    	case PARAMETER:
    		result = checkFindBugsDefaultAnnotation(DEFAULT_ANNOTATION_FOR_PARAMETERS, o, typeQualifierValue);
    	default:
    		// ignore
    	}
    	
    	return result;
    }
	
    private static @CheckForNull TypeQualifierAnnotation checkFindBugsDefaultAnnotation(ClassDescriptor defaultAnnotation, AnnotatedObject o,
    		TypeQualifierValue typeQualifierValue) {
    	
    	// TODO:
    	// - check to see if default annotation is present; if not, return null
    	// - get value - should be array of Type
    	// - scan through array elements; see if any match the TypeQualifierValue (including type qualifier nicknames)
    	
    	return null;
    }

	/**
	 * Get the effective TypeQualifierAnnotation on given
	 * AnnotatedObject.  Takes into account inherited and
	 * default (outer scope) annotations.
	 * 
	 * @param o                  an AnnotatedObject
	 * @param typeQualifierValue a TypeQualifierValue specifying kind of annotation
	 *                           we want to look up
	 * @return the effective TypeQualifierAnnotation, or null if
	 *         there is no effective TypeQualifierAnnotation on this
	 *         AnnotatedObject
	 */
	public static TypeQualifierAnnotation getEffectiveTypeQualifierAnnotation(
			AnnotatedObject o,
			TypeQualifierValue typeQualifierValue) {
		if (DEBUG) {
			System.out.println("Looking up application of " + typeQualifierValue + " on " + o);
		}
		
		Map<AnnotatedObject, EffectiveTypeQualifierAnnotation> map = effectiveObjectAnnotations.get(typeQualifierValue);
		if (map == null) {
			map = new HashMap<AnnotatedObject, EffectiveTypeQualifierAnnotation>();
			effectiveObjectAnnotations.put(typeQualifierValue, map);
		}
		
		// Check cached answer
		EffectiveTypeQualifierAnnotation result = map.get(o);
		
		if (result == null) {
			// Compute answer
			
			TypeQualifierAnnotation tqa;

			// See if there is a direct application
			tqa = getDirectTypeQualifierAnnotation(o, typeQualifierValue);

			// If it's an instance method, check for an inherited annotation
			if (tqa == null && (o instanceof XMethod) && !((XMethod) o).isStatic()) {
				tqa = getInheritedTypeQualifierAnnotation((XMethod) o, typeQualifierValue);
			}

			// Check for a default (outer scope) annotation
			if (tqa == null) {
				tqa = getDefaultTypeQualifierAnnotation(o, typeQualifierValue);
			}
			
			// Cache computed answer
			result = new EffectiveTypeQualifierAnnotation(tqa);
			map.put(o, result);
		}
		if (DEBUG) {
			System.out.println("  => Answer: " + result.typeQualifierAnnotation);
		}

		// Return cached answer
		return result.typeQualifierAnnotation;
	}


	/**
	 * Get a directly-applied TypeQualifierAnnotation on given AnnotatedObject.
	 * 
	 * @param o                  an AnnotatedObject
	 * @param typeQualifierValue the kind of TypeQualifierValue we are looking for
	 * @return directly-applied TypeQualifierAnnotation, or null if there is no
	 *         such annotation on the AnnotatedObject
	 */
	private static TypeQualifierAnnotation getDirectTypeQualifierAnnotation(AnnotatedObject o,
			TypeQualifierValue typeQualifierValue) {
		TypeQualifierAnnotation result;
		
		Set<TypeQualifierAnnotation> applications = new HashSet<TypeQualifierAnnotation>();
		getDirectApplications(applications, o, o.getElementType());
		
		result = findMatchingTypeQualifierAnnotation(/*annotations*/applications, typeQualifierValue);
		
		return result;
	}

	/**
	 * Get the effective inherited TypeQualifierAnnotation on given
	 * instance method.
	 * 
	 * @param o                  an XMethod
	 * @param typeQualifierValue the kind of TypeQualifierValue we are looking for
	 * @return effective TypeQualifierAnnotation inherited from overridden supertype methods,
	 *         or null if there is no inherited TypeQualifierAnnotation
	 */
	private static TypeQualifierAnnotation getInheritedTypeQualifierAnnotation(XMethod o, TypeQualifierValue typeQualifierValue) {
		assert !o.isStatic();
		
		ReturnTypeAnnotationAccumulator accumulator = new ReturnTypeAnnotationAccumulator(typeQualifierValue, o);
		try {
			AnalysisContext.currentAnalysisContext().getSubtypes2().traverseSupertypes(o.getClassDescriptor(), accumulator);
			return accumulator.getResult().getEffectiveTypeQualifierAnnotation();
		} catch (ClassNotFoundException e) {
			AnalysisContext.currentAnalysisContext().getLookupFailureCallback().reportMissingClass(e);
			return null;
		}
	}

	/**
	 * Get the default (outer scope) annotation applicable to given
	 * AnnotatedObject.
	 * 
	 * @param o                  an AnnotatedObject
	 * @param typeQualifierValue the kind of TypeQualifierValue we are looking for
	 * @return the applicable default TypeQualifierAnnotation, or null
	 *         if there is no default TypeQualifierAnnotation
	 */
	private static TypeQualifierAnnotation getDefaultTypeQualifierAnnotation(AnnotatedObject o,
			TypeQualifierValue typeQualifierValue) {
		TypeQualifierAnnotation result = null;

		ElementType elementType = o.getElementType();
		while (o.getContainingScope() != null) {
			o = o.getContainingScope();
			
			// Check direct applications of the type qualifier
			Set<TypeQualifierAnnotation> applications = new HashSet<TypeQualifierAnnotation>();
			getDirectApplications(applications, o, elementType);
			result = findMatchingTypeQualifierAnnotation(applications, typeQualifierValue);
			if (result != null) {
				// Great - found an outer scope with a relevant annotation
				break;
			}
			
			// Check FindBugs-specific default annotations
			result = getFindBugsDefaultAnnotation(o, typeQualifierValue, elementType);
			if (result != null) {
				break;
			}
		}
		return result;
	}
	
	/**
	 * Get the effective TypeQualifierAnnotation on given method parameter.
	 * Takes into account inherited and default (outer scope) annotations.
	 * 
	 * @param xmethod            a method
	 * @param parameter          a parameter (0 == first parameter)
	 * @param typeQualifierValue the kind of TypeQualifierValue we are looking for
	 * @return effective TypeQualifierAnnotation on the parameter,
	 *         or null if there is no effective TypeQualifierAnnotation
	 */
	public static @CheckForNull TypeQualifierAnnotation getEffectiveTypeQualifierAnnotation(
			XMethod xmethod,
			int parameter,
			TypeQualifierValue typeQualifierValue) {
		
		DualKeyHashMap<XMethod, Integer, EffectiveTypeQualifierAnnotation> map =
			effectiveParameterAnnotations.get(typeQualifierValue);
		if (map == null) {
			map = new DualKeyHashMap<XMethod, Integer, EffectiveTypeQualifierAnnotation>();
			effectiveParameterAnnotations.put(typeQualifierValue, map);
		}
		
		// Check cached answer
		EffectiveTypeQualifierAnnotation result = map.get(xmethod, parameter);
		if (result == null) {
			// Compute answer
			
			TypeQualifierAnnotation tqa;
			
			// Check direct application
			tqa = getDirectTypeQualifierAnnotation(xmethod, parameter, typeQualifierValue);
			
			// If it's an instance method, check for inherited annotation
			if (tqa == null && !xmethod.isStatic()) {
				tqa = getInheritedTypeQualifierAnnotation(xmethod, parameter, typeQualifierValue);
			}
			
			// Check for default (outer scope) annotation
			if (tqa == null) {
				tqa = getDefaultTypeQualifierAnnotation(xmethod, parameter, typeQualifierValue);
			}
			
			// Cache answer
			result = new EffectiveTypeQualifierAnnotation(tqa);
			map.put(xmethod, parameter, result);
		}
		
		// Return cached answer
		return result.typeQualifierAnnotation;
	}

	/**
	 * Get the TypeQualifierAnnotation directly applied to given
	 * method parameter.
	 * 
	 * @param xmethod            a method
	 * @param parameter          a parameter (0 == first parameter)
	 * @param typeQualifierValue the kind of TypeQualifierValue we are looking for
	 * @return TypeQualifierAnnotation directly applied to the parameter,
	 *         or null if there is no directly applied TypeQualifierAnnotation
	 */
	private static @CheckForNull TypeQualifierAnnotation getDirectTypeQualifierAnnotation(XMethod xmethod, int parameter,
			TypeQualifierValue typeQualifierValue) {
		Set<TypeQualifierAnnotation> applications = new HashSet<TypeQualifierAnnotation>();
		getDirectApplications(applications, xmethod, parameter);
		
		return findMatchingTypeQualifierAnnotation(applications, typeQualifierValue);
	}

	/**
	 * Get the effective inherited TypeQualifierAnnotation on the given
	 * instance method parameter.
	 * 
	 * @param xmethod            an instance method
	 * @param parameter          a parameter (0 == first parameter)
	 * @param typeQualifierValue the kind of TypeQualifierValue we are looking for
	 * @return effective inherited TypeQualifierAnnotation on the parameter,
	 *         or null if there is not effective TypeQualifierAnnotation
	 */
	private static @CheckForNull TypeQualifierAnnotation getInheritedTypeQualifierAnnotation(XMethod xmethod, int parameter,
			TypeQualifierValue typeQualifierValue) {
		assert !xmethod.isStatic();
		
		ParameterAnnotationAccumulator accumulator = new ParameterAnnotationAccumulator(typeQualifierValue, xmethod, parameter);
		try {
			AnalysisContext.currentAnalysisContext().getSubtypes2().traverseSupertypes(xmethod.getClassDescriptor(), accumulator);
			return accumulator.getResult().getEffectiveTypeQualifierAnnotation();
		} catch (ClassNotFoundException e) {
			AnalysisContext.currentAnalysisContext().getLookupFailureCallback().reportMissingClass(e);
			return null;
		}
	}

	/**
	 * Get the default (outer-scope) TypeQualifierAnnotation on given method parameter.
	 * 
	 * @param xmethod            a method
	 * @param parameter          a parameter (0 == first parameter)
	 * @param typeQualifierValue the kind of TypeQualifierValue we are looking for
	 * @return the default (outer scope) TypeQualifierAnnotation on the parameter,
	 *         or null if there is no default TypeQualifierAnnotation
	 */
	private static @CheckForNull TypeQualifierAnnotation getDefaultTypeQualifierAnnotation(
			XMethod xmethod,
			int parameter,
			TypeQualifierValue typeQualifierValue) {
		AnnotatedObject o = xmethod;

		while (o.getContainingScope() != null) {
			o = o.getContainingScope();

			Set<TypeQualifierAnnotation> applications = new HashSet<TypeQualifierAnnotation>();
			getDirectApplications(applications, o, ElementType.PARAMETER);

			TypeQualifierAnnotation tqa = findMatchingTypeQualifierAnnotation(applications, typeQualifierValue);
			if (tqa != null) {
				// Found matching annotation in outer scope
				return tqa;
			}
		}

		return null;
	}
}
