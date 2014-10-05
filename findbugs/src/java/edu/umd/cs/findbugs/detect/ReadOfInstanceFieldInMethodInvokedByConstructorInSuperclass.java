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
import java.util.Map;
import java.util.Set;

import javax.annotation.CheckForNull;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ProgramPoint;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.FieldSummary;
import edu.umd.cs.findbugs.ba.PutfieldScanner;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;

public class ReadOfInstanceFieldInMethodInvokedByConstructorInSuperclass extends OpcodeStackDetector {

    final BugAccumulator accumulator;

    public ReadOfInstanceFieldInMethodInvokedByConstructorInSuperclass(BugReporter bugReporter) {
        this.accumulator = new BugAccumulator(bugReporter);
    }

    Set<XField> initializedFields, nullCheckedFields;

    @Override
    public void visit(Code obj) {
        if (getMethod().isStatic()) {
            return;
        }
        initializedFields = new HashSet<XField>();
        nullCheckedFields = new HashSet<XField>();
        super.visit(obj);
        accumulator.reportAccumulatedBugs();
    }

    @Override
    public void sawOpcode(int opcode) {
        if (opcode == PUTFIELD) {
            XField f = getXFieldOperand();
            OpcodeStack.Item item = stack.getStackItem(1);
            if (item.getRegisterNumber() != 0) {
                return;
            }
            initializedFields.add(f);
            return;
        }
        if (opcode != GETFIELD) {
            return;
        }
        OpcodeStack.Item item = stack.getStackItem(0);
        if (item.getRegisterNumber() != 0) {
            return;
        }
        XField f = getXFieldOperand();

        if (f == null || !f.getClassDescriptor().equals(getClassDescriptor())) {
            return;
        }
        if (f.isSynthetic() || f.getName().startsWith("this$")) {
            return;
        }
        if (initializedFields.contains(f)) {
            return;
        }
        FieldSummary fieldSummary = AnalysisContext.currentAnalysisContext().getFieldSummary();

        ClassDescriptor superClassDescriptor = DescriptorFactory.createClassDescriptor(getSuperclassName());
        Set<ProgramPoint> calledFrom = fieldSummary.getCalledFromSuperConstructor(superClassDescriptor, getXMethod());
        if (calledFrom.isEmpty()) {
            return;
        }
        UnreadFieldsData unreadFields = AnalysisContext.currentAnalysisContext().getUnreadFieldsData();

        int priority;
        if (!unreadFields.isWrittenInConstructor(f)) {
            return;
        }

        if (f.isFinal()) {
            priority = HIGH_PRIORITY;
        } else if (unreadFields.isWrittenDuringInitialization(f) || unreadFields.isWrittenOutsideOfInitialization(f)) {
            priority = NORMAL_PRIORITY;
        } else {
            priority = HIGH_PRIORITY;
        }

        int nextOpcode = getNextOpcode();
        if (nullCheckedFields.contains(f) || nextOpcode == IFNULL || nextOpcode == IFNONNULL || nextOpcode == IFEQ
                || nextOpcode == IFNE) {
            priority++;
            nullCheckedFields.add(f);
        }

        for (ProgramPoint p : calledFrom) {
            XMethod upcall = getConstructorThatCallsSuperConstructor(p.method);
            if (upcall == null) {
                continue;
            }
            Method upcallMethod = null;
            for (Method m : getThisClass().getMethods()) {
                if (m.getName().equals(upcall.getName()) && m.getSignature().equals(upcall.getSignature())) {
                    upcallMethod = m;
                    break;
                }
            }
            if (upcallMethod == null) {
                continue;
            }
            Map<Integer, OpcodeStack.Item> putfieldsAt = PutfieldScanner.getPutfieldsFor(getThisClass(), upcallMethod, f);
            if (putfieldsAt.isEmpty()) {
                continue;
            }
            Map.Entry<Integer, OpcodeStack.Item> e = putfieldsAt.entrySet().iterator().next();
            int pc = e.getKey();
            OpcodeStack.Item value = e.getValue();
            if (value.isNull() || value.hasConstantValue(0)) {
                priority++;
            }

            SourceLineAnnotation fieldSetAt = SourceLineAnnotation.fromVisitedInstruction(getThisClass(), upcallMethod, pc);

            BugInstance bug = new BugInstance(this, "UR_UNINIT_READ_CALLED_FROM_SUPER_CONSTRUCTOR", priority).addClassAndMethod(
                    this).addField(f);
            bug.addMethod(p.method).describe(MethodAnnotation.METHOD_SUPERCLASS_CONSTRUCTOR)
            .addSourceLine(p.getSourceLineAnnotation()).describe(SourceLineAnnotation.ROLE_CALLED_FROM_SUPERCLASS_AT)
            .addMethod(upcall).describe(MethodAnnotation.METHOD_CONSTRUCTOR).add(fieldSetAt)
            .describe(SourceLineAnnotation.ROLE_FIELD_SET_TOO_LATE_AT);

            accumulator.accumulateBug(bug, this);
        }

    }

    private @CheckForNull
    XMethod getConstructorThatCallsSuperConstructor(XMethod superConstructor) {
        FieldSummary fieldSummary = AnalysisContext.currentAnalysisContext().getFieldSummary();

        XMethod lookfor = "()V".equals(superConstructor.getSignature()) ? null : superConstructor;
        for (XMethod m : getXClass().getXMethods()) {
            if ("<init>".equals(m.getName())) {
                if (fieldSummary.getSuperCall(m) == lookfor) {
                    return m;
                }
            }
        }
        return null;
    }

}
