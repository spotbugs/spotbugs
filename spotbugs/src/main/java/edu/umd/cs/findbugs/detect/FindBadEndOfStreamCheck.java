/*
 * SpotBugs - Find bugs in Java programs
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
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class FindBadEndOfStreamCheck extends OpcodeStackDetector {

    private final BugAccumulator bugAccumulator;
    private OpcodeStack.Item itemUnderCast;
    private OpcodeStack.Item castedItem;
    private XMethod source;

    public FindBadEndOfStreamCheck(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visitAfter(JavaClass obj) {
        bugAccumulator.reportAccumulatedBugs();
    }

    @Override
    public void sawOpcode(int seen) {
        itemUnderCast = null;

        if (seen == Const.I2B || seen == Const.I2C) {
            OpcodeStack.Item item = stack.getStackItem(0);
            XMethod method = item.getReturnValueOf();
            if (method == null || !isFileRead(method)) {
                return;
            }
            itemUnderCast = item;
            source = method;
        }

        if ((seen == Const.IF_ICMPEQ || seen == Const.IF_ICMPNE) && castedItem != null) {
            OpcodeStack.Item rightItem = stack.getStackItem(0);
            OpcodeStack.Item leftItem = stack.getStackItem(1);
            Object value = null;
            if (leftItem.equals(castedItem)) {
                value = rightItem.getConstant();
            } else if (rightItem.equals(castedItem)) {
                value = leftItem.getConstant();
            }
            if (value instanceof Integer && ((Integer) value).intValue() == -1) {
                bugAccumulator.accumulateBug(new BugInstance(this, "EOS_BAD_END_OF_STREAM_CHECK", NORMAL_PRIORITY)
                        .addClassAndMethod(this).addCalledMethod(source)
                        .addString(castedItem.getSignature().equals("B") ? "byte" : "char").addInt(-1), this);
            }
        }

        if ((seen == Const.IFGE || seen == Const.IFLT) && castedItem != null) {
            OpcodeStack.Item item = stack.getStackItem(0);
            if (item.equals(castedItem)) {
                bugAccumulator.accumulateBug(new BugInstance(this, "EOS_BAD_END_OF_STREAM_CHECK", NORMAL_PRIORITY)
                        .addClassAndMethod(this).addCalledMethod(source)
                        .addString(castedItem.getSignature().equals("B") ? "byte" : "char").addInt(0), this);
            }
        }

        if ((seen == Const.IF_ICMPLE || seen == Const.IF_ICMPGT) && castedItem != null) {
            OpcodeStack.Item rightItem = stack.getStackItem(0);
            OpcodeStack.Item leftItem = stack.getStackItem(1);
            Object value = null;
            if (rightItem.equals(castedItem)) {
                value = leftItem.getConstant();
            }
            if (value instanceof Integer && ((Integer) value).intValue() == 0) {
                bugAccumulator.accumulateBug(new BugInstance(this, "EOS_BAD_END_OF_STREAM_CHECK", NORMAL_PRIORITY)
                        .addClassAndMethod(this).addCalledMethod(source)
                        .addString(castedItem.getSignature().equals("B") ? "byte" : "char").addInt(0), this);
            }
        }
    }

    @Override
    public void afterOpcode(int seen) {
        super.afterOpcode(seen);

        if ((seen == Const.I2B || seen == Const.I2C) && itemUnderCast != null) {
            castedItem = stack.getStackItem(0);
        }
    }

    private boolean isFileRead(XMethod method) {
        String classSig = method.getClassDescriptor().getSignature();
        return method != null && "read".equals(method.getName())
                && ("Ljava/io/FileInputStream;".equals(classSig) || "Ljava/io/FileReader;".equals(classSig));
    }
}
