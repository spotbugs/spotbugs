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
import org.apache.bcel.classfile.EnumElementValue;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.FirstPassDetector;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.bcel.BCELUtil;
import edu.umd.cs.findbugs.visitclass.AnnotationVisitor;

public class NoteAnnotationRetention extends AnnotationVisitor implements Detector, NonReportingDetector, FirstPassDetector {

    private boolean runtimeRetention;

    public NoteAnnotationRetention(BugReporter bugReporter) {
    }

    @Override
    public void visitAnnotation(String annotationClass, Map<String, ElementValue> map, boolean runtimeVisible) {

        if (!"java.lang.annotation.Retention".equals(annotationClass)) {
            return;
        }
        EnumElementValue v = (EnumElementValue) map.get("value");

        if ("RUNTIME".equals(v.getEnumValueString())) {
            runtimeRetention = true;
        }
    }

    @Override
    public void visit(JavaClass obj) {
        runtimeRetention = false;
    }

    @Override
    public void visitAfter(JavaClass obj) {
        for (String i : obj.getInterfaceNames()) {
            if ("java.lang.annotation.Annotation".equals(i)) {
                AnalysisContext.currentAnalysisContext().getAnnotationRetentionDatabase()
                .setRuntimeRetention(getDottedClassName(), runtimeRetention);
            }
        }

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

    }

}
