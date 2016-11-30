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

import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ProgramPoint;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.FieldSummary;
import edu.umd.cs.findbugs.ba.Hierarchy2;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

public class FieldItemSummary extends OpcodeStackDetector implements NonReportingDetector {

    FieldSummary fieldSummary = new FieldSummary();

    public FieldItemSummary(BugReporter bugReporter) {
        AnalysisContext context = AnalysisContext.currentAnalysisContext();
        context.setFieldSummary(fieldSummary);
    }

    Set<XField> touched = new HashSet<XField>();

    @Override
    public boolean shouldVisit(JavaClass obj) {
        return !getXClass().hasStubs();
    }

    boolean sawInitializeSuper;

    @Override
    public void sawOpcode(int seen) {
        if ("<init>".equals(getMethodName()) && seen == INVOKEVIRTUAL) {
            XMethod m = getXMethodOperand();
            if (m != null && !m.isPrivate() && !m.isFinal()) {
                int args = PreorderVisitor.getNumberArguments(m.getSignature());
                OpcodeStack.Item item = stack.getStackItem(args);
                if (item.getRegisterNumber() == 0) {
                    try {
                        Set<XMethod> targets = Hierarchy2.resolveVirtualMethodCallTargets(m, false, false);
                        Subtypes2 subtypes2 = AnalysisContext.currentAnalysisContext().getSubtypes2();

                        for (XMethod called : targets) {
                            if (!called.isAbstract() && !called.equals(m)
                                    && subtypes2.isSubtype(called.getClassDescriptor(), getClassDescriptor())) {
                                fieldSummary.setCalledFromSuperConstructor(new ProgramPoint(this), called);
                            }
                        }
                    } catch (ClassNotFoundException e) {
                        AnalysisContext.reportMissingClass(e);
                    }

                }

            }

        }

        if (seen == INVOKESPECIAL && "<init>".equals(getMethodName()) && "<init>".equals(getNameConstantOperand())) {

            String classOperand = getClassConstantOperand();
            OpcodeStack.Item invokedOn = stack.getItemMethodInvokedOn(this);
            if (invokedOn.getRegisterNumber() == 0 && !classOperand.equals(getClassName())) {
                sawInitializeSuper = true;
                XMethod invoked = getXMethodOperand();
                if (invoked != null) {
                    fieldSummary.sawSuperCall(getXMethod(), invoked);
                }
            }

        }

        if (seen == PUTFIELD || seen == PUTSTATIC) {
            XField fieldOperand = getXFieldOperand();
            if (fieldOperand == null) {
                return;
            }
            touched.add(fieldOperand);
            if (!fieldOperand.getClassDescriptor().getClassName().equals(getClassName())) {
                fieldSummary.addWrittenOutsideOfConstructor(fieldOperand);
            } else if (seen == PUTFIELD) {
                OpcodeStack.Item addr = stack.getStackItem(1);
                {
                    if (addr.getRegisterNumber() != 0 || !"<init>".equals(getMethodName())) {
                        fieldSummary.addWrittenOutsideOfConstructor(fieldOperand);
                    }
                }
            } else if (seen == PUTSTATIC && !"<clinit>".equals(getMethodName())) {
                fieldSummary.addWrittenOutsideOfConstructor(fieldOperand);
            }
            OpcodeStack.Item top = stack.getStackItem(0);
            fieldSummary.mergeSummary(fieldOperand, top);
        }

    }

    @Override
    public void visit(Code obj) {
        sawInitializeSuper = false;
        super.visit(obj);
        fieldSummary.setFieldsWritten(getXMethod(), touched);
        if ("<init>".equals(getMethodName()) && sawInitializeSuper) {
            XClass thisClass = getXClass();
            for (XField f : thisClass.getXFields()) {
                if (!f.isStatic() && !f.isFinal() && !touched.contains(f)) {
                    OpcodeStack.Item item;
                    char firstChar = f.getSignature().charAt(0);
                    if (firstChar == 'L' || firstChar == '[') {
                        item = OpcodeStack.Item.nullItem(f.getSignature());
                    } else if (firstChar == 'I') {
                        item = new OpcodeStack.Item("I", (Integer) 0);
                    } else if (firstChar == 'J') {
                        item = new OpcodeStack.Item("J", 0L);
                    } else {
                        item = new OpcodeStack.Item(f.getSignature());
                    }
                    fieldSummary.mergeSummary(f, item);
                }
            }
        }
        touched.clear();
    }

    @Override
    public void report() {
        fieldSummary.setComplete(true);
    }

}
