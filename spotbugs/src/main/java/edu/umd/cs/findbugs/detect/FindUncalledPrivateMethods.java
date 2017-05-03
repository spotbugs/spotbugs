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
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.util.ClassName;

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
        if (!"writeReplace".equals(methodName) && !"readResolve".equals(methodName)
                && !"readObject".equals(methodName) && !"readObjectNoData".equals(methodName)
                && !"writeObject".equals(methodName)
                && methodName.indexOf("debug") == -1 && methodName.indexOf("Debug") == -1
                && methodName.indexOf("trace") == -1 && methodName.indexOf("Trace") == -1
                && !"<init>".equals(methodName) && !"<clinit>".equals(methodName)) {
            for(AnnotationEntry a : obj.getAnnotationEntries()) {
                String typeName =  a.getAnnotationType();
                if ("Ljavax/annotation/PostConstruct;".equals(typeName)
                        || "Ljavax/annotation/PreDestroy;".equals(typeName)) {
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

        ConstantPool cp = classContext.getJavaClass().getConstantPool();
        for(Constant constant : cp.getConstantPool()) {
            if(constant instanceof ConstantMethodHandle) {
                int kind = ((ConstantMethodHandle) constant).getReferenceKind();
                if(kind >= 5 && kind <= 9) {
                    Constant ref = cp.getConstant(((ConstantMethodHandle)constant).getReferenceIndex());
                    if(ref instanceof ConstantCP) {
                        String className = cp.getConstantString(((ConstantCP) ref).getClassIndex(), CONSTANT_Class);
                        ConstantNameAndType nameAndType = (ConstantNameAndType) cp.getConstant(((ConstantCP) ref).getNameAndTypeIndex());
                        String name = ((ConstantUtf8)cp.getConstant(nameAndType.getNameIndex())).getBytes();
                        String signature = ((ConstantUtf8)cp.getConstant(nameAndType.getSignatureIndex())).getBytes();
                        MethodAnnotation called = new MethodAnnotation(ClassName.toDottedClassName(className), name, signature, kind==6 /* invokestatic */);
                        calledMethods.add(called);
                        calledMethodNames.add(name.toLowerCase());
                    }
                }
            }
        }

        super.visitClassContext(classContext);

        definedPrivateMethods.removeAll(calledMethods);

        for (MethodAnnotation m : definedPrivateMethods) {
            // System.out.println("Checking " + m);
            int priority = LOW_PRIORITY;
            String methodName = m.getMethodName();
            if (methodName.equals(simpleClassName) && "()V".equals(m.getMethodSignature())) {
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

