/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005 Jay Dunning
 * Copyright (C) 2005 University of Maryland
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

import java.util.Collection;
import java.util.LinkedList;

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class FindFloatEquality extends OpcodeStackDetector implements StatelessDetector {
    private static final int SAW_NOTHING = 0;

    private static final int SAW_COMP = 1;

    private int priority;

    private final BugReporter bugReporter;

    private final BugAccumulator bugAccumulator;

    private int state;

    public FindFloatEquality(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    Collection<SourceLineAnnotation> found = new LinkedList<SourceLineAnnotation>();

    @Override
    public void visit(Code obj) {
        found.clear();
        priority = LOW_PRIORITY;

        state = SAW_NOTHING;

        super.visit(obj);
        bugAccumulator.reportAccumulatedBugs();
        if (!found.isEmpty()) {
            BugInstance bug = new BugInstance(this, "FE_FLOATING_POINT_EQUALITY", priority).addClassAndMethod(this);

            boolean first = true;
            for (SourceLineAnnotation s : found) {
                bug.add(s);
                if (first) {
                    first = false;
                } else {
                    bug.describe(SourceLineAnnotation.ROLE_ANOTHER_INSTANCE);
                }
            }

            bugReporter.reportBug(bug);

            found.clear();
        }
    }

    public boolean isZero(Number n) {
        if (n == null) {
            return false;
        }
        double v = n.doubleValue();
        return v == 0.0;
    }

    public boolean okValueToCompareAgainst(Number n) {
        if (n == null) {
            return false;
        }
        double v = n.doubleValue();
        if (Double.isInfinite(v) || Double.isNaN(v)) {
            return true;
        }
        v = v - Math.floor(v);
        return v == 0.0;
    }

    @Override
    public void sawOpcode(int seen) {
        switch (seen) {
        case FCMPG:
        case FCMPL:
        case DCMPG:
        case DCMPL:
            if (stack.getStackDepth() >= 2) {
                OpcodeStack.Item first = stack.getStackItem(0);
                OpcodeStack.Item second = stack.getStackItem(1);

                if (first.getRegisterNumber() == second.getRegisterNumber() && first.getRegisterNumber() != -1) {
                    break;
                }
                if (first.isInitialParameter() && second.isInitialParameter()) {
                    break;
                }
                if (sameField(first, second)) {
                    break;
                }

                Number n1 = (Number) first.getConstant();
                Number n2 = (Number) second.getConstant();
                if (n1 != null && Double.isNaN(n1.doubleValue()) || n2 != null && Double.isNaN(n2.doubleValue())) {
                    BugInstance bug = new BugInstance(this, "FE_TEST_IF_EQUAL_TO_NOT_A_NUMBER", HIGH_PRIORITY)
                    .addClassAndMethod(this);
                    bugAccumulator.accumulateBug(bug, this);
                    state = SAW_NOTHING;
                    break;
                }
                if (first.getSpecialKind() == OpcodeStack.Item.NASTY_FLOAT_MATH && !isZero(n2)
                        || second.getSpecialKind() == OpcodeStack.Item.NASTY_FLOAT_MATH && !isZero(n1)
                        || first.getSpecialKind() == OpcodeStack.Item.FLOAT_MATH && !okValueToCompareAgainst(n2)
                        || second.getSpecialKind() == OpcodeStack.Item.FLOAT_MATH && !okValueToCompareAgainst(n1)) {
                    if (priority != HIGH_PRIORITY) {
                        found.clear();
                    }
                    priority = HIGH_PRIORITY;
                    state = SAW_COMP;
                    break;
                }
                if (priority == HIGH_PRIORITY) {
                    break;
                }
                // if (first.isInitialParameter() && n2 != null) break;
                // if (second.isInitialParameter() && n1 != null) break;
                if (n1 != null && n2 != null) {
                    break;
                }

                if (okValueToCompareAgainst(n1) || okValueToCompareAgainst(n2)) {
                    break;
                }
                if (n1 != null && !second.isInitialParameter() || n2 != null && !first.isInitialParameter()) {
                    if (priority == LOW_PRIORITY) {
                        found.clear();
                    }
                    priority = NORMAL_PRIORITY;

                } else if (priority == NORMAL_PRIORITY) {
                    break;
                }
                state = SAW_COMP;
            }
            break;

        case IFEQ:
        case IFNE:
            if (state == SAW_COMP) {
                SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(getClassContext(), this,
                        getPC());
                if (sourceLineAnnotation != null) {
                    found.add(sourceLineAnnotation);
                }
            }
            state = SAW_NOTHING;
            break;

        default:
            state = SAW_NOTHING;
            break;
        }
    }

    static boolean sameField(Item i1, Item i2) {
        if (i1.getXField() == null) {
            return false;
        }
        if (!i1.getXField().equals(i2.getXField())) {
            return false;
        }
        if (i1.getFieldLoadedFromRegister() != i2.getFieldLoadedFromRegister()) {
            return false;
        }
        return true;
    }
}
