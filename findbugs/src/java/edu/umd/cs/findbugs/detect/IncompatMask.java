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

import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.StatelessDetector;

/**
 * Find comparisons involving values computed with bitwise operations whose
 * outcomes are fixed at compile time.
 *
 * @author Tom Truscott <trt@unx.sas.com>
 */
public class IncompatMask extends BytecodeScanningDetector implements StatelessDetector {
    int state;

    long arg0, arg1;

    int bitop;

    boolean isLong;

    private final BugReporter bugReporter;

    public IncompatMask(BugReporter bugReporter) {
        this.state = 0;
        this.bugReporter = bugReporter;
    }

    @Override
    public void visit(Method obj) {
        super.visit(obj);
        this.state = 0;
    }

    private void checkState(int expectedState) {
        if (state == expectedState) {
            state++;
        } else {
            state = 0;
        }
    }

    private void noteVal(long val) {
        if (state == 0) {
            arg0 = val;
        } else if (state == 2) {
            arg1 = val;
        } else {
            state = -1;
        }
        state++;
    }

    @Override
    public void sawInt(int val) {
        noteVal(val);
    }

    @Override
    public void sawLong(long val) {
        noteVal(val);
    }

    static int populationCount(long i) {
        int result = 0;
        while (i != 0) {
            if ((i & 1) == 1) {
                result++;
            }
            i >>>= 1;
        }
        return result;
    }

    @Override
    public void sawOpcode(int seen) {
        // System.out.println("BIT: " + state + ": " + OPCODE_NAMES[seen]);

        switch (seen) {
        case ICONST_M1:
            noteVal(-1);
            return;
        case ICONST_0:
            noteVal(0);
            return;
        case ICONST_1:
            noteVal(1);
            return;
        case ICONST_2:
            noteVal(2);
            return;
        case ICONST_3:
            noteVal(3);
            return;
        case ICONST_4:
            noteVal(4);
            return;
        case ICONST_5:
            noteVal(5);
            return;
        case LCONST_0:
            noteVal(0);
            return;
        case LCONST_1:
            noteVal(1);
            return;

        case BIPUSH:
            return; /* will pick up value via sawInt */
        case LDC2_W:
            return; /* will pick up value via sawLong */

        case SIPUSH:
            return; /* will pick up value via sawInt */
        case LDC:
        case LDC_W:
            return; /* will pick up value via sawInt */

        case IAND:
        case LAND:
            bitop = IAND;
            isLong = seen == LAND;
            checkState(1);
            return;
        case IOR:
        case LOR:
            bitop = IOR;
            isLong = seen == LOR;
            checkState(1);
            return;

        case LCMP:
            if (state == 3) {
                isLong = true;
                return; /* Ignore. An 'if' opcode will follow */
            }
            state = 0;
            return;

        case IFLE:
        case IFLT:
        case IFGT:
        case IFGE:
            if (state == 3 && isLong || state == 2 && !isLong) {
                long bits = getFlagBits(isLong, arg0);
                boolean highbit = !isLong && (bits & 0x80000000) != 0 || isLong && bits < 0 && bits << 1 == 0;
                boolean onlyLowBits = bits >>> 12 == 0;
                BugInstance bug;
                if (highbit) {
                    bug = new BugInstance(this, "BIT_SIGNED_CHECK_HIGH_BIT", (seen == IFLE || seen == IFGT) ? HIGH_PRIORITY
                            : NORMAL_PRIORITY);
                } else {
                    bug = new BugInstance(this, "BIT_SIGNED_CHECK", onlyLowBits ? LOW_PRIORITY : NORMAL_PRIORITY);
                }
                bugReporter.reportBug(bug.addClassAndMethod(this).addSourceLine(this));
            }
            state = 0;
            return;

        case IFEQ:
        case IFNE:
            /* special case: if arg1 is 0 it will not be pushed */
            if (state == 2) {
                arg1 = 0;
                state = 3;
            }

            //$FALL-THROUGH$
        case IF_ICMPEQ:
        case IF_ICMPNE:
            checkState(3);
            if (state != 4) {
                return;
            }
            break; /* the only break in this switch! gross */

        case GOTO:
            state = -1;
            return;

        default:
            state = 0;
            return;
        }

        /* We have matched the instruction pattern, so check the args */
        long dif;
        String t;

        if (bitop == IOR) {
            dif = arg0 & ~arg1;
            t = "BIT_IOR";
        } else if (arg0 != 0 || arg1 != 0) {
            dif = arg1 & ~arg0;
            t = "BIT_AND";
        } else {
            dif = 1;
            t = "BIT_AND_ZZ";
        }

        if (dif != 0) {
            // System.out.println("Match at offset " + getPC());
            BugInstance bug = new BugInstance(this, t, HIGH_PRIORITY).addClassAndMethod(this);
            if (!"BIT_AND_ZZ".equals(t)) {
                bug.addString("0x" + Long.toHexString(arg0)).addString("0x" + Long.toHexString(arg1));
            }

            bug.addSourceLine(this);
            bugReporter.reportBug(bug);
        }
        state = 0;
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

