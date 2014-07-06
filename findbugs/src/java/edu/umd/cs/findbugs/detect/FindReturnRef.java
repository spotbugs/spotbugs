/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class FindReturnRef extends OpcodeStackDetector {
    boolean check = false;

    boolean thisOnTOS = false;

    boolean fieldOnTOS = false;

    boolean publicClass = false;

    boolean staticMethod = false;

    String nameOnStack;

    String classNameOnStack;

    String sigOnStack;

    int parameterCount;

    boolean fieldIsStatic;

    private final BugAccumulator bugAccumulator;

    // private LocalVariableTable variableNames;

    public FindReturnRef(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visit(JavaClass obj) {
        publicClass = obj.isPublic();
        super.visit(obj);
    }

    @Override
    public void visitAfter(JavaClass obj) {
        bugAccumulator.reportAccumulatedBugs();
    }

    @Override
    public void visit(Method obj) {
        check = publicClass && (obj.getAccessFlags() & (ACC_PUBLIC)) != 0;
        if (!check) {
            return;
        }
        staticMethod = (obj.getAccessFlags() & (ACC_STATIC)) != 0;
        // variableNames = obj.getLocalVariableTable();
        parameterCount = getNumberMethodArguments();

        if (!staticMethod) {
            parameterCount++;
        }

        thisOnTOS = false;
        fieldOnTOS = false;
        super.visit(obj);
        thisOnTOS = false;
        fieldOnTOS = false;
    }

    @Override
    public void visit(Code obj) {
        if (check) {
            super.visit(obj);
        }
    }

    @Override
    public void sawOpcode(int seen) {

        if (!check) {
            return;
        }

        if (staticMethod && seen == PUTSTATIC && MutableStaticFields.mutableSignature(getSigConstantOperand())) {
            OpcodeStack.Item top = stack.getStackItem(0);
            if (isPotentialCapture(top)) {
                bugAccumulator.accumulateBug(
                        new BugInstance(this, "EI_EXPOSE_STATIC_REP2", NORMAL_PRIORITY)
                        .addClassAndMethod(this)
                        .addReferencedField(this)
                        .add(LocalVariableAnnotation.getLocalVariableAnnotation(getMethod(), top.getRegisterNumber(),
                                getPC(), getPC() - 1)), this);
            }
        }
        if (!staticMethod && seen == PUTFIELD && MutableStaticFields.mutableSignature(getSigConstantOperand())) {
            OpcodeStack.Item top = stack.getStackItem(0);
            OpcodeStack.Item target = stack.getStackItem(1);
            if (isPotentialCapture(top) && target.getRegisterNumber() == 0) {
                bugAccumulator.accumulateBug(
                        new BugInstance(this, "EI_EXPOSE_REP2", NORMAL_PRIORITY)
                        .addClassAndMethod(this)
                        .addReferencedField(this)
                        .add(LocalVariableAnnotation.getLocalVariableAnnotation(getMethod(), top.getRegisterNumber(),
                                getPC(), getPC() - 1)), this);
            }
        }

        if (seen == ALOAD_0 && !staticMethod) {
            thisOnTOS = true;
            fieldOnTOS = false;
            return;
        }

        if (thisOnTOS && seen == GETFIELD && getClassConstantOperand().equals(getClassName())
                && !AnalysisContext.currentXFactory().isEmptyArrayField(getXFieldOperand())) {
            fieldOnTOS = true;
            thisOnTOS = false;
            nameOnStack = getNameConstantOperand();
            classNameOnStack = getDottedClassConstantOperand();
            sigOnStack = getSigConstantOperand();
            fieldIsStatic = false;
            return;
        }
        if (seen == GETSTATIC && getClassConstantOperand().equals(getClassName())
                && !AnalysisContext.currentXFactory().isEmptyArrayField(getXFieldOperand())) {
            fieldOnTOS = true;
            thisOnTOS = false;
            nameOnStack = getNameConstantOperand();
            classNameOnStack = getDottedClassConstantOperand();
            sigOnStack = getSigConstantOperand();
            fieldIsStatic = true;

            return;
        }
        thisOnTOS = false;
        if (check && fieldOnTOS && seen == ARETURN
                /*
                 * && !sigOnStack.equals("Ljava/lang/String;") &&
                 * sigOnStack.indexOf("Exception") == -1 && sigOnStack.indexOf("[") >= 0
                 */
                && nameOnStack.indexOf("EMPTY") == -1 && MutableStaticFields.mutableSignature(sigOnStack)) {
            bugAccumulator.accumulateBug(new BugInstance(this, staticMethod ? "MS_EXPOSE_REP" : "EI_EXPOSE_REP", NORMAL_PRIORITY)
            .addClassAndMethod(this).addField(classNameOnStack, nameOnStack, sigOnStack, fieldIsStatic), this);
        }

        fieldOnTOS = false;
        thisOnTOS = false;
    }

    private boolean isPotentialCapture(OpcodeStack.Item top) {
        if (!top.isInitialParameter()) {
            return false;
        }
        if ((getMethod().getAccessFlags() & ACC_VARARGS) == 0) {
            return true;
        }
        if (top.getRegisterNumber() == parameterCount - 1)
        {
            return false; // var-arg parameter
        }
        return true;

    }
}
