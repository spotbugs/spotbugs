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

package edu.umd.cs.findbugs.detect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.jsr305.Analysis;
import edu.umd.cs.findbugs.ba.jsr305.DirectlyRelevantTypeQualifiersDatabase;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierAnnotation;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierApplications;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierValue;
import edu.umd.cs.findbugs.bcel.BCELUtil;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.visitclass.DismantleBytecode;

/**
 * Scan classes for type qualifier annotations
 * and convey them to interested detectors (FindNullDeref, CheckTypeQualifiers, ...)
 */
public class NoteDirectlyRelevantTypeQualifiers //extends DirectlyRelevantTypeQualifiersDatabase
	extends DismantleBytecode
	implements Detector, NonReportingDetector {
	
	private BugReporter bugReporter;
	private DirectlyRelevantTypeQualifiersDatabase qualifiers;

	public NoteDirectlyRelevantTypeQualifiers(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void visitClassContext(ClassContext classContext) {
		if (qualifiers == null) {
			qualifiers = AnalysisContext.currentAnalysisContext().getDirectlyRelevantTypeQualifiersDatabase();
		}
		
		JavaClass javaClass = classContext.getJavaClass();
		if  (!BCELUtil.preTiger(javaClass)) javaClass.accept(this);
	}

	
	HashSet<TypeQualifierValue> applicableApplications;
	@Override
    public void visit(Code m) {
		applicableApplications = new HashSet<TypeQualifierValue>();
		XMethod xMethod = getXMethod();
		
		// Find the direct annotations on this method
		updateApplicableAnnotations(xMethod);
		
		// Find direct annotations on called methods and loaded fields
		super.visit(m);
		
		if (applicableApplications.size() > 0) {
			qualifiers.setDirectlyRelevantTypeQualifiers(getMethodDescriptor(), new ArrayList<TypeQualifierValue>(applicableApplications));
		}
		
		
	}
	@Override
	public void sawOpcode(int seen) {
		switch(seen) {
		case INVOKEINTERFACE:
		case INVOKEVIRTUAL:
		case INVOKESTATIC:
		case INVOKESPECIAL:
		{
			XMethod m = XFactory.createReferencedXMethod(this);
			updateApplicableAnnotations(m);
	
			break;
		}
		case GETSTATIC:
		case PUTSTATIC:
		case GETFIELD:
		case PUTFIELD:
			{
				XField f = XFactory.createReferencedXField(this);

				Collection<TypeQualifierAnnotation> annotations = TypeQualifierApplications.getApplicableApplications(f);
				Analysis.addKnownTypeQualifiers(applicableApplications, annotations);
				
				
			break;
			}
			
		}
	}

	/**
     * @param m
     */
    private void updateApplicableAnnotations(XMethod m) {
	    Collection<TypeQualifierAnnotation> annotations = TypeQualifierApplications.getApplicableApplications(m);
	    Analysis.addKnownTypeQualifiers(applicableApplications, annotations);
	    Analysis.addKnownTypeQualifiersForParameters(applicableApplications, m);
    }

    public void report() {
	}
}
