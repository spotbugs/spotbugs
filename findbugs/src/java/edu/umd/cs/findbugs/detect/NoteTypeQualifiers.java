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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.ba.AnnotationDatabase;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifier;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierDatabase;
import edu.umd.cs.findbugs.ba.jsr305.When;
import edu.umd.cs.findbugs.bcel.AnnotationDetector;
import edu.umd.cs.findbugs.bcel.BCELUtil;
import edu.umd.cs.findbugs.classfile.Global;

/**
 * Note JSR-305 type qualifier annotations in referenced
 * and application classes.
 * 
 * @author David Hovemeyer
 */
public class NoteTypeQualifiers extends AnnotationDetector implements NonReportingDetector {
	private BugReporter bugReporter;
	private Set<String> typeQualifierSet;
	private Set<String> knownAnnotationSet;
	private TypeQualifierDatabase typeQualifierDatabase;
	
	public NoteTypeQualifiers(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		this.typeQualifierSet = new HashSet<String>();
		this.knownAnnotationSet = new HashSet<String>();
	}
	
	private boolean isTypeQualifer(String annotationClass) {
		if (!knownAnnotationSet.contains(annotationClass)) {
			try {
				// See if annotation class is a descendent of javax.annotation.meta.Qualifier
				if (Hierarchy.isSubtype(annotationClass, "javax.annotation.meta.Qualifier")) {
					typeQualifierSet.add(annotationClass);
				} else {
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
		if (!isTypeQualifer(annotationClass)) {
			return;
		}
		
		if (visitingMethod()) {
			TypeQualifier tq = getTypeQualifier(annotationClass, map);

			AnnotationDatabase<TypeQualifier> db = typeQualifierDatabase.getAnnotationDatabase(annotationClass);

			// TODO: register the annotation in the database
		} else if (visitingField()) {
			
		}
	}
}
