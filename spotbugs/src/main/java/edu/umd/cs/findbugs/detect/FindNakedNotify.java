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

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.ba.XField;

//   2:   astore_1
//   3:   monitorenter
//   4:   aload_0
//   5:   invokevirtual   #13; //Method java/lang/Object.notify:()V
//   8:   aload_1
//   9:   monitorexit

public class FindNakedNotify extends BytecodeScanningDetector implements StatelessDetector {
    private Stage stage = Stage.START;

    private final BugReporter bugReporter;

    boolean synchronizedMethod;

    private int notifyPC;

    /**
     * True when this method performed a visible state update after the previous
     * synchronized region (or method entry) and before the current
     * {@code monitorenter}. Volatile field stores and ordinary method calls count;
     * plain non-volatile field stores do not (see {@code NakedWait}).
     */
    private boolean stateUpdatedBeforeSync;

    public FindNakedNotify(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visit(Method obj) {
        int flags = obj.getAccessFlags();
        synchronizedMethod = (flags & Const.ACC_SYNCHRONIZED) != 0;
    }

    @Override
    public void visit(Code obj) {
        stage = synchronizedMethod ? Stage.MONITOR_ENTERED : Stage.START;
        stateUpdatedBeforeSync = false;
        super.visit(obj);
        if (synchronizedMethod && stage == Stage.LOCK_LOADED) {
            reportIfNakedNotify();
        }
    }

    @Override
    public void sawOpcode(int seen) {
        if (stage == Stage.START) {
            notePossibleStateUpdate(seen);
        }

        switch (stage) {
        case START:
            if (seen == Const.MONITORENTER) {
                stage = Stage.MONITOR_ENTERED;
            }
            break;
        case MONITOR_ENTERED:
            if (isRegisterLoad() || seen == Const.GETSTATIC || seen == Const.GETFIELD) {
                stage = Stage.LOADED;
            }
            break;
        case LOADED:
            if (isRegisterLoad() || seen == Const.GETSTATIC || seen == Const.GETFIELD) {
                break;
            } else if (seen == Const.INVOKEVIRTUAL
                    && ("notify".equals(getNameConstantOperand()) || "notifyAll".equals(getNameConstantOperand()))
                    && "()V".equals(getSigConstantOperand())) {
                stage = Stage.NOTIFY_CALLED;
                notifyPC = getPC();
            } else {
                // Non-notify work inside the region: not naked; start over for later regions.
                abandonSyncRegion();
            }
            break;
        case NOTIFY_CALLED:
            stage = Stage.LOCK_LOADED;
            break;
        case LOCK_LOADED:
            if (seen == Const.MONITOREXIT) {
                reportIfNakedNotify();
                stage = Stage.MONITOR_EXITED;
            } else {
                abandonSyncRegion();
            }
            break;
        case MONITOR_EXITED:
            break;
        default:
            assert false;
        }

    }

    /**
     * Remember updates that can make a following notify meaningful even when they
     * occur outside the synchronized block (issue #3786).
     */
    private void notePossibleStateUpdate(int seen) {
        switch (seen) {
        case Const.PUTFIELD:
        case Const.PUTSTATIC:
            XField field = getXFieldOperand();
            if (field != null && field.isVolatile()) {
                stateUpdatedBeforeSync = true;
            }
            break;
        case Const.INVOKEVIRTUAL:
        case Const.INVOKEINTERFACE:
        case Const.INVOKESPECIAL:
        case Const.INVOKESTATIC:
            if (!isWaitOrNotifyName(getNameConstantOperand())) {
                stateUpdatedBeforeSync = true;
            }
            break;
        default:
            break;
        }
    }

    private static boolean isWaitOrNotifyName(String name) {
        return "wait".equals(name) || "notify".equals(name) || "notifyAll".equals(name);
    }

    private void reportIfNakedNotify() {
        if (!stateUpdatedBeforeSync) {
            bugReporter.reportBug(new BugInstance(this, "NN_NAKED_NOTIFY", NORMAL_PRIORITY).addClassAndMethod(this)
                    .addSourceLine(this, notifyPC));
        }
        // Finished with this synchronized region; later regions are independent.
        stateUpdatedBeforeSync = false;
    }

    private void abandonSyncRegion() {
        // Mutation (or other non-notify work) inside the region means this notify
        // path is not naked. Clear pre-sync state so a later pure-notify region
        // in the same method is still reported.
        stateUpdatedBeforeSync = false;
        stage = Stage.START;
    }

    private enum Stage {
        START,
        MONITOR_ENTERED,
        LOADED,
        NOTIFY_CALLED,
        LOCK_LOADED,
        MONITOR_EXITED
    }
}
