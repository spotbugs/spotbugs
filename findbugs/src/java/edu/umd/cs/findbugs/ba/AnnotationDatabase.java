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
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.util.MapCache;

/**
 * @author William Pugh
 */
public class AnnotationDatabase<AnnotationEnum extends AnnotationEnumeration> {
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

	private Map<Object, AnnotationEnum> directAnnotations = new HashMap<Object, AnnotationEnum>();
	
	private Set<Object> syntheticElements = new HashSet<Object>();

	private final Map<String, Map<String, AnnotationEnum>> defaultAnnotation = new HashMap<String, Map<String, AnnotationEnum>>();

	public AnnotationDatabase() {
		defaultAnnotation.put(ANY,
				new HashMap<String, AnnotationEnum>());
		defaultAnnotation.put(PARAMETER,
				new HashMap<String, AnnotationEnum>());
		defaultAnnotation.put(METHOD,
				new HashMap<String, AnnotationEnum>());
		defaultAnnotation.put(FIELD,
				new HashMap<String, AnnotationEnum>());

	}

	private final Set<AnnotationEnum> seen = new HashSet<AnnotationEnum>();
	public void addSyntheticElement(Object o) {
		syntheticElements.add(o);
		if (DEBUG)
			System.out.println("Synthetic element: " + o);
	}
	
	public void addDirectAnnotation(Object o, AnnotationEnum n) {
		directAnnotations.put(o, n);
		seen.add(n);
	}

	public void addDefaultAnnotation(String target, String c,
			AnnotationEnum n) {
		if (!defaultAnnotation.containsKey(target))
			return;
		if (DEBUG)
			System.out.println("Default annotation " + target + " " + c + " " + n);
		defaultAnnotation.get(target).put(c, n);
		seen.add(n);
	}

	public boolean anyAnnotations(AnnotationEnum n) {
		return seen.contains(n);
	}
	
	// TODO: Parameterize these values?
	Map<Object, AnnotationEnum> cachedMinimal = new MapCache<Object, AnnotationEnum>(20000);
	Map<Object, AnnotationEnum> cachedMaximal= new MapCache<Object, AnnotationEnum>(20000);
	@CheckForNull
	public AnnotationEnum getResolvedAnnotation(Object o, boolean getMinimal) {
		Map<Object, AnnotationEnum> cache;
		if (getMinimal) cache = cachedMinimal;
		else cache = cachedMaximal;
		
		if (cache.containsKey(o)) {
			return cache.get(o);
		}
		AnnotationEnum n = getUncachedResolvedAnnotation(o, getMinimal);
		if (DEBUG) System.out.println("TTT: " + o + " " + n);
		cache.put(o,n);
		return n;
	}
	
	@CheckForNull
	public AnnotationEnum getUncachedResolvedAnnotation(final Object o, boolean getMinimal) {
		
		AnnotationEnum n = directAnnotations.get(o);
		if (n != null)
			return n;

		try {
			
			String className;
			String kind;
			boolean isParameterToInitMethodofAnonymousInnerClass = false;
			boolean isSyntheticMethod = false;
			if (o instanceof XMethod || o instanceof XMethodParameter) {
				
				XMethod m;
				if (o instanceof XMethod) {
					m = (XMethod) o;
					isSyntheticMethod = syntheticElements.contains(m);
					kind = METHOD;
					className = m.getClassName();
				} else if (o instanceof XMethodParameter) {
					m = ((XMethodParameter) o).getMethod();
					// Don't 
					isSyntheticMethod = syntheticElements.contains(m);
					className = m.getClassName();
					kind = PARAMETER;
					if (m.getName().equals("<init>")) {
						int i = className.lastIndexOf("$");
						if (i+1 < className.length()
								&& Character.isDigit(className.charAt(i+1)))
								isParameterToInitMethodofAnonymousInnerClass = true;
					}
				} else
					throw new IllegalStateException("impossible");

				

				if (!m.isStatic() && !m.getName().equals("<init>")) {
					JavaClass c = Repository.lookupClass(className);
					// get inherited annotation
					TreeSet<AnnotationEnum> inheritedAnnotations = new TreeSet<AnnotationEnum>();
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
						
						AnnotationEnum min = inheritedAnnotations.first();
						if (min.getIndex() == 0) {
							inheritedAnnotations.remove(min);
							min = inheritedAnnotations.first();
						}
						return min;
					}
					// check to see if method is defined in this class;
					// if not, on't consider default annotations
					if (! classDefinesMethod(c, m) ) return null;
					if (DEBUG) System.out.println("looking for default annotations: " + c.getClassName() + " defines " + m);
				} // if not static
				} // associated with method
			 else if (o instanceof XField) {
				
				className = ((XField) o).getClassName();
				kind = FIELD;
			} else if (o instanceof String) {
				className = (String) o;
				kind = "CLASS";
			} else throw new IllegalArgumentException("Can't look up annotation for " + o.getClass().getName());

			// <init> method parameters for inner classes don't inherit default annotations
			// since some of them are synthetic
			if (isParameterToInitMethodofAnonymousInnerClass) return null;
			if (isSyntheticMethod) return null;
			
			// synthetic elements should not inherit default annotations
			if (syntheticElements.contains(o)) return null;
			if (syntheticElements.contains(className)) return null;
			
			
			// look for default annotation
			n = defaultAnnotation.get(kind).get(className);
			if (DEBUG) 
				System.out.println("Default annotation for " + kind + " is " + n);
			if (n != null)
				return n;

			n = defaultAnnotation.get(ANY).get(className);
			if (DEBUG) 
				System.out.println("Default annotation for any is " + n);
			if (n != null)
				return n;


			int p = className.lastIndexOf('.');
			className = className.substring(0,p+1) + "package-info";
			n = defaultAnnotation.get(kind).get(className);
			if (DEBUG) 
				System.out.println("Default annotation for " + kind + " is " + n);
			if (n != null)
				return n;

			n = defaultAnnotation.get(ANY).get(className);
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

	private boolean classDefinesMethod(JavaClass c, XMethod m) {
		for(Method definedMethod : c.getMethods()) 
			if (definedMethod.getName().equals(m.getName())
					&& definedMethod.getSignature().equals(m.getSignature())
					&& definedMethod.isStatic() == m.isStatic())
				return true;
		return false;
	}

	private AnnotationEnum lookInOverriddenMethod(final Object originalQuery, 
			String classToLookIn, XMethod originalMethod, boolean getMinimal) {
		try {
		AnnotationEnum n;
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

	protected void addDefaultMethodAnnotation(String cName, AnnotationEnum annotation) {
	

		addDefaultAnnotation(AnnotationDatabase.METHOD, cName, annotation);

	
	}

	protected void addMethodAnnotation(String cName, String mName, String mSig, boolean isStatic, AnnotationEnum annotation) {
		XMethod m = XFactory.createXMethod(cName, mName, mSig, isStatic);
		addDirectAnnotation(m, annotation);
	}
	protected void addMethodParameterAnnotation(String cName, String mName, String mSig, boolean isStatic, int param, AnnotationEnum annotation) {
		XMethod m = XFactory.createXMethod(cName, mName, mSig, isStatic);
		addDirectAnnotation(new XMethodParameter(m, param), annotation);
	}
}
