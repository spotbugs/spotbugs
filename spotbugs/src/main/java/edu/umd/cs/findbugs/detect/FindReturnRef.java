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

import java.util.Map;
import java.util.HashMap;

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
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.util.MutableClasses;

public class FindReturnRef extends OpcodeStackDetector {
    boolean check = false;

    boolean publicClass = false;

    boolean staticMethod = false;

    int parameterCount;

    private OpcodeStack.Item paramUnderClone = null;
    private OpcodeStack.Item paramCloneUnderCast = null;
    private Map<OpcodeStack.Item, OpcodeStack.Item> arrayParamClones =
            new HashMap<OpcodeStack.Item, OpcodeStack.Item>();

    private XField fieldUnderClone = null;
    private XField fieldCloneUnderCast = null;
    private Map<OpcodeStack.Item, XField> arrayFieldClones =
            new HashMap<OpcodeStack.Item, XField>();

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

        paramUnderClone = null;
        paramCloneUnderCast = null;
        fieldUnderClone = null;
        fieldCloneUnderCast = null;

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
            if (field == null) {
                field = arrayFieldClones.get(item);
            }
            if (field == null ||
                    !isFieldOf(field, getClassDescriptor()) ||
                    field.isPublic() ||
                    AnalysisContext.currentXFactory().isEmptyArrayField(field) ||
                    field.getName().indexOf("EMPTY") != -1 ||
                    !MutableClasses.mutableSignature(field.getSignature())) {
                return;
            }
            bugAccumulator.accumulateBug(new BugInstance(this, staticMethod ? "MS_EXPOSE_REP" : "EI_EXPOSE_REP", NORMAL_PRIORITY)
                    .addClassAndMethod(this).addField(field.getClassName(), field.getName(), field.getSignature(), field.isStatic()), this);
        }

        if (seen == Const.INVOKEINTERFACE || seen == Const.INVOKEVIRTUAL) {
            MethodDescriptor method = getMethodDescriptorOperand();
            OpcodeStack.Item item = stack.getStackItem(0);
            XField field = item.getXField();
            if (method == null || !"clone".equals(method.getName()) ||
                    !item.isArray() ||
                    !MutableClasses.mutableSignature(item.getSignature().substring(1))) {
                return;
            }

            if (field != null && field.getClassDescriptor().equals(getClassDescriptor()) &&
                    !field.isPublic()) {
                fieldUnderClone = field;
            } else if (item.isInitialParameter()) {
                System.err.println("Cloning: " + item);
                paramUnderClone = item;
            }
        }

        if (seen == Const.CHECKCAST) {
            OpcodeStack.Item item = stack.getStackItem(0);
            XField field = arrayFieldClones.get(item);
            if (field != null) {
                fieldCloneUnderCast = field;
            }
            OpcodeStack.Item param = arrayParamClones.get(item);
            if (param != null) {
                System.err.println("Casting: " + item + " which is a clone of " + param);
                paramCloneUnderCast = param;
            }
        }
    }

    @Override
    public void afterOpcode(int seen) {
        super.afterOpcode(seen);

        if (seen == Const.INVOKEINTERFACE || seen == Const.INVOKEVIRTUAL) {
            if (fieldUnderClone != null) {
                arrayFieldClones.put(stack.getStackItem(0), fieldUnderClone);
            }
            if (paramUnderClone != null) {
                arrayParamClones.put(stack.getStackItem(0), paramUnderClone);
            }
        }

        if (seen == Const.CHECKCAST) {
            OpcodeStack.Item item = stack.getStackItem(0);
            if (fieldCloneUnderCast != null) {
                arrayFieldClones.put(item, fieldCloneUnderCast);
            }
            if (paramCloneUnderCast != null) {
                arrayParamClones.put(item, paramCloneUnderCast);
            }
        }
    }

    private boolean nonPublicFieldOperand() {
        XField xField = getXFieldOperand();
        return xField == null || !xField.isPublic();
    }

    private boolean isPotentialCapture(OpcodeStack.Item top) {
        if (!top.isInitialParameter()) {
            top = arrayParamClones.get(top);
            if (top == null) {
                return false;
            }
            System.err.println("This is a clone:" + top);
        }
        if (!top.isInitialParameter()) {
            return false;
        }
        if ((getMethod().getAccessFlags() & Const.ACC_VARARGS) == 0) {
            return true;
        }
        // var-arg parameter
        return top.getRegisterNumber() != parameterCount - 1;

    }

    private boolean isFieldOf(XField field, ClassDescriptor klass) {
        do {
            if (field.getClassDescriptor().equals(klass)) {
                return true;
            }
            try {
                klass = klass.getXClass().getSuperclassDescriptor();
            } catch (CheckedAnalysisException e) {
                AnalysisContext.logError("Error checking for class " + klass, e);
                return false;
            }
        } while (klass != null);
        return false;
    }
}
