/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
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

import org.apache.bcel.classfile.AnnotationEntry;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.ba.ClassContext;

/**
 * Detector to find private methods that are never called.
 */
public class FindUncalledPrivateMethods extends BytecodeScanningDetector implements StatelessDetector {
    private final BugReporter bugReporter;

    private String className;

    private HashSet<MethodAnnotation> definedPrivateMethods, calledMethods;

    private HashSet<String> calledMethodNames;

    public FindUncalledPrivateMethods(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitMethod(Method obj) {
        if (!obj.isPrivate() || obj.isSynthetic()) {
            return;
        }
        super.visitMethod(obj);
        String methodName = getMethodName();
        if (!methodName.equals("writeReplace") && !methodName.equals("readResolve")
                && !methodName.equals("readObject") && !methodName.equals("readObjectNoData")
                && !methodName.equals("writeObject")
                && methodName.indexOf("debug") == -1 && methodName.indexOf("Debug") == -1
                && methodName.indexOf("trace") == -1 && methodName.indexOf("Trace") == -1
                && !methodName.equals("<init>") && !methodName.equals("<clinit>")) {
            for(AnnotationEntry a : obj.getAnnotationEntries()) {
                String typeName =  a.getAnnotationType();
                if (typeName.equals("Ljavax/annotation/PostConstruct;")
                        || typeName.equals("Ljavax/annotation/PreDestroy;")) {
                    return;
                }
            }
            definedPrivateMethods.add(MethodAnnotation.fromVisitedMethod(this));
        }
    }

    @Override
    public void sawOpcode(int seen) {
        switch (seen) {
        case INVOKEVIRTUAL:
        case INVOKESPECIAL:
        case INVOKESTATIC:
            if (getDottedClassConstantOperand().equals(className)) {
                String className = getDottedClassConstantOperand();
                MethodAnnotation called = new MethodAnnotation(className, getNameConstantOperand(), getSigConstantOperand(),
                        seen == INVOKESTATIC);
                calledMethods.add(called);
                calledMethodNames.add(getNameConstantOperand().toLowerCase());
                // System.out.println("Saw call to " + called);

            }
            break;
        default:
            break;
        }
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        definedPrivateMethods = new HashSet<MethodAnnotation>();
        calledMethods = new HashSet<MethodAnnotation>();
        calledMethodNames = new HashSet<String>();
        className = classContext.getJavaClass().getClassName();
        String[] parts = className.split("[$+.]");
        String simpleClassName = parts[parts.length - 1];
        super.visitClassContext(classContext);

        definedPrivateMethods.removeAll(calledMethods);

        for (MethodAnnotation m : definedPrivateMethods) {
            // System.out.println("Checking " + m);
            int priority = LOW_PRIORITY;
            String methodName = m.getMethodName();
            if (methodName.equals(simpleClassName) && m.getMethodSignature().equals("()V")) {
                continue;
            }
            if (methodName.length() > 1 && calledMethodNames.contains(methodName.toLowerCase())) {
                priority = NORMAL_PRIORITY;
            }
            BugInstance bugInstance = new BugInstance(this, "UPM_UNCALLED_PRIVATE_METHOD", priority).addClass(this).addMethod(m);
            bugReporter.reportBug(bugInstance);
        }

        definedPrivateMethods = null;
        calledMethods = null;
    }
}

