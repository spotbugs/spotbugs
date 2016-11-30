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

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class InfiniteRecursiveLoop extends OpcodeStackDetector implements StatelessDetector {

    private final BugReporter bugReporter;

    private boolean seenTransferOfControl;

    private boolean seenReturn;

    //    private boolean seenThrow;

    private boolean seenStateChange;

    private int largestBranchTarget;

    private final static boolean DEBUG = SystemProperties.getBoolean("irl.debug");

    public InfiniteRecursiveLoop(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visit(Method obj) {
        seenTransferOfControl = false;
        seenStateChange = false;
        seenReturn = false;
        //        seenThrow = false;
        largestBranchTarget = -1;

        if (DEBUG) {
            System.out.println();
            System.out.println(" --- " + getFullyQualifiedMethodName());
            System.out.println();
        }
    }

    @Override
    public void sawBranchTo(int target) {
        if (target == getNextPC()) {
            return;
        }
        if (largestBranchTarget < target) {
            largestBranchTarget = target;
        }
        seenTransferOfControl = true;
    }

    /**
     * Signal an infinite loop if either: we see a call to the same method with
     * the same parameters, or we see a call to the same (dynamically dispatched
     * method), and there has been no transfer of control.
     */
    @Override
    public void sawOpcode(int seen) {
        if (seenReturn && seenTransferOfControl && seenStateChange) {
            return;
        }

        if (DEBUG) {
            System.out.println(stack);
            System.out.println(getPC() + " : " + OPCODE_NAMES[seen]);
        }

        if ((seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE) && "add".equals(getNameConstantOperand())
                && "(Ljava/lang/Object;)Z".equals(getSigConstantOperand()) && stack.getStackDepth() >= 2) {
            OpcodeStack.Item it0 = stack.getStackItem(0);
            int r0 = it0.getRegisterNumber();
            OpcodeStack.Item it1 = stack.getStackItem(1);
            int r1 = it1.getRegisterNumber();
            if (r0 == r1 && r0 > 0) {
                bugReporter.reportBug(new BugInstance(this, "IL_CONTAINER_ADDED_TO_ITSELF", NORMAL_PRIORITY).addClassAndMethod(
                        this).addSourceLine(this));
            }
        }

        if ((seen == INVOKEVIRTUAL || seen == INVOKESPECIAL || seen == INVOKEINTERFACE || seen == INVOKESTATIC)
                && getNameConstantOperand().equals(getMethodName())
                && getSigConstantOperand().equals(getMethodSig())
                && (seen == INVOKESTATIC) == getMethod().isStatic()
                && (seen == INVOKESPECIAL) == (getMethod().isPrivate() && !getMethod().isStatic() || "<init>".equals(getMethodName()))) {
            Type arguments[] = getMethod().getArgumentTypes();
            // stack.getStackDepth() >= parameters
            int parameters = arguments.length;
            if (!getMethod().isStatic()) {
                parameters++;
            }
            XMethod xMethod = XFactory.createReferencedXMethod(this);
            if (DEBUG) {
                System.out.println("IL: Checking...");
                System.out.println(xMethod);
                System.out.println("vs. " + getClassName() + "." + getMethodName() + " : " + getMethodSig());

            }
            if (xMethod.getClassName().replace('.', '/').equals(getClassName()) || seen == INVOKEINTERFACE) {
                // Invocation of same method
                // Now need to see if parameters are the same
                int firstParameter = 0;
                if ("<init>".equals(getMethodName())) {
                    firstParameter = 1;
                }

                // match1 should be true if it is any call to the exact same
                // method
                // and no state has been change.
                // if match1 is true, the method may not always perform
                // a recursive infinite loop, but this particular method call is
                // an infinite
                // recursive loop

                boolean match1 = !seenStateChange;
                for (int i = firstParameter; match1 && i < parameters; i++) {
                    OpcodeStack.Item it = stack.getStackItem(parameters - 1 - i);
                    if (!it.isInitialParameter() || it.getRegisterNumber() != i) {
                        match1 = false;
                    }
                }

                boolean sameMethod = seen == INVOKESTATIC || "<init>".equals(getNameConstantOperand());
                if (!sameMethod) {
                    // Have to check if first parmeter is the same
                    // know there must be a this argument
                    if (DEBUG) {
                        System.out.println("Stack is " + stack);
                    }
                    OpcodeStack.Item p = stack.getStackItem(parameters - 1);
                    if (DEBUG) {
                        System.out.println("parameters = " + parameters + ", Item is " + p);
                    }
                    String sig = p.getSignature();
                    sameMethod = p.isInitialParameter() && p.getRegisterNumber() == 0 && sig.equals("L" + getClassName() + ";");

                }

                // match2 and match3 are two different ways of seeing if the
                // call site
                // postdominates the method entry.

                // technically, we use (!seenTransferOfControl || !seenReturn &&
                // largestBranchTarget < getPC())
                // as a check that the call site postdominates the method entry.
                // If those are true,
                // and sameMethod is true, we have a guaranteed IL.

                boolean match2 = sameMethod && !seenTransferOfControl;
                boolean match3 = sameMethod && !seenReturn && largestBranchTarget < getPC();
                if (match1 || match2 || match3) {
                    if (DEBUG) {
                        System.out.println("IL: " + sameMethod + " " + match1 + " " + match2 + " " + match3);
                    }
                    //                    int priority = HIGH_PRIORITY;
                    //                    if (!match1 && !match2 && seenThrow)
                    //                        priority = NORMAL_PRIORITY;
                    //                    if (seen == INVOKEINTERFACE)
                    //                        priority = NORMAL_PRIORITY;
                    bugReporter.reportBug(new BugInstance(this, "IL_INFINITE_RECURSIVE_LOOP", HIGH_PRIORITY).addClassAndMethod(
                            this).addSourceLine(this));
                }
            }
        }

        switch (seen) {
        case ARETURN:
        case IRETURN:
        case LRETURN:
        case RETURN:
        case DRETURN:
        case FRETURN:
            seenReturn = true;
            seenTransferOfControl = true;
            break;
        case ATHROW:
            //            seenThrow = true;
            seenTransferOfControl = true;
            break;
        case PUTSTATIC:
        case PUTFIELD:
        case IASTORE:
        case AASTORE:
        case DASTORE:
        case FASTORE:
        case LASTORE:
        case SASTORE:
        case CASTORE:
        case BASTORE:
            seenStateChange = true;
            break;
        case INVOKEVIRTUAL:
        case INVOKESPECIAL:
        case INVOKEINTERFACE:
        case INVOKESTATIC:
            if ("print".equals(getNameConstantOperand()) || "println".equals(getNameConstantOperand())
                    || "log".equals(getNameConstantOperand()) || "toString".equals(getNameConstantOperand())) {
                break;
            }
            seenStateChange = true;
            break;
        default:
            break;
        }
    }

}
