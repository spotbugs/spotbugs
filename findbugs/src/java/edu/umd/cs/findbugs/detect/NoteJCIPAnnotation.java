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

import java.util.Map;

import org.apache.bcel.classfile.ElementValue;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.ClassMember;
import edu.umd.cs.findbugs.ba.JCIPAnnotationDatabase;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.bcel.BCELUtil;
import edu.umd.cs.findbugs.visitclass.AnnotationVisitor;

public class NoteJCIPAnnotation extends AnnotationVisitor implements Detector, NonReportingDetector {

    private static final String NET_JCIP_ANNOTATIONS = "net.jcip.annotations.";
    private static final String JSR305_CONCURRENT_ANNOTATIONS = "javax.annotation.concurrent.";

    public NoteJCIPAnnotation(BugReporter bugReporter) {
        super();
    }

    @Override
    public void visitAnnotation(String annotationClass, Map<String, ElementValue> map, boolean runtimeVisible) {
        if (annotationClass.startsWith(NET_JCIP_ANNOTATIONS)) {
            annotationClass = annotationClass.substring(NET_JCIP_ANNOTATIONS.length());
        } else if (annotationClass.startsWith(JSR305_CONCURRENT_ANNOTATIONS)) {
            annotationClass = annotationClass.substring(JSR305_CONCURRENT_ANNOTATIONS.length());
        } else {
            return;
        }
        JCIPAnnotationDatabase annotationDatabase = AnalysisContext.currentAnalysisContext()
                .getJCIPAnnotationDatabase();
        ElementValue value = map.get("value");
        ClassMember member;
        if (visitingField()) {
            member = XFactory.createXField(this);
        } else if (visitingMethod()) {
            member = XFactory.createXMethod(this);
        } else {
            annotationDatabase.addEntryForClass(getDottedClassName(), annotationClass, value);
            return;
        }
        annotationDatabase.addEntryForClassMember(member, annotationClass, value);
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        JavaClass javaClass = classContext.getJavaClass();
        if (!BCELUtil.preTiger(javaClass)) {
            javaClass.accept(this);
        }

    }

    @Override
    public void report() {
        // noop
    }

}
