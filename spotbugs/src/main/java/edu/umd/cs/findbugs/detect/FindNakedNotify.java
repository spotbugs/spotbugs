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

    private boolean stateMutatedInSync;

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
        stateMutatedInSync = false;
        super.visit(obj);
        if (synchronizedMethod && stage == Stage.LOCK_LOADED) {
            reportNakedNotifyIfNeeded();
        }
    }

    private void reportNakedNotifyIfNeeded() {
        if (!stateMutatedInSync) {
            bugReporter.reportBug(new BugInstance(this, "NN_NAKED_NOTIFY", NORMAL_PRIORITY).addClassAndMethod(this)
                    .addSourceLine(this, notifyPC));
        }
    }

    private void noteStateChangeInSync(int seen) {
        if ((stage == Stage.MONITOR_ENTERED || stage == Stage.LOADED) && isStateMutatingInstruction(seen)) {
            stateMutatedInSync = true;
            stage = Stage.START;
        }
    }

    private boolean isStateMutatingInstruction(int seen) {
        switch (seen) {
        case Const.PUTFIELD:
        case Const.PUTSTATIC:
        case Const.IINC:
            return true;
        case Const.INVOKEVIRTUAL:
        case Const.INVOKEINTERFACE:
        case Const.INVOKESPECIAL:
        case Const.INVOKESTATIC:
            return !isBenignSyncInvoke(seen);
        default:
            return false;
        }
    }

    private boolean isBenignSyncInvoke(int seen) {
        if (isWaitOrNotifyMethod()) {
            return true;
        }
        String className = getClassConstantOperand();
        return className.startsWith("java/io/") || "java/lang/System".equals(className);
    }

    private boolean isWaitOrNotifyMethod() {
        String name = getNameConstantOperand();
        return "notify".equals(name) || "notifyAll".equals(name) || "wait".equals(name);
    }

    @Override
    public void sawOpcode(int seen) {
        noteStateChangeInSync(seen);
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
            } else if (isStateMutatingInstruction(seen)) {
                stateMutatedInSync = true;
                stage = Stage.START;
            }
            break;
        case NOTIFY_CALLED:
            stage = Stage.LOCK_LOADED;
            break;
        case LOCK_LOADED:
            if (seen == Const.MONITOREXIT) {
                reportNakedNotifyIfNeeded();
                stage = Stage.MONITOR_EXITED;
            } else {
                stage = Stage.START;
            }
            break;
        case MONITOR_EXITED:
            break;
        default:
            assert false;
        }

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
