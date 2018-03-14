/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2017-2018 Public
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

import java.util.Arrays;
import java.util.List;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class FindLockOnLocal extends OpcodeStackDetector {
    private BugReporter bugReporter;
    private List<String> lockMethods = Arrays.asList(
            new String[] { "java.util.concurrent.locks.Lock.lock", "java.util.concurrent.locks.ReentrantLock.lock",
                    "java.util.concurrent.locks.ReentrantReadWriteLock.readLock",
                    "java.util.concurrent.locks.ReentrantReadWriteLock.writeLock" });

    public FindLockOnLocal(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    /*
     * Just called once one class file.
     */
    @Override
    public void visit(Code obj) {
        super.visit(obj);
    }

    /**
     * @param seen
     *            One op code of jvm
     */
    @Override
    public void sawOpcode(int seen) {
        boolean isBug = false;
        if (seen == Const.MONITORENTER) {
            isBug = checkLocal(stack.getStackItem(0));
        } else if (seen == Const.INVOKEINTERFACE || seen == Const.INVOKEVIRTUAL) {
            if (isLockFunction()) {
                isBug = checkLocal(stack.getStackItem(0));
            }
        }

        if (isBug) {
            BugInstance bug = new BugInstance(this, "SPEC_LOCK_ON_LOCAL", HIGH_PRIORITY).addClassAndMethod(this)
                    .addSourceLine(this, getPC());
            bugReporter.reportBug(bug);
        }
    }

    private boolean isLockFunction() {
        boolean isLock = false;
        XMethod method = getXMethodOperand();
        if (null != method) {
            String methodName = method.getName();
            String className = method.getClassName();
            if (null != methodName && null != className) {
                isLock = lockMethods.contains(className + "." + methodName);
            }
        }

        return isLock;
    }

    private static boolean checkLocal(Item top) {
        return top.isNewlyAllocated();
    }
}
