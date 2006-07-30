/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004-2006 University of Maryland
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
import java.util.Map;

import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.ClassMember;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.visitclass.AnnotationVisitor;

public class NoteJCIPAnnotation extends AnnotationVisitor implements
		Detector, NonReportingDetector {

	private static final String NET_JCIP_ANNOTATIONS = "net.jcip.annotations.";
	BugReporter bugReporter;

	public NoteJCIPAnnotation(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	
	public void visitAnnotation(String annotationClass,
			Map<String, Object> map, boolean runtimeVisible) {

		if (!annotationClass.startsWith(NET_JCIP_ANNOTATIONS))
			return;
		annotationClass = annotationClass.substring(NET_JCIP_ANNOTATIONS
				.length());
		Object value = map.get("value");
		ClassMember member;
		if (visitingField())
			member = XFactory.createXField(this);
		else if (visitingMethod())
			member = XFactory.createXMethod(this);
		else {
			Map<String, Object> annotationsOfThisClass = AnalysisContext.currentAnalysisContext()
			.getJCIPAnnotationDatabase().getEntryForClass(getDottedClassName());
			annotationsOfThisClass.put(annotationClass, value);
			return;
		}
		Map<String, Object> annotationsOfThisMember = AnalysisContext.currentAnalysisContext()
		.getJCIPAnnotationDatabase().getEntryForClassMember(member);
		annotationsOfThisMember.put(annotationClass, value);
	}

	public void visitClassContext(ClassContext classContext) {
		JavaClass javaClass = classContext.getJavaClass();
		if  (!FindUnreleasedLock.preTiger(javaClass)) javaClass.accept(this);

	}

	public void report() {

	}

}
