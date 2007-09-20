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

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.NonReportingDetector;
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

/**
 * Scan classes for @NonNull, @PossiblyNull and @CheckForNull annotations,
 * and convey them to FindNullDeref.
 * 
 */
public class NoteDirectlyRelevantTypeQualifiers extends DirectlyRelevantTypeQualifiersDatabase
	implements Detector, NonReportingDetector {

	public NoteDirectlyRelevantTypeQualifiers(BugReporter bugReporter) {
	
	}

	public void visitClassContext(ClassContext classContext) {

		JavaClass javaClass = classContext.getJavaClass();
		if  (!BCELUtil.preTiger(javaClass)) javaClass.accept(this);
	}

	
	HashSet<TypeQualifierValue> applicableApplications;
	@Override
    public void visitMethod(Method m) {
		applicableApplications = new HashSet<TypeQualifierValue>();
		super.visitMethod(m);
		if (applicableApplications.size() > 0) {
			qualifiers.put(getMethodDescriptor(), new ArrayList<TypeQualifierValue>(applicableApplications));
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
			Collection<TypeQualifierAnnotation> annotations = TypeQualifierApplications.getApplicableApplications(m);
			Analysis.addKnownTypeQualifiers(applicableApplications, annotations);
			Analysis.addKnownTypeQualifiersForParameters(applicableApplications, m);
	
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
	public void report() {
	}
}
