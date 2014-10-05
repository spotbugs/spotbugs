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
import java.util.Set;

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;

public class VolatileUsage extends BytecodeScanningDetector {
    enum IncrementState {
        START, GETFIELD, LOADCONSTANT, ADD
    }

    private final BugReporter bugReporter;

    public VolatileUsage(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        classContext.getJavaClass().accept(this);
    }

    Set<XField> initializationWrites = new HashSet<XField>();

    Set<XField> otherWrites = new HashSet<XField>();

    IncrementState state = IncrementState.START;

    XField incrementField;

    @Override
    public void visit(Code obj) {
        resetIncrementState();
        super.visit(obj);
    }

    @Override
    public void sawOpcode(int seen) {
        switch (state) {
        case START:
            if (seen == GETFIELD) {
                XField f = getXFieldOperand();
                if (isVolatile(f)) {
                    incrementField = f;
                    state = IncrementState.GETFIELD;
                }
            }
            break;
        case GETFIELD:
            if (seen == ICONST_1 || seen == LCONST_1 || seen == ICONST_M1) {
                state = IncrementState.LOADCONSTANT;
            } else {
                resetIncrementState();
            }

            break;
        case LOADCONSTANT:
            if (seen == IADD || seen == ISUB || seen == LADD || seen == LSUB) {
                state = IncrementState.ADD;
            } else {
                resetIncrementState();
            }
            break;
        case ADD:
            if (seen == PUTFIELD && incrementField.equals(getXFieldOperand())) {
                bugReporter.reportBug(new BugInstance(this, "VO_VOLATILE_INCREMENT",
                        "J".equals(incrementField.getSignature()) ? Priorities.HIGH_PRIORITY : Priorities.NORMAL_PRIORITY)
                .addClassAndMethod(this).addField(incrementField).addSourceLine(this));
            }
            resetIncrementState();
            break;
        }
        switch (seen) {
        case PUTSTATIC: {
            XField f = getXFieldOperand();
            if (!isVolatileArray(f)) {
                return;
            }
            if ("<clinit>".equals(getMethodName())) {
                initializationWrites.add(f);
            } else {
                otherWrites.add(f);
            }
            break;
        }
        case PUTFIELD: {
            XField f = getXFieldOperand();
            if (!isVolatileArray(f)) {
                return;
            }

            if ("<init>".equals(getMethodName())) {
                initializationWrites.add(f);
            } else {
                otherWrites.add(f);
            }
            break;
        }
        default:
            break;
        }
    }

    /**
     *
     */
    private void resetIncrementState() {
        state = IncrementState.START;
        incrementField = null;
    }

    @Override
    public void report() {
        Subtypes2 subtypes2 = AnalysisContext.currentAnalysisContext().getSubtypes2();

        for (XField f : AnalysisContext.currentXFactory().allFields()) {
            if (isVolatileArray(f) && subtypes2.isApplicationClass(f.getClassDescriptor())) {
                int priority = LOW_PRIORITY;
                if (initializationWrites.contains(f) && !otherWrites.contains(f)) {
                    priority = NORMAL_PRIORITY;
                }
                bugReporter.reportBug(new BugInstance(this, "VO_VOLATILE_REFERENCE_TO_ARRAY", priority).addClass(
                        f.getClassDescriptor()).addField(f));
            }
        }
    }

    private boolean isVolatile(XField f) {
        return f != null && f.isVolatile();
    }

    private boolean isVolatileArray(XField f) {
        return isVolatile(f) && f.getSignature().charAt(0) == '[';
    }
}
