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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * @author William Pugh
 */
public class AnnotationDatabase<Annotation extends AnnotationEnumeration> {
	static final boolean DEBUG = false;

	/**
	 * 
	 */
	public static final String FIELD = "Field";

	/**
	 * 
	 */
	public static final String METHOD = "Method";

	/**
	 * 
	 */
	public static final String PARAMETER = "Parameter";

	/**
	 * 
	 */
	public static final String ANY = "Any";

	private static final String DEFAULT_ANNOTATION_ANNOTATION_CLASS = "DefaultAnnotation";

	private Map<Object, Annotation> directAnnotations = new HashMap<Object, Annotation>();

	private final Map<String, Map<JavaClass, Annotation>> defaultAnnotation = new HashMap<String, Map<JavaClass, Annotation>>();

	public AnnotationDatabase() {
		defaultAnnotation.put(ANY,
				new HashMap<JavaClass, Annotation>());
		defaultAnnotation.put(PARAMETER,
				new HashMap<JavaClass, Annotation>());
		defaultAnnotation.put(METHOD,
				new HashMap<JavaClass, Annotation>());
		defaultAnnotation.put(FIELD,
				new HashMap<JavaClass, Annotation>());

	}

	private final Set<Annotation> seen = new HashSet<Annotation>();
	public void addDirectAnnotation(@NonNull Object o, @NonNull Annotation n) {
		directAnnotations.put(o, n);
		seen.add(n);
	}

	public void addDefaultAnnotation(String target, JavaClass c,
			Annotation n) {
		if (!defaultAnnotation.containsKey(target))
			return;
		if (DEBUG)
			System.out.println("Default annotation " + target + " " + c.getClassName() + " " + n);
		defaultAnnotation.get(target).put(c, n);
		seen.add(n);
	}

	public boolean anyAnnotations(Annotation n) {
		return seen.contains(n);
	}

	@CheckForNull
	public Annotation getResolvedAnnotation(final Object o, boolean getMinimal) {
		Annotation n = directAnnotations.get(o);
		if (n != null)
			return n;

		try {
			JavaClass c;
			String kind;
			if (o instanceof XMethod || o instanceof XMethodParameter) {
				
				XMethod m;
				if (o instanceof XMethod) {
					m = (XMethod) o;
					kind = METHOD;
				} else if (o instanceof XMethodParameter) {
					m = ((XMethodParameter) o).getMethod();
					kind = PARAMETER;
				} else
					throw new IllegalStateException("impossible");
				if (m.getClassName().startsWith("java")) {
					// TODO: none of these are annotated
					return null;
				}
				c = Repository.lookupClass(m.getClassName());
				if (!m.isStatic()) {
					TreeSet<Annotation> inheritedAnnotations = new TreeSet<Annotation>();
					if (c.getSuperclassNameIndex() > 0) {
						
						n = lookInOverriddenMethod(o, c.getSuperclassName(), m, getMinimal);
						if (n != null)
							inheritedAnnotations.add(n);
					}
					for(String implementedInterface : c.getInterfaceNames()) {
						n = lookInOverriddenMethod(o, implementedInterface, m, getMinimal);
						if (n != null)
							inheritedAnnotations.add(n);
					}
					if (DEBUG) System.out.println("# of inherited annotations : " + inheritedAnnotations.size());
					if (!inheritedAnnotations.isEmpty()) {
						if (inheritedAnnotations.size() == 1) 
							return inheritedAnnotations.first();
						if (!getMinimal) 
							return inheritedAnnotations.last();
						
						Annotation min = inheritedAnnotations.first();
						if (min.getIndex() == 0) {
							inheritedAnnotations.remove(min);
							min = inheritedAnnotations.first();
						}
						return min;
					}
					
				} // if not static
				} // associated with method
			 else if (o instanceof XField) {
				
				String className = ((XField) o).getClassName();
				if (className.startsWith("java")) {
					// TODO: None of these are annotated
					return null;
				}
				c = Repository.lookupClass(className);
				kind = FIELD;
			} else
				throw new IllegalArgumentException(
						"Can't lookup annotation for " + o.getClass().getName());

			// look for default annotation

			n = defaultAnnotation.get(kind).get(c);
			if (DEBUG) 
				System.out.println("Default annotation for " + kind + " is " + n);
			if (n != null)
				return n;

			n = defaultAnnotation.get(ANY).get(c);
			if (DEBUG) 
				System.out.println("Default annotation for any is " + n);
			if (n != null)
				return n;

			String cName = c.getClassName();
			int p = cName.lastIndexOf('.');
			cName = cName.substring(0,p+1) + "package-info";
			c = Repository.lookupClass(cName);
			n = defaultAnnotation.get(kind).get(c);
			if (DEBUG) 
				System.out.println("Default annotation for " + kind + " is " + n);
			if (n != null)
				return n;

			n = defaultAnnotation.get(ANY).get(c);
			if (DEBUG) 
				System.out.println("Default annotation for any is " + n);
			if (n != null)
				return n;
			
			
			
			return n;
		} catch (ClassNotFoundException e) {
			// ignore it
			return null;
		}

	}

	private Annotation lookInOverriddenMethod(final Object originalQuery, 
			String classToLookIn, XMethod originalMethod, boolean getMinimal) {
		try {
		Annotation n;
		// Look in supermethod
		XMethod superMethod = XFactory.createXMethod(classToLookIn, originalMethod.getName(),
				originalMethod.getSignature(), originalMethod.isStatic());
		if (DEBUG)
			System.out.println("Looking for overridden method " + superMethod);
		
		Object probe;
		if (originalQuery instanceof XMethod)
			probe = superMethod;
		else if (originalQuery instanceof XMethodParameter)
			probe = new XMethodParameter(superMethod,
					((XMethodParameter) originalQuery).getParameterNumber());
		else
			throw new IllegalStateException("impossible");

		n = getResolvedAnnotation(probe, getMinimal);
		return n;
		} catch (RuntimeException e) {
			e.printStackTrace();
			throw e;
		}
	}
}
