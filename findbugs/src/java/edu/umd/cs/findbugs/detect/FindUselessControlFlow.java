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

import java.util.BitSet;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.LineNumber;
import org.apache.bcel.classfile.LineNumberTable;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.StatelessDetector;

/**
 * A Detector to look for useless control flow. For example,
 *
 * <pre>
 * if (argv.length == 1)
 *     ;
 * System.out.println(&quot;Hello, &quot; + argv[0]);
 * </pre>
 *
 * In this kind of bug, we'll see an ifcmp instruction where the IF target is
 * the same as the fall-through target.
 * <p/>
 * <p>
 * The idea for this detector came from Richard P. King, and the idea of looking
 * for if instructions with identical branch and fall-through targets is from
 * Mike Fagan.
 *
 * @author David Hovemeyer
 */
public class FindUselessControlFlow extends BytecodeScanningDetector implements StatelessDetector {
    private static final BitSet ifInstructionSet = new BitSet();

    static {
        ifInstructionSet.set(Constants.IF_ACMPEQ);
        ifInstructionSet.set(Constants.IF_ACMPNE);
        ifInstructionSet.set(Constants.IF_ICMPEQ);
        ifInstructionSet.set(Constants.IF_ICMPNE);
        ifInstructionSet.set(Constants.IF_ICMPLT);
        ifInstructionSet.set(Constants.IF_ICMPLE);
        ifInstructionSet.set(Constants.IF_ICMPGT);
        ifInstructionSet.set(Constants.IF_ICMPGE);
        ifInstructionSet.set(Constants.IFEQ);
        ifInstructionSet.set(Constants.IFNE);
        ifInstructionSet.set(Constants.IFLT);
        ifInstructionSet.set(Constants.IFLE);
        ifInstructionSet.set(Constants.IFGT);
        ifInstructionSet.set(Constants.IFGE);
        ifInstructionSet.set(Constants.IFNULL);
        ifInstructionSet.set(Constants.IFNONNULL);
    }

    private final BugAccumulator bugAccumulator;

    public FindUselessControlFlow(BugReporter bugReporter) {
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visit(Code obj) {
        super.visit(obj);
        bugAccumulator.reportAccumulatedBugs();
    }

    @Override
    public void sawOpcode(int seen) {
        if (ifInstructionSet.get(seen)) {
            if (getBranchTarget() == getBranchFallThrough()) {
                int priority = NORMAL_PRIORITY;

                LineNumberTable lineNumbers = getCode().getLineNumberTable();
                if (lineNumbers != null) {
                    int branchLineNumber = lineNumbers.getSourceLine(getPC());
                    int targetLineNumber = lineNumbers.getSourceLine(getBranchFallThrough());
                    int nextLine = getNextSourceLine(lineNumbers, branchLineNumber);

                    if (branchLineNumber + 1 == targetLineNumber || branchLineNumber == targetLineNumber
                            && nextLine == branchLineNumber + 1) {
                        priority = HIGH_PRIORITY;
                    } else if (branchLineNumber + 2 < Math.max(targetLineNumber, nextLine)) {
                        priority = LOW_PRIORITY;
                    }
                } else {
                    priority = LOW_PRIORITY;
                }
                bugAccumulator.accumulateBug(new BugInstance(this,
                        priority == HIGH_PRIORITY ? "UCF_USELESS_CONTROL_FLOW_NEXT_LINE" : "UCF_USELESS_CONTROL_FLOW", priority)
                .addClassAndMethod(this), this);
            }
        }
    }

    public static int getNextSourceLine(LineNumberTable lineNumbers, int sourceLine) {
        int result = Integer.MAX_VALUE;
        for (LineNumber ln : lineNumbers.getLineNumberTable()) {

            int thisLine = ln.getLineNumber();
            if (sourceLine < thisLine && thisLine < result) {
                result = thisLine;
            }
        }
        return result;

    }
}

