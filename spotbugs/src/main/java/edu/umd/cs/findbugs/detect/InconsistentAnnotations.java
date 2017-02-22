/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

import javax.annotation.meta.When;

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.UseAnnotationDatabase;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.interproc.ParameterProperty;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierAnnotation;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierApplications;
import edu.umd.cs.findbugs.ba.jsr305.TypeQualifierValue;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;

public class InconsistentAnnotations implements Detector, UseAnnotationDatabase {

    public final TypeQualifierValue<?> nonnullTypeQualifierValue;

    final BugReporter reporter;

    public InconsistentAnnotations(BugReporter reporter) {
        ClassDescriptor nonnullClassDesc = DescriptorFactory.createClassDescriptor(javax.annotation.Nonnull.class);
        this.nonnullTypeQualifierValue = TypeQualifierValue.getValue(nonnullClassDesc, null);
        this.reporter = reporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {

        JavaClass jclass = classContext.getJavaClass();

        for (Method method : jclass.getMethods()) {
            XMethod xmethod = XFactory.createXMethod(classContext.getJavaClass(), method);
            ParameterProperty nonnullParameters = AnalysisContext.currentAnalysisContext().getUnconditionalDerefParamDatabase()
                    .getProperty(xmethod.getMethodDescriptor());
            if (nonnullParameters != null) {
                for (int p : nonnullParameters.iterable()) {
                    TypeQualifierAnnotation directTypeQualifierAnnotation = TypeQualifierApplications
                            .getDirectTypeQualifierAnnotation(xmethod, p, nonnullTypeQualifierValue);
                    if (directTypeQualifierAnnotation != null && directTypeQualifierAnnotation.when == When.UNKNOWN) {
                        //
                        // The LocalVariableAnnotation is constructed using the
                        // local variable
                        // number of the parameter, not the parameter number.
                        //
                        int paramLocal = xmethod.isStatic() ? p : p + 1;

                        reporter.reportBug(new BugInstance(this, "NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE",
                                NORMAL_PRIORITY).addClassAndMethod(jclass, method).add(
                                        LocalVariableAnnotation.getParameterLocalVariableAnnotation(method, paramLocal)));

                    }

                }
            }
        }

    }

    @Override
    public void report() {
    }
}
