/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2006, University of Maryland
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

import java.util.Collections;

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

/**
 * if we get from a ConcurrentHashMap and assign to a variable... and don't do
 * anything else and perform a null check on it... and then do a set on it...
 * (or anything else inside the if that modifies it?) then we have a bug.
 *
 * @author Michael Midgley-Biggs
 */
public class AtomicityProblem extends OpcodeStackDetector {

    int priority = IGNORE_PRIORITY;

    int lastQuestionableCheckTarget = -1;

    private final BugReporter bugReporter;

    final static boolean DEBUG = false;

    public AtomicityProblem(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        if(hasInterestingClass(classContext.getJavaClass().getConstantPool(), Collections.singleton("java/util/concurrent/ConcurrentHashMap"))) {
            super.visitClassContext(classContext);
        }
    }

    @Override
    public void visit(Code obj) {
        if (DEBUG) {
            System.out.println("Checking " + obj);
        }
        lastQuestionableCheckTarget = -1;
        super.visit(obj);
    }

    /**
     * This is the "dumb" version of the detector. It may generate false
     * positives, and/or not detect all instances of the bug.
     *
     * @see edu.umd.cs.findbugs.visitclass.DismantleBytecode#sawOpcode(int)
     */
    @Override
    public void sawOpcode(int seen) {
        if (DEBUG) {
            System.out.println(getPC() + " " + OPCODE_NAMES[seen]);
        }
        switch (seen) {
        case IFNE:
        case IFEQ: {
            OpcodeStack.Item top = stack.getStackItem(0);
            if (DEBUG) {
                System.out.println("Stack top: " + top);
            }
            XMethod m = top.getReturnValueOf();
            if (m != null && "java.util.concurrent.ConcurrentHashMap".equals(m.getClassName())
                    && "containsKey".equals(m.getName())) {
                lastQuestionableCheckTarget = getBranchTarget();
                if (seen == IFEQ) {
                    priority = LOW_PRIORITY;
                } else if (seen == IFNE) {
                    priority = NORMAL_PRIORITY;
                }
            }
            break;
        }
        case IFNULL:
        case IFNONNULL: {
            OpcodeStack.Item top = stack.getStackItem(0);
            if (DEBUG) {
                System.out.println("Stack top: " + top);
            }
            XMethod m = top.getReturnValueOf();
            if (DEBUG) {
                System.out.println("Found null check");
            }
            if (m != null && "java.util.concurrent.ConcurrentHashMap".equals(m.getClassName()) && "get".equals(m.getName())) {
                lastQuestionableCheckTarget = getBranchTarget();
                if (seen == IFNULL) {
                    priority = LOW_PRIORITY;
                } else if (seen == IFNONNULL) {
                    priority = NORMAL_PRIORITY;
                }
            }
            break;
        }
        case INVOKEVIRTUAL:
        case INVOKEINTERFACE: {
            if ("java.util.concurrent.ConcurrentHashMap".equals(getDottedClassConstantOperand())) {
                String methodName = getNameConstantOperand();
                XClass xClass = getXClassOperand();
                if (xClass != null && "put".equals(methodName)) {
                    if ((getPC() < lastQuestionableCheckTarget) && (lastQuestionableCheckTarget != -1)) {
                        bugReporter.reportBug(new BugInstance(this, "AT_OPERATION_SEQUENCE_ON_CONCURRENT_ABSTRACTION", priority)
                        .addClassAndMethod(this).addType(xClass.getClassDescriptor()).addCalledMethod(this)
                        .addSourceLine(this));
                    }
                }
            }
            break;
        }
        default:
            break;
        }
    }
}
