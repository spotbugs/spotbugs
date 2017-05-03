/*
 * FindBugs - Find bugs in Java programs
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

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.bcel.BCELUtil;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class ConfusionBetweenInheritedAndOuterMethod extends OpcodeStackDetector {

    BugAccumulator bugAccumulator;

    BugInstance iteratorBug;

    public ConfusionBetweenInheritedAndOuterMethod(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visitJavaClass(JavaClass obj) {
        isInnerClass = false;
        // totally skip methods not defined in inner classes
        if (obj.getClassName().indexOf('$') >= 0) {
            super.visitJavaClass(obj);
            bugAccumulator.reportAccumulatedBugs();
        }

    }

    boolean isInnerClass;

    @Override
    public void visit(Field f) {
        if (f.getName().startsWith("this$")) {
            isInnerClass = true;
        }
    }

    @Override
    public void visit(Code obj) {
        if (isInnerClass  && !BCELUtil.isSynthetic(getMethod())) {
            super.visit(obj);
            iteratorBug = null;
        }
    }

    @Override
    public void sawOpcode(int seen) {
        if (iteratorBug != null) {
            if (isRegisterStore()) {
                LocalVariableTable lvt = getMethod().getLocalVariableTable();
                if (lvt != null) {
                    LocalVariable localVariable = lvt.getLocalVariable(getRegisterOperand(), getNextPC());
                    if(localVariable == null || localVariable.getName().endsWith("$")) {
                        // iterator() result is stored to the synthetic variable which has no name in LVT or name is suffixed with '$'
                        // Looks like it's for-each cycle like for(Object obj : this)
                        // Do not report such case
                        iteratorBug = null;
                    }
                }
            }
            if(iteratorBug != null) {
                bugAccumulator.accumulateBug(iteratorBug, this);
            }
            iteratorBug = null;
        }
        if (seen != INVOKEVIRTUAL) {
            return;
        }
        if (!getClassName().equals(getClassConstantOperand())) {
            return;
        }
        XMethod invokedMethod = XFactory.createXMethod(getDottedClassConstantOperand(), getNameConstantOperand(),
                getSigConstantOperand(), false);
        if (invokedMethod.isResolved() && invokedMethod.getClassName().equals(getDottedClassConstantOperand())
                || invokedMethod.isSynthetic()) {
            return;
        }
        if(getStack().getStackItem(getNumberArguments(getSigConstantOperand())).getRegisterNumber() != 0) {
            // called not for this object
            return;
        }
        // method is inherited
        String possibleTargetClass = getDottedClassName();
        String superClassName = getDottedSuperclassName();
        while (true) {
            int i = possibleTargetClass.lastIndexOf('$');
            if (i <= 0) {
                break;
            }
            possibleTargetClass = possibleTargetClass.substring(0, i);
            if (possibleTargetClass.equals(superClassName)) {
                break;
            }
            XMethod alternativeMethod = XFactory.createXMethod(possibleTargetClass, getNameConstantOperand(),
                    getSigConstantOperand(), false);
            if (alternativeMethod.isResolved() && alternativeMethod.getClassName().equals(possibleTargetClass)) {
                String targetPackage = invokedMethod.getPackageName();
                String alternativePackage = alternativeMethod.getPackageName();
                int priority = HIGH_PRIORITY;
                if (targetPackage.equals(alternativePackage)) {
                    priority++;
                }
                if (targetPackage.startsWith("javax.swing") || targetPackage.startsWith("java.awt")) {
                    priority += 2;
                }
                if (invokedMethod.getName().equals(getMethodName())) {
                    priority++;
                }

                BugInstance bug = new BugInstance(this, "IA_AMBIGUOUS_INVOCATION_OF_INHERITED_OR_OUTER_METHOD", priority)
                .addClassAndMethod(this).addMethod(invokedMethod).describe("METHOD_INHERITED")
                .addMethod(alternativeMethod).describe("METHOD_ALTERNATIVE_TARGET");
                if(invokedMethod.getName().equals("iterator") && invokedMethod.getSignature().equals("()Ljava/util/Iterator;")
                        && Subtypes2.instanceOf(getDottedClassName(), "java.lang.Iterable")) {
                    iteratorBug = bug;
                } else {
                    bugAccumulator.accumulateBug(bug, this);
                }
                break;
            }
        }
    }
}
