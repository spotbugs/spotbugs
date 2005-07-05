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

package edu.umd.cs.findbugs.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.BugAnnotation;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.ClassAnnotation;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.ba.SignatureParser;

/**
 * Build a map of added class names to removed class names.
 * 
 * @author David Hovemeyer
 */
public class MovedClassMap {
	
	private static final boolean DEBUG = true;//Boolean.getBoolean("movedClasses.debug");
	
	private BugCollection before;
	private BugCollection after;
	private Map<String,String> rewriteMap;
	
	public MovedClassMap(BugCollection before, BugCollection after) {
		 this.before = before;
		 this.after = after;
		 this.rewriteMap = new HashMap<String,String>();
	}
	
	public MovedClassMap execute() {
		Set<String> beforeClasses = buildClassSet(before);
		Set<String> afterClasses = buildClassSet(after);
		
		Set<String> removedClasses = new HashSet<String>(beforeClasses);
		removedClasses.removeAll(afterClasses);
		
		Set<String> addedClasses = new HashSet<String>(afterClasses);
		addedClasses.removeAll(beforeClasses);
		
		Map<String,String> removedShortNameToFullNameMap = buildShortNameToFullNameMap(removedClasses);
		
		// Map names of added classes to names of removed classes if
		// they have the same short name.
		for (String fullAddedName : addedClasses) {
			
			// FIXME: could use a similarity metric to match added and removed
			// classes.  Instead, we just match based on the short class name.
			
			String shortAddedName = getShortClassName(fullAddedName);
			String fullRemovedName = removedShortNameToFullNameMap.get(shortAddedName);
			if (fullRemovedName != null) {
				if (DEBUG) System.err.println(fullAddedName + " --> " + fullRemovedName);
				rewriteMap.put(fullAddedName, fullRemovedName);
			}
			
		}
		
		return this;
	}
	
	public String rewriteClassName(String className) {
		String rewrittenClassName = rewriteMap.get(className);
		if (rewrittenClassName != null) {
			className = rewrittenClassName;
		}
		return className;
	}
	
	public String rewriteMethodSignature(String methodSignature) {
		SignatureParser parser = new SignatureParser(methodSignature);
		
		StringBuffer buf = new StringBuffer();
		
		buf.append('(');
		for (Iterator<String> i = parser.parameterSignatureIterator(); i.hasNext();) {
			buf.append(rewriteSignature(i.next()));
		}
		
		buf.append(')');
		buf.append(rewriteSignature(parser.getReturnTypeSignature()));
		
		return buf.toString();
	}
	
	/**
	 * Rewrite a signature.
	 * 
	 * @param signature a signature (parameter, return type, or field)
	 * @return rewritten signature with class name updated if required
	 */
	public String rewriteSignature(String signature) {
		if (!signature.startsWith("L"))
			return signature;
		
		String className = signature.substring(1, signature.length() - 1).replace('/', '.');
		className = rewriteClassName(className);
		
		return "L" + className.replace('.', '/') + ";";
	}

	/**
	 * Rewrite a MethodAnnotation to update the class name,
	 * and any class names mentioned in the method signature.
	 * 
	 * @param annotation a MethodAnnotation
	 * @return the possibly-updated MethodAnnotation
	 */
	public MethodAnnotation convertMethodAnnotation(MethodAnnotation annotation) {
		annotation = new MethodAnnotation(
				rewriteClassName(annotation.getClassName()),
				annotation.getMethodName(),
				rewriteMethodSignature(annotation.getMethodSignature()),
				annotation.isStatic());
		return annotation;
	}

	/**
	 * Rewrite a FieldAnnotation to update the class name
	 * and field signature, if needed.
	 * 
	 * @param annotation a FieldAnnotation
	 * @return the possibly-updated FieldAnnotation
	 */
	public FieldAnnotation convertFieldAnnotation(FieldAnnotation annotation) {
		annotation = new FieldAnnotation(
				rewriteClassName(annotation.getClassName()),
				annotation.getFieldName(),
				rewriteSignature(annotation.getFieldSignature()),
				annotation.isStatic());
		return annotation;
	}

	/**
	 * Find set of classes referenced in given BugCollection.
	 * 
	 * @param bugCollection
	 * @return set of classes referenced in the BugCollection
	 */
	private Set<String> buildClassSet(BugCollection bugCollection) {
		Set<String> classSet = new HashSet<String>();
		
		for (Iterator<BugInstance> i = bugCollection.iterator(); i.hasNext(); ) {
			BugInstance warning = i.next();
			for (Iterator<BugAnnotation> j = warning.annotationIterator(); j.hasNext();) {
				BugAnnotation annotation = j.next();
				if (!(annotation instanceof ClassAnnotation))
					continue;
				classSet.add(((ClassAnnotation)annotation).getClassName());
			}
		}
		
		return classSet;
	}

	/**
	 * Build a map of short class names (without package) to full
	 * class names.
	 * 
	 * @param classSet set of fully-qualified class names
	 * @return map of short class names to fully-qualified class names
	 */
	private Map<String, String> buildShortNameToFullNameMap(Set<String> classSet) {
		Map<String,String> result = new HashMap<String,String>();
		for (String className : classSet) {
			String shortClassName = getShortClassName(className);
			result.put(shortClassName, className);
		}
		return result;
	}

	/**
	 * Get a short class name (no package part).
	 * 
	 * @param className a class name
	 * @return short class name
	 */
	private String getShortClassName(String className) {
		int lastDot = className.lastIndexOf('.');
		if (lastDot >= 0) {
			className = className.substring(lastDot + 1);
		}
		return className;
	}
	
	
}
