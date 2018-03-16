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
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;

public class CheckLogParamNum extends OpcodeStackDetector {
    private final BugReporter bugReporter;

    /** The last AASTORE value before INVOKEINTERFACE, is the last parameter if the method is varArgs. */
    private OpcodeStack.Item lastAastoreItem = null;

    private final List<String> loggerMethods = Arrays
            .asList(new String[] { "error", "warn", "info", "debug", "trace" });

    public CheckLogParamNum(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    /*
     * Just called once one class file.
     */
    @Override
    public void visit(Code obj) {
        lastAastoreItem = null;
        super.visit(obj);
        lastAastoreItem = null;
    }

    /**
     * @param seen
     *            One op code of jvm
     */
    @Override
    public void sawOpcode(int seen) {
        if (seen == Const.AASTORE) {
            lastAastoreItem = stack.getStackItem(0);
        }

        if (seen == Const.INVOKEINTERFACE) {
            String className = getClassConstantOperand();
            if ("org/slf4j/Logger".equals(className)) {
                MethodDescriptor method = getMethodDescriptorOperand();
                if ((method != null) && (loggerMethods.contains(method.getName()))) {
                    BugInstance bug = checkPlaceholder();
                    if (null != bug) {
                        bug.addString(method.getName());
                        bug.addSourceLine(this, getPC());
                        bugReporter.reportBug(bug);
                    }
                }
            }
            lastAastoreItem = null;
        }
    }

    /**
     * Check if placeholder correct when call log.[info|error|warn|debug|trace].
     *
     * @return When the first parameter is a formatted string, and the number of brace in it does not match the number
     *         of parameters, it returns a bug, or else return null
     */
    private BugInstance checkPlaceholder() {
        /*
         * stack item(depth-1) is logger, item(depth-2) is formatter string; if no enough parameters, return null;
         */
        int depth = stack.getStackDepth();
        if (depth < 2) {
            return null;
        }

        /* If item(depth-2) is not an constant string, return null */
        Object formatStr = stack.getStackItem(depth - 2).getConstant();
        if (!(formatStr instanceof String)) {
            return null;
        }

        int placeholderCount = countStr((String) formatStr, "{}");
        boolean lastThrowable = false;
        int needCount = 0;

        if (depth == 2) {
            /* If only have formatter string, no formatter parameter, needCount is 0. */
            needCount = 0;
        } else {
            /* If have one or more formatter parameters, the parameters may be var args */
            XMethod m = getXMethodOperand();
            if (m != null && m.isVarArgs()) {
                /* parameter is varArgs, need placeholder count is varArgs.len, except last Throwable */
                OpcodeStack.Item varArgsItem = stack.getStackItem(0);
                needCount = (Integer) (varArgsItem.getConstant());
                if (isThrowable(lastAastoreItem)) {
                    lastThrowable = true;
                    needCount--;
                }
            } else {
                /* Need placeholder count is parameter number, except last Throwable */
                needCount = depth - 2;
                OpcodeStack.Item param = stack.getStackItem(0);
                if (isThrowable(param)) {
                    needCount--;
                    lastThrowable = true;
                }
            }
        }

        if (needCount == placeholderCount) {
            return null;
        }

        int confidence = HIGH_PRIORITY;
        if (lastThrowable && (placeholderCount == (needCount + 1))) {
            confidence = NORMAL_PRIORITY;
        }

        BugInstance bug = new BugInstance(this, "FS_LOG_PARAM_NUM", confidence).addClassAndMethod(this)
                .addInt(needCount);
        return bug;
    }

    /**
     * @param myClass
     * @return
     */
    private static boolean isThrowable(OpcodeStack.Item param) {
        if (null == param) {
            return false;
        }

        JavaClass myClass;
        try {
            myClass = param.getJavaClass();
        } catch (ClassNotFoundException e) {
            myClass = null;
        }

        if (null == myClass) {
            return false;
        }
        if ("java.lang.Throwable".equals(myClass.getClassName())) {
            return true;
        }

        JavaClass[] superClasses;
        try {
            superClasses = myClass.getSuperClasses();
        } catch (ClassNotFoundException e) {
            return false;
        }

        for (final JavaClass oneSuper : superClasses) {
            if ("java.lang.Throwable".equals(oneSuper.getClassName())) {
                return true;
            }
        }

        return false;
    }

    private static int countStr(String src, String sub) {
        int counter = 0;
        int index = -1;
        while ((index = src.indexOf(sub)) != -1) {
            counter++;
            src = src.substring(index + sub.length());
        }
        return counter;
    }

}
