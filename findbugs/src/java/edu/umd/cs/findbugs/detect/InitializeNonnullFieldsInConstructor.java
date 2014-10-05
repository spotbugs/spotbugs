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

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.NullnessAnnotation;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class InitializeNonnullFieldsInConstructor extends OpcodeStackDetector {

    final BugReporter bugReporter;

    final HashSet<XField> initializedFields = new HashSet<XField>();

    final HashSet<XField> nonnullFields = new HashSet<XField>();

    final HashSet<XField> nonnullStaticFields = new HashSet<XField>();

    public InitializeNonnullFieldsInConstructor(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void setupVisitorForClass(JavaClass obj) {
        super.setupVisitorForClass(obj);
        nonnullFields.clear();
    }

    @Override
    public void visitAfter(JavaClass obj) {
        super.visitAfter(obj);
        nonnullFields.clear();
        nonnullStaticFields.clear();

    }

    @Override
    public void visit(Field obj) {
        super.visit(obj);
        XField f = XFactory.createXField(this);
        if (checkForInitialization(f) && !f.isSynthetic()) {
            if (f.isStatic()) {
                nonnullStaticFields.add(f);
            } else {
                nonnullFields.add(f);
            }
        }
    }

    public boolean checkForInitialization(XField f) {
        if (!f.isReferenceType() || f.isFinal()) {
            return false;
        }
        NullnessAnnotation annotation = AnalysisContext.currentAnalysisContext().getNullnessAnnotationDatabase()
                .getResolvedAnnotation(f, false);
        boolean isNonnull = annotation == NullnessAnnotation.NONNULL;
        return isNonnull;
    }

    @Override
    public void visit(Code code) {
        boolean interesting = "<init>".equals(getMethodName()) || "<clinit>".equals(getMethodName());
        if (!interesting) {
            return;
        }

        secondaryConstructor = false;
        HashSet<XField> needToInitialize = getMethod().isStatic() ? nonnullStaticFields : nonnullFields;
        if (needToInitialize.isEmpty()) {
            return;
        }
        // initialize any variables we want to initialize for the method
        super.visit(code); // make callbacks to sawOpcode for all opcodes
        if (!secondaryConstructor && !initializedFields.containsAll(needToInitialize)) {
            int priority = Priorities.NORMAL_PRIORITY;
            if (needToInitialize.size() - initializedFields.size() == 1 && needToInitialize.size() > 1) {
                priority = Priorities.HIGH_PRIORITY;
            }

            for (XField f : needToInitialize) {
                if (initializedFields.contains(f)) {
                    continue;
                }

                BugInstance b = new BugInstance(this, "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", priority)
                .addClassAndMethod(this).addField(f);
                bugReporter.reportBug(b);
            }

        }
        initializedFields.clear();

    }

    boolean secondaryConstructor;

    @Override
    public void sawOpcode(int seen) {

        if (secondaryConstructor) {
            return;
        }

        switch (seen) {
        case Constants.INVOKESPECIAL:
            if (!getMethod().isStatic() && "<init>".equals(getNameConstantOperand()) && isSelfOperation()) {
                OpcodeStack.Item invokedOn = stack.getItemMethodInvokedOn(this);
                if (invokedOn.isInitialParameter() && invokedOn.getRegisterNumber() == 0) {
                    secondaryConstructor = true;
                }
                break;
            }
            break;
        case Constants.PUTFIELD:
            if (getMethod().isStatic()) {
                return;
            }
            OpcodeStack.Item left = stack.getStackItem(1);
            if (left.isInitialParameter() && left.getRegisterNumber() == 0 && isSelfOperation()) {
                XField f = getXFieldOperand();
                if (f == null) {
                    break;
                }
                if (checkForInitialization(f)) {
                    initializedFields.add(f);
                }
            }
            break;
        case Constants.PUTSTATIC:
            if (!getMethod().isStatic()) {
                break;
            }

            if (isSelfOperation()) {
                XField f = getXFieldOperand();
                if (f == null) {
                    break;
                }

                if (checkForInitialization(f)) {
                    initializedFields.add(f);
                }
            }
            break;
        default:
            break;

        }

    }

    public boolean isSelfOperation() {
        return getClassConstantOperand().equals(getClassName());
    }

}
