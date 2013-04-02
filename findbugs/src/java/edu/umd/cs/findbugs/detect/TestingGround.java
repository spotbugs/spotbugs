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

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class TestingGround extends OpcodeStackDetector {

    final BugReporter bugReporter;

    final BugAccumulator accumulator;

    public TestingGround(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.accumulator = new BugAccumulator(bugReporter);
    }

    enum State {
        START, SAW_INIT, FOUND, NO, RETURNED;
    }

    State state;

    @Override
    public void visit(Code code) {
        boolean interesting = getMethod().isPublic() && !getMethodSig().startsWith("()");
        if (interesting) {
            state = State.START;
            // initialize any variables we want to initialize for the method
//            System.out.println(getFullyQualifiedMethodName());
            super.visit(code); // make callbacks to sawOpcode for all opcodes
//            System.out.println();
            if (state == State.RETURNED)
                bugReporter.reportBug(new BugInstance(this, "TESTING", NORMAL_PRIORITY).addClassAndMethod(this));

        }
        accumulator.reportAccumulatedBugs();
    }

    @Override
    public void sawOpcode(int seen) {
//        System.out.printf("%6s %3d %s%n", state, getPC(), OPCODE_NAMES[seen]);
        switch (seen) {
        case INVOKESPECIAL:
            if (getNameConstantOperand().equals("<init>") && state == State.START)
                state = State.SAW_INIT;
            else
                state = State.NO;
            break;
        case INVOKESTATIC:
            if (state == State.SAW_INIT && getNameConstantOperand().equals("doPrivileged")
                    && getClassConstantOperand().equals("java/security/AccessController")) {
                state = State.FOUND;

            } else
                state = State.NO;
            break;

        case INVOKEVIRTUAL:
        case INVOKEINTERFACE:
            state = state.NO;
            break;
        case IRETURN:
        case ARETURN:
        case RETURN:
            if (state == State.FOUND)
                state = State.RETURNED;
            else state = State.NO;

        }

    }

}
