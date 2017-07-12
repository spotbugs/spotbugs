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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.charsets.UTF8;

public class Noise extends OpcodeStackDetector {

    class HashQueue {
        HashQueue(int size) throws NoSuchAlgorithmException {
            md = MessageDigest.getInstance("SHA");
            this.size = size;
            this.data = new byte[size];
        }

        MessageDigest md;

        final int size;

        int next = 0; // 0 <= next < size

        final byte[] data;

        // data is next..size-1, 0..next-1
        public void push(byte b) {
            data[next++] = b;
            if (next == size) {
                next = 0;
            }
        }

        public void reset() {
            next = 0;
            for (int i = 0; i < size; i++) {
                data[i] = 0;
            }
        }

        public void push(String s) {
            for (byte b : UTF8.getBytes(s)) {
                push(b);
            }
        }

        public void pushHash(Object x) {
            push(x.hashCode());
        }

        public void push(int x) {
            push((byte) (x));
            push((byte) (x >> 8));
            push((byte) (x >> 16));
            push((byte) (x >> 24));
        }

        public int getHash() {
            md.update(primer);
            md.update(data, next, size - next);
            md.update(data, 0, next);
            byte[] hash = md.digest();
            int result = (hash[0] & 0xff) | (hash[1] & 0xff) << 8 | (hash[2] & 0xff) << 16 | (hash[3] & 0x7f) << 24;
            return result;
        }

        public int getPriority() {
            int hash = getHash();

            if ((hash & 0x1ff0) == 0) {
                hash = hash & 0xf;
                if (hash < 1) {
                    return Priorities.HIGH_PRIORITY;
                } else if (hash < 1 + 2) {
                    return Priorities.NORMAL_PRIORITY;
                } else if (hash < 1 + 2 + 4) {
                    return Priorities.LOW_PRIORITY;
                } else {
                    return Priorities.IGNORE_PRIORITY;
                }
            } else {
                return Priorities.IGNORE_PRIORITY + 1;
            }
        }
    }

    //    final BugReporter bugReporter;

    final BugAccumulator accumulator;

    final HashQueue hq;

    byte[] primer;

    public Noise(BugReporter bugReporter) throws NoSuchAlgorithmException {
        //        this.bugReporter = bugReporter;
        this.accumulator = new BugAccumulator(bugReporter);
        hq = new HashQueue(24);
    }

    @Override
    public void visit(Code code) {
        primer = UTF8.getBytes(getFullyQualifiedMethodName());

        super.visit(code); // make callbacks to sawOpcode for all opcodes
        accumulator.reportAccumulatedBugs();
    }

    @Override
    public void sawInt(int i) {
        hq.push(i);
    }

    @Override
    public void sawLong(long x) {
        hq.push((int) (x >> 0));
        hq.push((int) (x >> 32));
    }

    @Override
    public void sawString(String s) {
        hq.pushHash(s);
    }

    @Override
    public void sawClass() {
        hq.push(getClassConstantOperand());
    }

    @Override
    public void sawOpcode(int seen) {
        int priority;
        switch (seen) {
        case Const.INVOKEINTERFACE:
        case Const.INVOKEVIRTUAL:
        case Const.INVOKESPECIAL:
        case Const.INVOKESTATIC:
            hq.pushHash(getClassConstantOperand());
            if (getNameConstantOperand().indexOf('$') == -1) {
                hq.pushHash(getNameConstantOperand());
            }
            hq.pushHash(getSigConstantOperand());

            priority = hq.getPriority();
            if (priority <= Priorities.LOW_PRIORITY) {
                accumulator.accumulateBug(new BugInstance(this, "NOISE_METHOD_CALL", priority).addClassAndMethod(this)
                        .addCalledMethod(this), this);
            }
            break;
        case Const.GETFIELD:
        case Const.PUTFIELD:
        case Const.GETSTATIC:
        case Const.PUTSTATIC:
            hq.pushHash(getClassConstantOperand());
            if (getNameConstantOperand().indexOf('$') == -1) {
                hq.pushHash(getNameConstantOperand());
            }
            hq.pushHash(getSigConstantOperand());
            priority = hq.getPriority();
            if (priority <= Priorities.LOW_PRIORITY) {
                accumulator.accumulateBug(new BugInstance(this, "NOISE_FIELD_REFERENCE", priority).addClassAndMethod(this)
                        .addReferencedField(this), this);
            }
            break;
        case Const.CHECKCAST:
        case Const.INSTANCEOF:
        case Const.NEW:
            hq.pushHash(getClassConstantOperand());
            break;
        case Const.IFEQ:
        case Const.IFNE:
        case Const.IFNONNULL:
        case Const.IFNULL:
        case Const.IF_ICMPEQ:
        case Const.IF_ICMPNE:
        case Const.IF_ICMPLE:
        case Const.IF_ICMPGE:
        case Const.IF_ICMPGT:
        case Const.IF_ICMPLT:
        case Const.IF_ACMPEQ:
        case Const.IF_ACMPNE:
        case Const.RETURN:
        case Const.ARETURN:
        case Const.IRETURN:
        case Const.MONITORENTER:
        case Const.MONITOREXIT:
        case Const.IINC:
        case Const.NEWARRAY:
        case Const.TABLESWITCH:
        case Const.LOOKUPSWITCH:
        case Const.LCMP:
        case Const.INEG:
        case Const.IADD:
        case Const.IMUL:
        case Const.ISUB:
        case Const.IDIV:
        case Const.IREM:
        case Const.IXOR:
        case Const.ISHL:
        case Const.ISHR:
        case Const.IUSHR:
        case Const.IAND:
        case Const.IOR:
        case Const.LAND:
        case Const.LOR:
        case Const.LADD:
        case Const.LMUL:
        case Const.LSUB:
        case Const.LDIV:
        case Const.LSHL:
        case Const.LSHR:
        case Const.LUSHR:
        case Const.AALOAD:
        case Const.AASTORE:
        case Const.IALOAD:
        case Const.IASTORE:
        case Const.BALOAD:
        case Const.BASTORE:
            hq.push(seen);
            priority = hq.getPriority();
            if (priority <= Priorities.LOW_PRIORITY) {
                accumulator.accumulateBug(
                        new BugInstance(this, "NOISE_OPERATION", priority).addClassAndMethod(this).addString(Const.getOpcodeName(seen)),
                        this);
            }
            break;
        default:
            break;
        }
    }

}
