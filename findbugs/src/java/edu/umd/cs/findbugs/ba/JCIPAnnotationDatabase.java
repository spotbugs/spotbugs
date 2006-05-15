/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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

package edu.umd.cs.findbugs.ba;

import java.util.HashMap;
import java.util.Map;

public class JCIPAnnotationDatabase {
	Map<ClassMember, Map<String, Object>> memberAnnotations = new HashMap<ClassMember, Map<String, Object>>();

	Map<String, Map<String, Object>> classAnnotations = new HashMap<String, Map<String, Object>>();


	public Object getClassAnnotation(String dottedClassName, String annotationClass) {
		assert dottedClassName.indexOf('/') == -1;
		return getEntryForClass(dottedClassName).get(annotationClass);
	}
	public boolean hasClassAnnotation(String dottedClassName, String annotationClass) {
		assert dottedClassName.indexOf('/') == -1;
		return getEntryForClass(dottedClassName).containsKey(annotationClass);
	}
	public Object getFieldAnnotation(XField field, String annotationClass) {
		return getEntryForClassMember(field).get(annotationClass);
	}
	public boolean hasFieldAnnotation(XField field, String annotationClass) {
		return getEntryForClassMember(field).containsKey(annotationClass);
	}
	public Object getMethodAnnotation(XMethod method, String annotationClass) {
		return getEntryForClassMember(method).get(annotationClass);
	}
	public boolean hasMethodAnnotation(XMethod method, String annotationClass) {
		return getEntryForClassMember(method).containsKey(annotationClass);
	}
	
	public Map<String, Object> getEntryForClassMember(ClassMember member) {
		Map<String, Object> map = memberAnnotations.get(member);
		if (map == null) {
			map = new HashMap<String, Object>();
			memberAnnotations.put(member, map);
		}
		return map;
	}

	public 
	 Map<String, Object> getEntryForClass(String dottedClassName) {
		assert dottedClassName.indexOf('/') == -1;
		Map<String, Object> map = classAnnotations.get(dottedClassName);
		if (map == null) {
			map = new HashMap<String, Object>();
			classAnnotations.put(dottedClassName, map);
		}
		return map;
	}


}
