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

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.ba.Hierarchy;

public class FindRunInvocations extends BytecodeScanningDetector implements StatelessDetector {

    private final BugReporter bugReporter;

    private final BugAccumulator bugAccumulator;

    private boolean alreadySawStart;

    public FindRunInvocations(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    private boolean isThread(String clazz) {
        try {
            return Hierarchy.isSubtype(clazz, "java.lang.Thread");
        } catch (ClassNotFoundException e) {
            bugReporter.reportMissingClass(e);
            return false;
        }
    }

    @Override
    public void visit(Code obj) {
        alreadySawStart = false;
        super.visit(obj);
        bugAccumulator.reportAccumulatedBugs();
    }

    @Override
    public void sawOpcode(int seen) {
        if (alreadySawStart) {
            return;
        }
        if ((seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE) && "()V".equals(getSigConstantOperand())
                && isThread(getDottedClassConstantOperand())) {
            if ("start".equals(getNameConstantOperand())) {
                alreadySawStart = true;
            } else {
                boolean isJustThread = !"java.lang.Thread".equals(getDottedClassConstantOperand());
                if (amVisitingMainMethod() && getPC() == getCode().getLength() - 4 && isJustThread) {
                    return;
                } else if ("run".equals(getNameConstantOperand())) {
                    bugAccumulator.accumulateBug(new BugInstance(this, "RU_INVOKE_RUN", isJustThread ? HIGH_PRIORITY
                            : NORMAL_PRIORITY).addClassAndMethod(this), this);
                }
            }
        }
    }
}
