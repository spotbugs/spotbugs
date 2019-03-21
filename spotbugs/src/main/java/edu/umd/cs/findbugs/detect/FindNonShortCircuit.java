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

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class FindNonShortCircuit extends OpcodeStackDetector implements StatelessDetector {

    static final String NS_NON_SHORT_CIRCUIT = "NS_NON_SHORT_CIRCUIT";

    static final String NS_DANGEROUS_NON_SHORT_CIRCUIT = "NS_DANGEROUS_NON_SHORT_CIRCUIT";

    int stage1 = 0;

    int stage2 = 0;

    int distance = 0;

    int operator;

    boolean sawDanger;

    boolean sawNullTestOld;

    boolean sawNullTestVeryOld;

    boolean sawNullTest;

    boolean sawDangerOld;

    boolean sawNumericTest, sawNumericTestOld, sawNumericTestVeryOld;

    boolean sawArrayDanger, sawArrayDangerOld;

    boolean sawMethodCall, sawMethodCallOld;

    private final BugAccumulator bugAccumulator;

    public FindNonShortCircuit(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visit(Method obj) {
        clearAll();
        prevOpcode = Const.NOP;
    }

    private void clearAll() {
        stage1 = 0;
        stage2 = 0;
        distance = 1000000;
        sawArrayDanger = sawArrayDangerOld = false;
        sawDanger = sawDangerOld = false;
        sawMethodCall = sawMethodCallOld = false;
        sawNullTest = sawNullTestOld = sawNullTestVeryOld = false;
        sawNumericTest = sawNumericTestOld = sawNumericTestVeryOld = false;
    }

    int prevOpcode;

    @Override
    public void visit(Code code) {
        super.visit(code);
        bugAccumulator.reportAccumulatedBugs();
    }

    @Override
    public void sawOpcode(int seen) {
        // System.out.println(getPC() + " " + Const.getOpcodeName(seen) + " " + stage1
        // + " " + stage2);
        // System.out.println(stack);
        // System.out.println(getPC() + " " + Const.getOpcodeName(seen) + " " +
        // sawMethodCall + " " + sawMethodCallOld + " " + stage1 + " " +
        // stage2);
        distance++;
        scanForBooleanValue(seen);
        scanForDanger(seen);
        scanForShortCircuit(seen);
        prevOpcode = seen;
    }

    private void scanForDanger(int seen) {
        switch (seen) {
        case Const.AALOAD:
        case Const.BALOAD:
        case Const.SALOAD:
        case Const.CALOAD:
        case Const.IALOAD:
        case Const.LALOAD:
        case Const.FALOAD:
        case Const.DALOAD:
            sawArrayDanger = true;
            sawDanger = true;
            break;

        case Const.INVOKEVIRTUAL:
            if ("length".equals(getNameConstantOperand()) && "java/lang/String".equals(getClassConstantOperand())) {
                break;
            }
            sawDanger = true;
            sawMethodCall = true;
            break;
        case Const.INVOKEINTERFACE:
        case Const.INVOKESPECIAL:
        case Const.INVOKESTATIC:
            sawDanger = true;
            sawMethodCall = true;
            break;
        case Const.IDIV:
        case Const.IREM:
        case Const.LDIV:
        case Const.LREM:
            sawDanger = true;
            break;

        case Const.ARRAYLENGTH:
        case Const.GETFIELD:
            // null pointer detector will handle these
            break;
        default:
            break;
        }

    }

    private void scanForShortCircuit(int seen) {
        switch (seen) {
        case Const.IAND:
        case Const.IOR:

            // System.out.println("Saw IOR or IAND at distance " + distance);
            OpcodeStack.Item item0 = stack.getStackItem(0);
            OpcodeStack.Item item1 = stack.getStackItem(1);
            if (item0.getConstant() == null && item1.getConstant() == null && distance < 4) {
                //                if (item0.getRegisterNumber() >= 0 && item1.getRegisterNumber() >= 0) {
                //                    if (false) {
                //                        clearAll();
                //                    }
                //                }
                operator = seen;
                stage2 = 1;
            } else {
                stage2 = 0;
            }
            break;
        case Const.IFEQ:
        case Const.IFNE:
            if (stage2 == 1) {
                // System.out.println("Found nsc");
                reportBug();
            }
            stage2 = 0;
            break;
        case Const.PUTFIELD:
        case Const.PUTSTATIC:
        case Const.IRETURN:
            if (operator == Const.IAND && stage2 == 1) {
                reportBug();
            }
            stage2 = 0;
            break;
        default:
            stage2 = 0;
            break;
        }
    }

    private void reportBug() {
        bugAccumulator.accumulateBug(createBugInstance().addClassAndMethod(this), this);
    }

    BugInstance createBugInstance() {
        int priority = LOW_PRIORITY;
        String pattern = NS_NON_SHORT_CIRCUIT;

        if (sawDangerOld) {
            if (sawNullTestVeryOld) {
                priority = HIGH_PRIORITY;
            } else if (sawMethodCallOld || sawNumericTestVeryOld && sawArrayDangerOld) {
                priority = HIGH_PRIORITY;
                pattern = NS_DANGEROUS_NON_SHORT_CIRCUIT;
            } else {
                priority = NORMAL_PRIORITY;
            }
        }
        return new BugInstance(this, pattern, priority);
    }

    private void scanForBooleanValue(int seen) {
        switch (seen) {

        case Const.IAND:
        case Const.IOR:
            switch (prevOpcode) {
            case Const.ILOAD:
            case Const.ILOAD_0:
            case Const.ILOAD_1:
            case Const.ILOAD_2:
            case Const.ILOAD_3:
                clearAll();
                break;
            default:
                break;
            }
            break;
        case Const.ICONST_1:
            stage1 = 1;
            switch (prevOpcode) {
            case Const.IFNONNULL:
            case Const.IFNULL:
                sawNullTest = true;
                break;
            case Const.IF_ICMPGT:
            case Const.IF_ICMPGE:
            case Const.IF_ICMPLT:
            case Const.IF_ICMPLE:
                sawNumericTest = true;
                break;
            }

            break;
        case Const.GOTO:
            if (stage1 == 1) {
                stage1 = 2;
            } else {
                stage1 = 0;
                clearAll();
            }
            break;
        case Const.ICONST_0:
            if (stage1 == 2) {
                sawBooleanValue();
            }
            stage1 = 0;
            break;
        case Const.INVOKEINTERFACE:
        case Const.INVOKEVIRTUAL:
        case Const.INVOKESPECIAL:
        case Const.INVOKESTATIC:
            String sig = getSigConstantOperand();
            if (sig.endsWith(")Z")) {
                sawBooleanValue();
            }
            stage1 = 0;
            break;
        default:
            stage1 = 0;
        }
    }

    private void sawBooleanValue() {
        sawMethodCallOld = sawMethodCall;
        sawDangerOld = sawDanger;
        sawArrayDangerOld = sawArrayDanger;
        sawNullTestVeryOld = sawNullTestOld;
        sawNullTestOld = sawNullTest;
        sawNumericTestVeryOld = sawNumericTestOld;
        sawNumericTestOld = sawNumericTest;
        sawNumericTest = false;
        sawDanger = false;
        sawArrayDanger = false;
        sawMethodCall = false;
        distance = 0;
        stage1 = 0;

    }
}
