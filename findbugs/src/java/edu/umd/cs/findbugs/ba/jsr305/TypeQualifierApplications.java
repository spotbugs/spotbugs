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

import edu.umd.cs.findbugs.classfile.analysis.AnnotatedObject;
import edu.umd.cs.findbugs.classfile.analysis.AnnotationValue;
import edu.umd.cs.findbugs.classfile.analysis.EnumValue;
import edu.umd.cs.findbugs.classfile.analysis.MethodInfo;
import edu.umd.cs.findbugs.util.DualKeyHashMap;

/**
 * @author William Pugh
 */
public class TypeQualifierApplications {
	
	static Map<AnnotatedObject, Collection<AnnotationValue>> objectAnnotations = new HashMap<AnnotatedObject, Collection<AnnotationValue>>();
	static DualKeyHashMap<MethodInfo, Integer, Collection<AnnotationValue>> parameterAnnotations 
	= new DualKeyHashMap<MethodInfo, Integer, Collection<AnnotationValue>>();
	
	public static Collection<AnnotationValue> getAnnotation(AnnotatedObject m) {
		Collection<AnnotationValue> result = objectAnnotations.get(m);
		if (result != null) return result;
		if (m.getAnnotationDescriptors().isEmpty()) return Collections.emptyList();
		result = TypeQualifierResolver.resolveTypeQualifierNicknames(m.getAnnotations());
		if (result.size() == 0) result = Collections.emptyList();
		objectAnnotations.put(m, result);
		return result;
	}
	public static Collection<AnnotationValue> getAnnotation(MethodInfo m, int parameter) {
		Collection<AnnotationValue> result = parameterAnnotations.get(m, parameter);
		if (result != null) return result;
		if (m.getParameterAnnotationDescriptors(parameter).isEmpty()) return Collections.emptyList();
		result = TypeQualifierResolver.resolveTypeQualifierNicknames(m.getParameterAnnotations(parameter));
		if (result.size() == 0) result = Collections.emptyList();
		parameterAnnotations.put(m, parameter, result);
		return result;
	}
	
	public static void getApplicableApplications(Map<TypeQualifierValue, When> result, MethodInfo o, int parameter) {
		 Collection<AnnotationValue> values = getAnnotation(o, parameter);
		 ElementType e = ElementType.PARAMETER;
		 for(AnnotationValue v : values) {
			 Object a = v.getValue("applyTo");
			 if (a instanceof Object[]) {
				 for(Object o2 : (Object[]) a) 
					 if (o2 == e) 
						 constructTypeQualifierAnnotation(result, v);
			 } else if (o.getElementType().equals(e))
				 constructTypeQualifierAnnotation(result, v);
		 }
	}
	public static void getApplicableApplications(Map<TypeQualifierValue, When> result, AnnotatedObject o, ElementType e) {
		 Collection<AnnotationValue> values = getAnnotation(o);
		 for(AnnotationValue v : values) {
			 Object a = v.getValue("applyTo");
			 if (a instanceof Object[]) {
				 for(Object o2 : (Object[]) a) 
					 if (o2 == e) 
						 constructTypeQualifierAnnotation(result, v);
			 } else if (o.getElementType().equals(e))
				 constructTypeQualifierAnnotation(result, v);
		 }
	}
	/**
     * @param v
     * @return
     */
    private static void constructTypeQualifierAnnotation( Map<TypeQualifierValue, When> map, AnnotationValue v) {
	    EnumValue value = (EnumValue) v.getValue("when");
	    map.put(TypeQualifierValue.getValue(v.getAnnotationClass(), v.getValue("value")), When.valueOf(value.value));
    }

	public static void getApplicableScopedApplications(Map<TypeQualifierValue, When> result, AnnotatedObject o, ElementType e) {
		AnnotatedObject outer = o.getContainingScope();
		if (outer != null) 
			getApplicableScopedApplications(result, outer, e);
		getApplicableApplications(result, o, e);
	}
	public static Collection<TypeQualifierAnnotation> getApplicableScopedApplications(AnnotatedObject o, ElementType e) {
		Map<TypeQualifierValue, When> result = new HashMap<TypeQualifierValue, When>();
		getApplicableScopedApplications(result, o, e);
		return  TypeQualifierAnnotation.getValues(result);
	}
		
	public static void getApplicableScopedApplications(Map<TypeQualifierValue, When> result, MethodInfo o, int parameter) {
		 ElementType e = ElementType.PARAMETER;
		getApplicableScopedApplications(result, o, e);
		getApplicableApplications(result, o, parameter);
	}
	public static Collection<TypeQualifierAnnotation> getApplicableScopedApplications(MethodInfo o, int parameter) {
		Map<TypeQualifierValue, When> result = new HashMap<TypeQualifierValue, When>();
		 ElementType e = ElementType.PARAMETER;
		getApplicableScopedApplications(result, o, e);
		getApplicableApplications(result, o, parameter);
		return TypeQualifierAnnotation.getValues(result);
	}
		
}
