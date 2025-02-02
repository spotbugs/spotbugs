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

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

/**
 * Find comparisons involving values computed with bitwise operations whose
 * outcomes are fixed at compile time.
 *
 * @author Tom Truscott &lt;trt@unx.sas.com&gt;
 * @author Tagir Valeev
 */
public class IncompatMask extends OpcodeStackDetector {
    private final BugReporter bugReporter;

    private int bitop = -1;

    private boolean equality;

    private Number arg1, arg2;

    private Item bitresultItem;

    public IncompatMask(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    static int populationCount(long i) {
        return Long.bitCount(i);
    }

    @Override
    public void visit(Code obj) {
        arg1 = arg2 = null;
        super.visit(obj);
    }

    private Number getArg() {
        Object constValue = stack.getStackItem(0).getConstant();
        if (!(constValue instanceof Number)) {
            constValue = stack.getStackItem(1).getConstant();
        }
        if (!(constValue instanceof Long) && !(constValue instanceof Integer)) {
            return null;
        }
        return (Number) constValue;
    }

    @Override
    public void sawOpcode(int seen) {
        switch (seen) {
        case Const.IAND:
        case Const.LAND:
            arg1 = getArg();
            bitop = Const.IAND;
            return;
        case Const.IOR:
        case Const.LOR:
            arg1 = getArg();
            bitop = Const.IOR;
            return;
        case Const.LCMP:
            if (checkItem(2)) {
                arg2 = getArg();
            }
            return;
        case Const.IF_ICMPEQ:
        case Const.IF_ICMPNE:
            if (checkItem(2)) {
                arg2 = getArg();
                equality = true;
            }
            break;
        case Const.IFEQ:
        case Const.IFNE:
            if (arg1 instanceof Integer && checkItem(1)) {
                arg2 = 0;
            }
            equality = true;
            break;
        case Const.IFLE:
        case Const.IFLT:
        case Const.IFGT:
        case Const.IFGE:
            if (arg1 instanceof Integer && checkItem(1)) {
                arg2 = 0;
            }
            equality = false;
            break;
        default:
            return;
        }
        if (arg1 == null || arg2 == null) {
            return;
        }
        boolean isLong = arg1 instanceof Long;
        if (!equality && arg2.longValue() == 0) {
            long bits = getFlagBits(isLong, arg1.longValue());
            boolean highbit = !isLong && (bits & 0x80000000) != 0 || isLong && bits < 0 && bits << 1 == 0;
            boolean onlyLowBits = bits >>> 12 == 0;
            BugInstance bug;
            if (highbit) {
                bug = new BugInstance(this, "BIT_SIGNED_CHECK_HIGH_BIT", (seen == Const.IFLE || seen == Const.IFGT) ? HIGH_PRIORITY
                        : NORMAL_PRIORITY);
            } else {
                bug = new BugInstance(this, "BIT_SIGNED_CHECK", onlyLowBits ? LOW_PRIORITY : NORMAL_PRIORITY);
            }
            bug.addClassAndMethod(this).addString(toHex(arg1) + " (" + arg1 + ")").addSourceLine(this);
            bugReporter.reportBug(bug);
        }
        if (equality) {
            long dif;
            String t;

            long val1 = arg1.longValue();
            long val2 = arg2.longValue();

            if (bitop == Const.IOR) {
                dif = val1 & ~val2;
                t = "BIT_IOR";
            } else if (val1 != 0 || val2 != 0) {
                dif = val2 & ~val1;
                t = "BIT_AND";
            } else {
                dif = 1;
                t = "BIT_AND_ZZ";
            }
            if (dif != 0) {
                BugInstance bug = new BugInstance(this, t, HIGH_PRIORITY).addClassAndMethod(this);
                if (!"BIT_AND_ZZ".equals(t)) {
                    bug.addString(toHex(arg1)).addString(toHex(arg2));
                }
                bug.addSourceLine(this);
                bugReporter.reportBug(bug);
            }
        }
        arg1 = arg2 = null;
        bitresultItem = null;
    }

    private static String toHex(Number n) {
        return "0x" + (n instanceof Long ? Long.toHexString(n.longValue()) : Integer.toHexString(n.intValue()));
    }

    private boolean checkItem(int n) {
        if (bitresultItem != null) {
            for (int i = 0; i < n; i++) {
                if (stack.getStackItem(i) == bitresultItem) {
                    return true;
                }
            }
        }
        arg1 = arg2 = null;
        bitresultItem = null;
        return false;
    }

    @Override
    public void afterOpcode(int seen) {
        super.afterOpcode(seen);
        switch (seen) {
        case Const.IAND:
        case Const.LAND:
        case Const.IOR:
        case Const.LOR:
            if (stack.getStackDepth() > 0) {
                bitresultItem = stack.getStackItem(0);
            }
            break;
        default:
            break;
        }
    }

    static long getFlagBits(boolean isLong, long arg0) {
        long bits = arg0;
        if (isLong) {
            if (populationCount(bits) > populationCount(~bits)) {
                bits = ~bits;
            }
        } else if (populationCount(0xffffffffL & bits) > populationCount(0xffffffffL & ~bits)) {
            bits = 0xffffffffL & ~bits;
        }
        return bits;
    }
}
