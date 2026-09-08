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

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.ba.ch.Subtypes2;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import org.apache.bcel.Const;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Code;

import java.util.Set;

public class ReadReturnShouldBeChecked extends OpcodeStackDetector implements StatelessDetector {

    boolean sawRead = false;

    boolean sawSkip = false;

    boolean recentCallToAvailable = false;

    int sawAvailable = 0;

    boolean wasBufferedInputStream = false;

    BugAccumulator accumulator;

    private int locationOfCall;

    private String lastCallClass = null, lastCallMethod = null, lastCallSig = null;

    private boolean readProcessed = false;

    private OpcodeStack.Item itemToBeCompared;

    private static final Set<Short> comparisonOpcodes = Set.of(
            Const.IF_ICMPEQ, Const.IF_ICMPNE,
            Const.IF_ICMPLT, Const.IF_ICMPGE,
            Const.IF_ICMPGT, Const.IF_ICMPLE,
            Const.FCMPL, Const.FCMPG,
            Const.DCMPL, Const.DCMPG,
            Const.LCMP);

    public ReadReturnShouldBeChecked(BugReporter bugReporter) {
        this.accumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visit(Code obj) {
        sawAvailable = 0;
        sawRead = false;
        sawSkip = false;
        readProcessed = false;
        itemToBeCompared = null;
        super.visit(obj);
        accumulator.reportAccumulatedBugs();
    }

    private boolean isInputStream() {

        if (lastCallClass.startsWith("[")) {
            return false;
        }
        return (Subtypes2.instanceOf(lastCallClass, "java.io.InputStream")
                || Subtypes2.instanceOf(lastCallClass, "java.io.DataInput") || Subtypes2.instanceOf(lastCallClass,
                        "java.io.Reader")) && !Subtypes2.instanceOf(lastCallClass, "java.io.ByteArrayInputStream");

    }

    private boolean isBufferedInputStream() {
        try {
            if (lastCallClass.startsWith("[")) {
                return false;
            }
            return Repository.instanceOf(lastCallClass, "java.io.BufferedInputStream");
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private boolean isImageIOInputStream() {
        try {
            if (lastCallClass.startsWith("[")) {
                return false;
            }
            return Repository.instanceOf(lastCallClass, "javax.imageio.stream.ImageInputStream");
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public void sawOpcode(int seen) {
        readProcessed = false;

        if (seen == Const.INVOKEVIRTUAL || seen == Const.INVOKEINTERFACE) {
            lastCallClass = getDottedClassConstantOperand();
            lastCallMethod = getNameConstantOperand();
            lastCallSig = getSigConstantOperand();
        }

        if (seen == Const.INVOKEVIRTUAL || seen == Const.INVOKEINTERFACE) {
            if ("available".equals(getNameConstantOperand()) && "()I".equals(getSigConstantOperand())
                    || getNameConstantOperand().startsWith("get") && getNameConstantOperand().endsWith("Length")
                            && "()I".equals(getSigConstantOperand()) || "java/io/File".equals(getClassConstantOperand())
                                    && "length".equals(getNameConstantOperand()) && "()J".equals(getSigConstantOperand())) {
                sawAvailable = 70;
                return;
            }
        }
        sawAvailable--;
        if ((seen == Const.INVOKEVIRTUAL || seen == Const.INVOKEINTERFACE)
                && "read".equals(getNameConstantOperand())

                && ("([B)I".equals(getSigConstantOperand()) || "([BII)I".equals(getSigConstantOperand())
                        || "([C)I".equals(getSigConstantOperand()) || "([CII)I".equals(getSigConstantOperand()))
                && isInputStream()) {
            sawRead = true;
            recentCallToAvailable = sawAvailable > 0;
            locationOfCall = getPC();
            readProcessed = true;
            return;
        }
        if ((seen == Const.INVOKEVIRTUAL || seen == Const.INVOKEINTERFACE)
                && ("skip".equals(getNameConstantOperand()) && "(J)J".equals(getSigConstantOperand()) || "skipBytes".equals(getNameConstantOperand())
                        && "(I)I".equals(getSigConstantOperand())) && isInputStream()
                && !isImageIOInputStream()) {
            // if not ByteArrayInput Stream
            // and either no recent calls to length
            // or it is a BufferedInputStream

            wasBufferedInputStream = isBufferedInputStream();
            sawSkip = true;
            locationOfCall = getPC();
            recentCallToAvailable = sawAvailable > 0 && !wasBufferedInputStream;
            return;

        }

        if ((seen == Const.POP) || (seen == Const.POP2)) {

            if (sawRead) {
                accumulator.accumulateBug(
                        new BugInstance(this, "RR_NOT_CHECKED", recentCallToAvailable ? LOW_PRIORITY : NORMAL_PRIORITY)
                                .addClassAndMethod(this).addCalledMethod(lastCallClass, lastCallMethod, lastCallSig, false),
                        SourceLineAnnotation.fromVisitedInstruction(getClassContext(), this, locationOfCall));

            } else if (sawSkip) {

                accumulator.accumulateBug(
                        new BugInstance(this, "SR_NOT_CHECKED", (wasBufferedInputStream ? HIGH_PRIORITY
                                : recentCallToAvailable ? LOW_PRIORITY : NORMAL_PRIORITY)).addClassAndMethod(this)
                                .addCalledMethod(lastCallClass, lastCallMethod, lastCallSig, false), SourceLineAnnotation
                                        .fromVisitedInstruction(getClassContext(), this, locationOfCall));
            }
        }

        if (sawRead && (seen == Const.I2F || seen == Const.I2D || seen == Const.I2L)) {
            readProcessed = true;
        }

        if (comparisonOpcodes.contains((short) seen) && stack.getStackDepth() > 1) {
            OpcodeStack.Item rightItem = stack.getStackItem(0);
            OpcodeStack.Item leftItem = stack.getStackItem(1);
            Object value = null;
            if (leftItem.equals(itemToBeCompared)) {
                value = rightItem.getConstant();
            } else if (rightItem.equals(itemToBeCompared)) {
                value = leftItem.getConstant();
            }

            reportIfMinusOneValue(value);
        }

        //The return value of the read() is copied into a variable
        if (seen == Const.DUP && itemToBeCompared != null) {
            itemToBeCompared = null;
        }

        sawRead = false;
        sawSkip = false;
    }

    @Override
    public void afterOpcode(int seen) {
        super.afterOpcode(seen);

        if (readProcessed) {
            itemToBeCompared = stack.getStackDepth() > 0 ? stack.getStackItem(0) : null;
        }
    }

    private void reportIfMinusOneValue(Object value) {
        boolean isMinusOne = false;

        if (value instanceof Integer) {
            isMinusOne = (Integer) value == -1;
        } else if (value instanceof Float) {
            isMinusOne = (Float) value == -1.0f;
        } else if (value instanceof Double) {
            isMinusOne = (Double) value == -1.0d;
        } else if (value instanceof Long) {
            isMinusOne = (Long) value == -1L;
        }

        if (isMinusOne) {
            accumulator.accumulateBug(
                    new BugInstance(this, "NCR_NOT_PROPERLY_CHECKED_READ", recentCallToAvailable ? LOW_PRIORITY : NORMAL_PRIORITY)
                            .addClassAndMethod(this)
                            .addCalledMethod(lastCallClass, lastCallMethod, lastCallSig, false),
                    SourceLineAnnotation.fromVisitedInstruction(getClassContext(), this, locationOfCall));
        }
    }
}
