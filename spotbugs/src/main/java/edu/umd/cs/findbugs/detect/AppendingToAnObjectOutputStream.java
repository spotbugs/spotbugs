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

import java.util.Collections;

import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class AppendingToAnObjectOutputStream extends OpcodeStackDetector {

    BugReporter bugReporter;

    public AppendingToAnObjectOutputStream(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        if(hasInterestingClass(classContext.getJavaClass().getConstantPool(), Collections.singleton("java/io/ObjectOutputStream"))) {
            super.visitClassContext(classContext);
        }
    }

    boolean sawOpenInAppendMode;

    @Override
    public void visit(Method obj) {
        sawOpenInAppendMode = false;
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.bcel.OpcodeStackDetector#sawOpcode(int)
     */
    @Override
    public void sawOpcode(int seen) {
        if (seen != INVOKESPECIAL) {
            sawOpenInAppendMode = false;
            return;
        }
        String calledClassName = getClassConstantOperand();
        String calledMethodName = getNameConstantOperand();
        String calledMethodSig = getSigConstantOperand();
        if (!sawOpenInAppendMode) {
            if ("java/io/ObjectOutputStream".equals(calledClassName) && "<init>".equals(calledMethodName)
                    && "(Ljava/io/OutputStream;)V".equals(calledMethodSig)
                    && stack.getStackItem(0).getSpecialKind() == OpcodeStack.Item.FILE_OPENED_IN_APPEND_MODE) {
                bugReporter.reportBug(new BugInstance(this, "IO_APPENDING_TO_OBJECT_OUTPUT_STREAM", Priorities.HIGH_PRIORITY)
                .addClassAndMethod(this).addSourceLine(this));
            }
            return;
        }
        if ("java/io/FileOutputStream".equals(calledClassName) && "<init>".equals(calledMethodName)
                && ("(Ljava/io/File;Z)V".equals(calledMethodSig) || "(Ljava/lang/String;Z)V".equals(calledMethodSig))) {
            OpcodeStack.Item item = stack.getStackItem(0);
            Object value = item.getConstant();
            sawOpenInAppendMode = value instanceof Integer && ((Integer) value).intValue() == 1;
        } else if (!sawOpenInAppendMode) {
            return;
        } else if ("java/io/BufferedOutputStream".equals(calledClassName) && "<init>".equals(calledMethodName)
                && "(Ljava/io/OutputStream;)V".equals(calledMethodSig)) {
            // do nothing

        } else if ("java/io/ObjectOutputStream".equals(calledClassName) && "<init>".equals(calledMethodName)
                && "(Ljava/io/OutputStream;)V".equals(calledMethodSig)) {
            bugReporter.reportBug(new BugInstance(this, "IO_APPENDING_TO_OBJECT_OUTPUT_STREAM", Priorities.HIGH_PRIORITY)
            .addClassAndMethod(this).addSourceLine(this));
            sawOpenInAppendMode = false;
        } else {
            sawOpenInAppendMode = false;
        }

    }

}
