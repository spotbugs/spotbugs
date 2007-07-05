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

package edu.umd.cs.findbugs.detect;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.ba.AnnotationDatabase;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.XMethodParameter;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifier;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierDatabase;
import edu.umd.cs.findbugs.ba.jsr305.When;
import edu.umd.cs.findbugs.bcel.AnnotationDetector;
import edu.umd.cs.findbugs.bcel.BCELUtil;
import edu.umd.cs.findbugs.classfile.Global;

/**
 * Note JSR-305 type qualifier annotations in referenced
 * and application classes.
 * Builds AnnotationDatabases recording how each kind
 * of type qualifier was used on observed classes/fields/methods/parameters/etc.
 * 
 * @author David Hovemeyer
 */
public class NoteTypeQualifiers extends AnnotationDetector implements NonReportingDetector {
	private BugReporter bugReporter;
	private Set<String> typeQualifierSet;
	private Set<String> knownAnnotationSet;
	private TypeQualifierDatabase typeQualifierDatabase;
	private Map<String, Map<String,Object>> classAnnotationMap; // class annotations for the most-recently visited class
	private Map<String, Map<String, Map<String,Object>>> typeQualifierNicknameMap; // map of type qualifier nickname classes to their annotations
	
	public NoteTypeQualifiers(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		this.typeQualifierSet = new HashSet<String>();
		this.knownAnnotationSet = new HashSet<String>();
		this.classAnnotationMap = new HashMap<String, Map<String,Object>>();
		this.typeQualifierNicknameMap = new HashMap<String, Map<String,Map<String,Object>>>();
	}
	
	private boolean isTypeQualifer(String annotationClass) {
		if (!knownAnnotationSet.contains(annotationClass)) {
			try {
				// See if annotation class is a descendent of javax.annotation.meta.Qualifier
				if (Hierarchy.isSubtype(annotationClass, "javax.annotation.meta.Qualifier")) {
					typeQualifierSet.add(annotationClass);
				}
			} catch (ClassNotFoundException e) {
				bugReporter.reportMissingClass(e);
			}
			knownAnnotationSet.add(annotationClass);
		}

		return knownAnnotationSet.contains(annotationClass);
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.bcel.AnnotationDetector#visitClassContext(edu.umd.cs.findbugs.ba.ClassContext)
	 */
	@Override
	public void visitClassContext(ClassContext classContext) {
		if (typeQualifierDatabase == null) {
			// Create the TypeQualifierDatabase and register it
			// with the analysis cache.
			typeQualifierDatabase = new TypeQualifierDatabase();
			Global.getAnalysisCache().eagerlyPutDatabase(TypeQualifierDatabase.class, typeQualifierDatabase);
		}

		// No need to scan classes compiled for any Java version before 1.5 (Tiger)
		if (BCELUtil.preTiger(classContext.getJavaClass())) {
			return;
		}
		
		super.visitClassContext(classContext);
	}

	private boolean isAnnotation(JavaClass obj) {
		boolean isAnnotation = false;
		if (obj.isInterface()) {
			String[] interfaceNames = obj.getInterfaceNames();
			for (String name : interfaceNames) {
				if (name.equals("java.lang.annotation.Annotation")) {
					isAnnotation = true;
					break;
				}
			}
		}
		return isAnnotation;
    }
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.visitclass.BetterVisitor#visit(org.apache.bcel.classfile.JavaClass)
	 */
	@Override
	public void visit(JavaClass obj) {
		classAnnotationMap.clear();
		
		super.visit(obj);

		if (isAnnotation(obj) && classAnnotationMap.keySet().contains("javax.annotation.meta.QualifierNickname")) {
			// This type qualifier annotation is a nickname for another
			// (more fully specified) type qualifier annotation.
			// Make a note so we can register it with the TypeQualifierDatabase
			// once we have seen all annotations and can make complete sense of them.
			Map<String, Map<String,Object>> classAnnotationMapCopy = new HashMap<String, Map<String,Object>>();
			classAnnotationMapCopy.putAll(classAnnotationMap);
			typeQualifierNicknameMap.put(obj.getClassName(), classAnnotationMapCopy);
		}
	}

	private TypeQualifier getTypeQualifier(String annotationClass, Map<String, Object> map) {
	    When when = typeQualifierDatabase.getWhen(annotationClass, map);
	    TypeQualifier tq = typeQualifierDatabase.getTypeQualifier(annotationClass, when);
	    return tq;
    }
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.visitclass.AnnotationVisitor#visitAnnotation(java.lang.String, java.util.Map, boolean)
	 */
	@Override
	public void visitAnnotation(String annotationClass, Map<String, Object> map, boolean runtimeVisible) {
		if (!(visitingField() || visitingMethod())) {
			// Keep track of class-level annotations
			classAnnotationMap.put(annotationClass, map);
		}
		
		if (!isTypeQualifer(annotationClass)) {
			return;
		}

		// FIXME: is this code doing the Right Thing here?
		// Probably need to add default annotations somewhere.
		
		AnnotationDatabase<TypeQualifier> db = typeQualifierDatabase.getAnnotationDatabase(annotationClass);
		
		if (visitingMethod()) {
			TypeQualifier tq = getTypeQualifier(annotationClass, map);


			XMethod method = XFactory.createXMethod(this);
			db.addDirectAnnotation(method, tq);
		} else if (visitingField()) {
			TypeQualifier tq = getTypeQualifier(annotationClass, map);

			XField field = XFactory.createXField(this);
			db.addDirectAnnotation(field, tq);
		} else {
			// Assume annotation applies to entire class
			TypeQualifier tq = getTypeQualifier(annotationClass, map);
			
			String className = getDottedClassName();
			db.addDirectAnnotation(className, tq);
		}
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.visitclass.AnnotationVisitor#visitSyntheticParameterAnnotation(int, boolean)
	 */
	@Override
	public void visitSyntheticParameterAnnotation(int p, boolean runtimeVisible) {
		// FIXME: what are we supposed to do here?
	}
	
	/* (non-Javadoc)
	 * @see edu.umd.cs.findbugs.visitclass.AnnotationVisitor#visitParameterAnnotation(int, java.lang.String, java.util.Map, boolean)
	 */
	@Override
	public void visitParameterAnnotation(int p, String annotationClass, Map<String, Object> map, boolean runtimeVisible) {
		if (!isTypeQualifer(annotationClass)) {
			return;
		}

		AnnotationDatabase<TypeQualifier> db = typeQualifierDatabase.getAnnotationDatabase(annotationClass);

		TypeQualifier tq = getTypeQualifier(annotationClass, map);
		XMethod method = XFactory.createXMethod(this);
		XMethodParameter param = new XMethodParameter(method, p);
		
		db.addDirectAnnotation(param, tq);
	}
}
