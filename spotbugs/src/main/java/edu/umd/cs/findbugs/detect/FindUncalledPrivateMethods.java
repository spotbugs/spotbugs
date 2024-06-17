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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.AnnotationEntry;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantCP;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.ConstantMethodref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantPool;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.ElementValuePair;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.util.ClassName;
import edu.umd.cs.findbugs.util.NestedAccessUtil;

/**
 * Detector to find private methods that are never called.
 */
public class FindUncalledPrivateMethods extends BytecodeScanningDetector implements StatelessDetector {
    private final BugReporter bugReporter;

    private String className;

    private HashSet<MethodAnnotation> definedPrivateMethods, calledMethods;

    private HashSet<String> calledMethodNames;
    private Set<String> jUnitSourceMethodNames;

    public FindUncalledPrivateMethods(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitMethod(Method obj) {
        for (AnnotationEntry a : obj.getAnnotationEntries()) {
            String typeName = a.getAnnotationType();
            if ("Lorg/junit/jupiter/params/provider/MethodSource;".equals(typeName)) {
                boolean hasValue = false;
                for (ElementValuePair pair : a.getElementValuePairs()) {
                    if ("value".equals(pair.getNameString())) {
                        String sourceMethodName = pair.getValue().stringifyValue();
                        if (sourceMethodName.length() > 2) {
                            // Remove the leading '{' and trailing '}'
                            sourceMethodName = sourceMethodName.substring(1, sourceMethodName.length() - 1);
                            for (String name : sourceMethodName.split(",")) {
                                jUnitSourceMethodNames.add(name);
                                hasValue = true;
                            }
                        }
                    }
                }

                if (!hasValue) {
                    // In case there's no value JUnit will look for a method with the same name
                    jUnitSourceMethodNames.add(obj.getName());
                }
            }
        }

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
                && !Const.CONSTRUCTOR_NAME.equals(methodName) && !Const.STATIC_INITIALIZER_NAME.equals(methodName)) {
            for (AnnotationEntry a : obj.getAnnotationEntries()) {
                String typeName = a.getAnnotationType();
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
        case Const.INVOKEVIRTUAL:
        case Const.INVOKESPECIAL:
        case Const.INVOKESTATIC:
            if (getDottedClassConstantOperand().equals(className)) {
                String className = getDottedClassConstantOperand();
                MethodAnnotation called = new MethodAnnotation(className, getNameConstantOperand(), getSigConstantOperand(),
                        seen == Const.INVOKESTATIC);
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
        definedPrivateMethods = new HashSet<>();
        calledMethods = new HashSet<>();
        calledMethodNames = new HashSet<>();
        jUnitSourceMethodNames = new HashSet<>();
        JavaClass javaClass = classContext.getJavaClass();
        className = javaClass.getClassName();
        String[] parts = className.split("[$+.]");
        String simpleClassName = parts[parts.length - 1];


        if (NestedAccessUtil.supportsNestedAccess(javaClass)) {
            checkForNestedAccess(classContext, javaClass);
        }

        ConstantPool cp = classContext.getJavaClass().getConstantPool();
        for (Constant constant : cp.getConstantPool()) {
            if (constant instanceof ConstantMethodHandle) {
                int kind = ((ConstantMethodHandle) constant).getReferenceKind();
                if (kind >= 5 && kind <= 9) {
                    Constant ref = cp.getConstant(((ConstantMethodHandle) constant).getReferenceIndex());
                    if (ref instanceof ConstantCP) {
                        String className = cp.getConstantString(((ConstantCP) ref).getClassIndex(), Const.CONSTANT_Class);
                        ConstantNameAndType nameAndType = (ConstantNameAndType) cp.getConstant(((ConstantCP) ref).getNameAndTypeIndex());
                        String name = ((ConstantUtf8) cp.getConstant(nameAndType.getNameIndex())).getBytes();
                        String signature = ((ConstantUtf8) cp.getConstant(nameAndType.getSignatureIndex())).getBytes();
                        MethodAnnotation called = new MethodAnnotation(ClassName.toDottedClassName(className), name, signature,
                                kind == 6 /* invokestatic */);
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
            if (m.isStatic() && m.toXMethod().getNumParams() == 0 && jUnitSourceMethodNames.contains(methodName)) {
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

    private void checkForNestedAccess(ClassContext classContext, JavaClass javaClass) {
        AnalysisContext analysisContext = classContext.getAnalysisContext();
        List<String> nestMateClassNames = Collections.EMPTY_LIST;
        try {
            nestMateClassNames = NestedAccessUtil.getNestMateClassNames(javaClass, analysisContext);
        } catch (ClassNotFoundException e) {
            bugReporter.reportMissingClass(e);
        }
        for (String nestMateClassName : nestMateClassNames) {
            try {
                JavaClass nestMemberClass = analysisContext.lookupClass(nestMateClassName);
                if (nestMemberClass.equals(javaClass)) {
                    continue;
                }
                ConstantPool cp = nestMemberClass.getConstantPool();
                for (Constant constant : nestMemberClass.getConstantPool().getConstantPool()) {
                    if (constant instanceof ConstantMethodref) {
                        ConstantMethodref ref = (ConstantMethodref) constant;
                        ConstantNameAndType nt = (ConstantNameAndType) cp.getConstant(ref.getNameAndTypeIndex());
                        String name = ((ConstantUtf8) cp.getConstant(nt.getNameIndex(), Const.CONSTANT_Utf8))
                                .getBytes();
                        String signature = ((ConstantUtf8) cp.getConstant(nt.getSignatureIndex(), Const.CONSTANT_Utf8))
                                .getBytes();
                        /*
                         * We don't check if the method is static, since that is not relevant for the actual error
                         * reporting. Called methods are removed from "definedPrivateMethods" via their hash code, which
                         * consists of class name, method name and method signature. Finding out whether the method is
                         * static will require checking in "definedPrivateMethods".
                         */
                        boolean isStatic = false;
                        String nestMemberClassName = getClassName(nestMemberClass, ref.getClassIndex());
                        MethodAnnotation called = new MethodAnnotation(ClassName.toDottedClassName(nestMemberClassName),
                                name, signature, isStatic);
                        calledMethods.add(called);
                        calledMethodNames.add(name.toLowerCase());
                    }
                }
            } catch (ClassNotFoundException e) {
                bugReporter.reportMissingClass(e);
            }
        }
    }

    private static String getClassName(JavaClass c, int classIndex) {
        String name = c.getConstantPool().getConstantString(classIndex, Const.CONSTANT_Class);
        return ClassName.toDottedClassName(ClassName.extractClassName(name));
    }
}
