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

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.StatelessDetector;

public class WaitInLoop extends BytecodeScanningDetector implements StatelessDetector {

    boolean sawWait = false;

    boolean sawAwait = false;

    boolean waitHasTimeout = false;

    boolean sawNotify = false;

    int notifyPC;

    int earliestJump = 0;

    int waitAt = 0;

    private final BugReporter bugReporter;

    public WaitInLoop(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visit(Code obj) {
        sawWait = false;
        sawAwait = false;
        waitHasTimeout = false;
        sawNotify = false;
        earliestJump = 9999999;
        super.visit(obj);
        if ((sawWait || sawAwait) && waitAt < earliestJump) {
            String bugType = sawWait ? "WA_NOT_IN_LOOP" : "WA_AWAIT_NOT_IN_LOOP";
            bugReporter.reportBug(new BugInstance(this, bugType, waitHasTimeout ? LOW_PRIORITY : NORMAL_PRIORITY)
            .addClassAndMethod(this).addSourceLine(this, waitAt));
        }
        if (sawNotify) {
            bugReporter.reportBug(new BugInstance(this, "NO_NOTIFY_NOT_NOTIFYALL", LOW_PRIORITY).addClassAndMethod(this)
                    .addSourceLine(this, notifyPC));
        }
    }

    @Override
    public void sawOpcode(int seen) {

        if ((seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE) && "notify".equals(getNameConstantOperand())
                && "()V".equals(getSigConstantOperand())) {
            sawNotify = true;
            notifyPC = getPC();
        }
        if (!(sawWait || sawAwait) && (seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE)
                && (isMonitorWait() || isConditionAwait())) {

            if ("wait".equals(getNameConstantOperand())) {
                sawWait = true;
            } else {
                sawAwait = true;
            }
            waitHasTimeout = !"()V".equals(getSigConstantOperand());
            waitAt = getPC();
            earliestJump = getPC() + 1;
            return;
        }
        if (seen >= IFEQ && seen <= GOTO || seen >= IFNULL && seen <= GOTO_W) {
            earliestJump = Math.min(earliestJump, getBranchTarget());
        }
    }

    private boolean isConditionAwait() {
        String className = getClassConstantOperand();
        String name = getNameConstantOperand();
        String sig = getSigConstantOperand();

        if (!"java/util/concurrent/locks/Condition".equals(className)) {
            return false;
        }

        if (!name.startsWith("await")) {
            return false;
        }

        if ("await".equals(name) && ("()V".equals(sig) || "(JLjava/util/concurrent/TimeUnit;)V".equals(sig))) {
            return true;
        }
        if ("awaitNanos".equals(name) && "(J)V".equals(sig)) {
            return true;
        }
        if ("awaitUninterruptibly".equals(name) && "()V".equals(sig)) {
            return true;
        }
        if ("awaitUntil".equals(name) && "(Ljava/util/Date;)V".equals(sig)) {
            return true;
        }

        return false;
    }

    private boolean isMonitorWait() {
        String name = getNameConstantOperand();
        String sig = getSigConstantOperand();

        return "wait".equals(name) && ("()V".equals(sig) || "(J)V".equals(sig) || "(JI)V".equals(sig));
    }

}
