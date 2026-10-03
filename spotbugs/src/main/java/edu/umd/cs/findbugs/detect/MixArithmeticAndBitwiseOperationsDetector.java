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


import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.*;

@OpcodeStack.CustomUserValue
public class MixArithmeticAndBitwiseOperationsDetector extends OpcodeStackDetector {

    private final BugAccumulator bugAccumulator;

    private Character saveContext = null;

    public MixArithmeticAndBitwiseOperationsDetector(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visitAfter(JavaClass obj) {
        bugAccumulator.reportAccumulatedBugs();
    }

    private void checkManipulation(int seen) {
        if (seen == Const.IINC) {
            int slot = getRegisterOperand();
            OpcodeStack.Item lv = stack.getLVValue(slot);
            Character t = (Character) lv.getUserValue();
            if (t != null && t == 'B') {
                bugAccumulator.accumulateBug(new BugInstance(this, "MABO_MIXING_ARITHMETIC_AND_BITWISE_OPERATIONS", NORMAL_PRIORITY)
                        .addClassAndMethod(this).addString("arithmetic").addString("bitwise").addSourceLine(this),
                        this);
            }
        } else if (isArithmeticOpcode(seen) || isBitwiseOpcode(seen)) {

            int start = 1, end = 2, depth = 2;
            if (isBitwiseShiftOpcode(seen)) {
                start = 2;
            } else if (isUnaryArithmeticOpcode(seen)) {
                end = 1;
                depth = 1;
            }

            Character type = isBitwiseOpcode(seen) ? 'B' : 'A';

            if (stack.getStackDepth() < depth) {
                return;
            }

            for (int i = start; i <= end; i++) {
                OpcodeStack.Item item = stack.getStackItem(i - 1);
                if (!checkItem(item, type)) {
                    bugAccumulator.accumulateBug(new BugInstance(this, "MABO_MIXING_ARITHMETIC_AND_BITWISE_OPERATIONS", NORMAL_PRIORITY)
                            .addClassAndMethod(this).addString(type.equals('B') ? "bitwise" : "arithmetic")
                            .addString(type.equals('A') ? "bitwise" : "arithmetic").addSourceLine(this),
                            this);
                }
            }
        }
    }

    @Override
    public void sawOpcode(int seen) {
        if (seen == Const.IINC || isArithmeticOpcode(seen) || isBitwiseOpcode(seen)) {
            checkManipulation(seen);
        } else if (isCastOpcode(seen)) {
            if (stack.getStackDepth() < 1) {
                return;
            }
            saveContext = (Character) stack.getStackItem(0).getUserValue();
        }

    }

    @Override
    public void afterOpcode(int seen) {
        super.afterOpcode(seen);

        if (seen == Const.IINC) {
            int slot = getRegisterOperand();
            OpcodeStack.Item lv = stack.getLVValue(slot);
            lv.setUserValue('A');
        } else if (isArithmeticOpcode(seen)
                || isBitwiseOpcode(seen)) {
            if (stack.getStackDepth() < 1) {
                return;
            }
            OpcodeStack.Item item = stack.getStackItem(0);
            if (isArithmeticOpcode(seen)) {
                item.setUserValue('A');
            } else if (isBitwiseOpcode(seen)) {
                item.setUserValue('B');
            }
        } else if (isCastOpcode(seen)) {
            if (saveContext != null && stack.getStackDepth() > 0) {
                stack.getStackItem(0).setUserValue(saveContext);
            }
            saveContext = null;
        }

    }

    private boolean isCastOpcode(int seen) {
        switch (seen) {
        case Const.I2B:
        case Const.I2L:
        case Const.I2S:
        case Const.L2I:
            return true;
        default:
            return false;
        }
    }

    private boolean isUnaryArithmeticOpcode(int seen) {
        switch (seen) {
        case Const.INEG:
        case Const.LNEG:
            return true;
        default:
            return false;
        }
    }

    private boolean isBinaryArithmeticOpcode(int seen) {
        switch (seen) {
        case Const.IADD:
        case Const.ISUB:
        case Const.IMUL:
        case Const.IDIV:
        case Const.IREM:
        case Const.LADD:
        case Const.LSUB:
        case Const.LMUL:
        case Const.LDIV:
        case Const.LREM:
            return true;
        default:
            return false;
        }
    }

    private boolean isBitwiseLogicalOpcode(int seen) {
        switch (seen) {
        case Const.IOR:
        case Const.IXOR:
        case Const.IAND:
        case Const.LOR:
        case Const.LXOR:
        case Const.LAND:
            return true;
        default:
            return false;
        }
    }

    private boolean isBitwiseShiftOpcode(int seen) {
        switch (seen) {
        case Const.ISHL:
        case Const.ISHR:
        case Const.IUSHR:
        case Const.LSHL:
        case Const.LSHR:
        case Const.LUSHR:
            return true;
        default:
            return false;
        }
    }

    private boolean isArithmeticOpcode(int seen) {
        return isUnaryArithmeticOpcode(seen) || isBinaryArithmeticOpcode(seen);
    }

    private boolean isBitwiseOpcode(int seen) {
        return isBitwiseLogicalOpcode(seen) || isBitwiseShiftOpcode(seen);
    }

    private boolean checkItem(OpcodeStack.Item item, Character expectedType) {
        Character itemType = (Character) item.getUserValue();

        if (itemType == null) {
            item.setUserValue(expectedType);
            return true;
        }

        return expectedType.equals(itemType);
    }

}
