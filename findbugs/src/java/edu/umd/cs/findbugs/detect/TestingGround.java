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

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class TestingGround extends OpcodeStackDetector {

    final BugReporter bugReporter;

    final BugAccumulator accumulator;

    public TestingGround(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.accumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visit(Code code) {
        boolean interesting = true;
        if (interesting) {
            // initialize any variables we want to initialize for the method
            super.visit(code); // make callbacks to sawOpcode for all opcodes
        }
        accumulator.reportAccumulatedBugs();
    }

    @Override
    public void sawOpcode(int seen) {
        switch (seen) {
        case Constants.IF_ICMPEQ:
        case Constants.IF_ICMPNE:
            OpcodeStack.Item left = stack.getStackItem(1);
            OpcodeStack.Item right = stack.getStackItem(0);
            if (bad(left, right) || bad(right, left))

                accumulator.accumulateBug(new BugInstance(this, "TESTING", NORMAL_PRIORITY).addClassAndMethod(this)
                        .addValueSource(left, this).addValueSource(right, this), this);
        }

    }

    private boolean bad(Item left, Item right) {
        XMethod m = left.getReturnValueOf();

        if (m == null)
            return false;
        Object value = right.getConstant();
        if (!(value instanceof Integer) || ((Integer) value).intValue() == 0)
            return false;
        if (m.isStatic() || !m.isPublic())
            return false;

        if (m.getName().equals("compareTo") && m.getSignature().equals("(Ljava/lang/Object;)I"))
            return true;
        if (m.getName().equals("compare") && m.getSignature().equals("(Ljava/lang/Object;Ljava/lang/Object;)I"))
            return true;

        return false;

    }

}
