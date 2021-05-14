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

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.util.MutableClasses;

public class FindReturnRef extends OpcodeStackDetector {
    boolean check = false;

    boolean publicClass = false;

    boolean staticMethod = false;

    int parameterCount;

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
        check = publicClass && (obj.getAccessFlags() & (Const.ACC_PUBLIC)) != 0;
        if (!check) {
            return;
        }
        staticMethod = (obj.getAccessFlags() & (Const.ACC_STATIC)) != 0;
        // variableNames = obj.getLocalVariableTable();
        parameterCount = getNumberMethodArguments();

        if (!staticMethod) {
            parameterCount++;
        }

        super.visit(obj);
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

        if (staticMethod && seen == Const.PUTSTATIC && nonPublicFieldOperand()
                && MutableClasses.mutableSignature(getSigConstantOperand())) {
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
        if (!staticMethod && seen == Const.PUTFIELD && nonPublicFieldOperand()
                && MutableClasses.mutableSignature(getSigConstantOperand())) {
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

        if (seen == Const.ARETURN) {
            OpcodeStack.Item item = stack.getStackItem(0);
            XField field = item.getXField();
            if (field == null ||
                    !field.getClassDescriptor().equals(getClassDescriptor()) ||
                    field.isPublic() ||
                    AnalysisContext.currentXFactory().isEmptyArrayField(field) ||
                    field.getName().indexOf("EMPTY") != -1 ||
                    !MutableClasses.mutableSignature(field.getSignature())) {
                return;
            }
            bugAccumulator.accumulateBug(new BugInstance(this, staticMethod ? "MS_EXPOSE_REP" : "EI_EXPOSE_REP", NORMAL_PRIORITY)
                    .addClassAndMethod(this).addField(field.getClassName(), field.getName(), field.getSignature(), field.isStatic()), this);

        }
    }

    private boolean nonPublicFieldOperand() {
        XField xField = getXFieldOperand();
        return xField == null || !xField.isPublic();
    }

    private boolean isPotentialCapture(OpcodeStack.Item top) {
        if (!top.isInitialParameter()) {
            return false;
        }
        if ((getMethod().getAccessFlags() & Const.ACC_VARARGS) == 0) {
            return true;
        }
        // var-arg parameter
        return top.getRegisterNumber() != parameterCount - 1;

    }
}
