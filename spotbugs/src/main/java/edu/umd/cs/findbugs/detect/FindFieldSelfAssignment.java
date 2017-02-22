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
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class FindFieldSelfAssignment extends OpcodeStackDetector implements StatelessDetector {
    private final BugReporter bugReporter;

    private static final boolean DEBUG = SystemProperties.getBoolean("fsa.debug");
    int state;

    public FindFieldSelfAssignment(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visit(Code obj) {
        state = 0;
        lastMethodCall = -1;

        if (DEBUG) {
            System.out.println(getXMethod());
        }
        super.visit(obj);
        possibleOverwrite = null;
        if (DEBUG) {
            System.out.println();
        }
        initializedFields.clear();
    }

    int register;

    int lastMethodCall;
    Set<String> initializedFields = new HashSet<String>();

    XField possibleOverwrite;
    @Override
    public void sawOpcode(int seen) {

        if (DEBUG) {
            System.out.printf("%5d %12s %s%n", getPC(), OPCODE_NAMES[seen],stack);
        }
        if (seen == PUTFIELD) {
            OpcodeStack.Item top = stack.getStackItem(0);
            OpcodeStack.Item next = stack.getStackItem(1);

            if (possibleOverwrite != null && possibleOverwrite.equals(getXFieldOperand())) {
                bugReporter.reportBug(new BugInstance(this, "SA_FIELD_SELF_ASSIGNMENT", Priorities.HIGH_PRIORITY).addClassAndMethod(this)
                        .addReferencedField(this).addSourceLine(this));

            }
            possibleOverwrite = null;

            if (stack.getStackDepth() >= 4 && getNextOpcode() == PUTFIELD) {
                OpcodeStack.Item third = stack.getStackItem(2);
                OpcodeStack.Item fourth = stack.getStackItem(3);
                XField f2 = third.getXField();
                int registerNumber2 = fourth.getRegisterNumber();
                if (f2 != null && f2.equals(getXFieldOperand()) && registerNumber2 >= 0
                        && registerNumber2 == third.getFieldLoadedFromRegister()
                        && !third.sameValue(top) && (third.getPC() == -1 || third.getPC() > lastMethodCall)) {
                    possibleOverwrite = f2;
                }
            }

            XField f = top.getXField();
            int registerNumber = next.getRegisterNumber();
            if (f != null && f.equals(getXFieldOperand()) && registerNumber >= 0
                    && registerNumber == top.getFieldLoadedFromRegister() && (top.getPC() == -1 || top.getPC() > lastMethodCall)) {
                int priority = NORMAL_PRIORITY;

                LocalVariableAnnotation possibleMatch = LocalVariableAnnotation.findMatchingIgnoredParameter(getClassContext(),
                        getMethod(), getNameConstantOperand(), getSigConstantOperand());
                if (possibleMatch != null) {
                    priority--;
                } else {
                    possibleMatch = LocalVariableAnnotation.findUniqueBestMatchingParameter(getClassContext(), getMethod(),
                            getNameConstantOperand(), getSigConstantOperand());
                }
                if (possibleMatch == null) {
                    String signature = stack.getLVValue(registerNumber).getSignature();
                    for (int i = 0; i < stack.getNumLocalValues(); i++) {
                        if (i != register) {
                            Item lvValue = stack.getLVValue(i);
                            if (lvValue != null && lvValue.getSignature().equals(signature)) {
                                priority--;
                                break;
                            }
                        }
                    }
                }

                bugReporter.reportBug(new BugInstance(this, "SA_FIELD_SELF_ASSIGNMENT", priority).addClassAndMethod(this)
                        .addReferencedField(this).addOptionalAnnotation(possibleMatch).addSourceLine(this));

            }
        } else {
            possibleOverwrite = null;
        }
        if (isMethodCall()) {
            lastMethodCall = getPC();
        }
        switch (state) {
        case 0:
            if (seen == DUP) {
                state = 6;
            }
            break;
        case 6:
            if (isRegisterStore()) {
                state = 7;
                register = getRegisterOperand();
            } else {
                state = 0;
            }
            break;
        case 7:
            if (isRegisterStore() && register == getRegisterOperand()) {
                bugReporter.reportBug(new BugInstance(this, "SA_LOCAL_DOUBLE_ASSIGNMENT", NORMAL_PRIORITY)
                .addClassAndMethod(this)
                .add(LocalVariableAnnotation.getLocalVariableAnnotation(getMethod(), register, getPC(), getPC() - 1))
                .addSourceLine(this));
            }
            state = 0;
            break;
        default:
            break;
        }

    }

}
