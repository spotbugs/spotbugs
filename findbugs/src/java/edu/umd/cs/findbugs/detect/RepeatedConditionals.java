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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.bcel.classfile.Code;
import org.objectweb.asm.Opcodes;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class RepeatedConditionals extends OpcodeStackDetector {
    private static final Set<String> NO_SIDEEFFECT_CLASSES = new HashSet<>(Arrays.asList(
            "java/lang/String", "java/lang/Integer", "java/lang/Long", "java/lang/Double",
            "java/lang/Float", "java/lang/Byte", "java/lang/Short", "java/math/BigInteger",
            "java/math/BigDecimal"));

    BugReporter bugReporter;

    public RepeatedConditionals(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        reset();
    }

    @Override
    public void visit(Code code) {
        boolean interesting = true;
        if (interesting) {
            // initialize any variables we want to initialize for the method
            super.visit(code); // make callbacks to sawOpcode for all opcodes
            reset();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see edu.umd.cs.findbugs.bcel.OpcodeStackDetector#sawOpcode(int)
     */
    int oldPC;

    LinkedList<Integer> emptyStackLocations = new LinkedList<Integer>();

    LinkedList<Integer> prevOpcodeLocations = new LinkedList<Integer>();

    Map<Integer, Integer> branchTargets = new HashMap<Integer, Integer>();

    @Override
    public void sawBranchTo(int pc) {
        branchTargets.put(getPC(), pc);
    }

    @Override
    public void sawOpcode(int seen) {
        if (hasSideEffect(seen)) {
            reset();
        } else if (stack.getStackDepth() == 0) {
            check: if (emptyStackLocations.size() > 1) {
                int first = emptyStackLocations.get(emptyStackLocations.size() - 2);
                int second = emptyStackLocations.get(emptyStackLocations.size() - 1);
                int third = getPC();
                if (third - second == second - first) {
                    int endOfFirstSegment = prevOpcodeLocations.get(emptyStackLocations.size() - 1);
                    int endOfSecondSegment = oldPC;
                    int opcodeAtEndOfFirst = getCodeByte(endOfFirstSegment);
                    int opcodeAtEndOfSecond = getCodeByte(endOfSecondSegment);

                    if (!isBranch(opcodeAtEndOfFirst) || !isBranch(opcodeAtEndOfSecond)) {
                        break check;
                    }
                    if (opcodeAtEndOfFirst == Opcodes.GOTO || opcodeAtEndOfSecond == Opcodes.GOTO) {
                        break check;
                    }
                    if (opcodeAtEndOfFirst != opcodeAtEndOfSecond
                            && !areOppositeBranches(opcodeAtEndOfFirst, opcodeAtEndOfSecond)) {
                        break check;
                    }

                    byte[] code = getCode().getCode();
                    if (first == endOfFirstSegment) {
                        break check;
                    }
                    for (int i = first; i < endOfFirstSegment; i++) {
                        if (code[i] != code[i - first + second]) {
                            break check;
                        }
                    }
                    if (false) {
                        System.out.println(getFullyQualifiedMethodName());
                        System.out.println(first + " ... " + endOfFirstSegment + " : " + OPCODE_NAMES[opcodeAtEndOfFirst]);
                        System.out.println(second + " ... " + endOfSecondSegment + " : " + OPCODE_NAMES[opcodeAtEndOfSecond]);
                    }
                    SourceLineAnnotation firstSourceLine = SourceLineAnnotation.fromVisitedInstructionRange(getClassContext(),
                            this, first, endOfFirstSegment - 1);
                    SourceLineAnnotation secondSourceLine = SourceLineAnnotation.fromVisitedInstructionRange(getClassContext(),
                            this, second, endOfSecondSegment - 1);

                    int priority = HIGH_PRIORITY;
                    if (firstSourceLine.getStartLine() == -1 || firstSourceLine.getStartLine() != secondSourceLine.getEndLine()) {
                        priority++;
                    }
                    if (stack.isJumpTarget(second)) {
                        priority++;
                    }
                    Integer firstTarget = branchTargets.get(endOfFirstSegment);
                    Integer secondTarget = branchTargets.get(endOfSecondSegment);
                    if (firstTarget == null || secondTarget == null) {
                        break check;
                    }
                    if (firstTarget.equals(secondTarget) && opcodeAtEndOfFirst == opcodeAtEndOfSecond
                            || firstTarget.intValue() == getPC()) {
                        // identical checks;
                    } else {
                        // opposite checks
                        priority += 2;
                    }

                    BugInstance bug = new BugInstance(this, "RpC_REPEATED_CONDITIONAL_TEST", priority).addClassAndMethod(this)
                            .add(firstSourceLine).add(secondSourceLine);
                    bugReporter.reportBug(bug);
                }
            }
        emptyStackLocations.add(getPC());
        prevOpcodeLocations.add(oldPC);

        }
        oldPC = getPC();
    }

    private boolean hasSideEffect(int seen) {
        if(seen == INVOKEVIRTUAL || seen == INVOKESTATIC) {
            if(NO_SIDEEFFECT_CLASSES.contains(getClassDescriptorOperand().getClassName())) {
                return false;
            }
            if(seen == INVOKEVIRTUAL && getMethodDescriptorOperand().getName().equals("equals") &&
                    getMethodDescriptorOperand().getSignature().equals("(Ljava/lang/Object;)Z")) {
                return false;
            }
            return true;
        }
        return isRegisterStore() || isReturn(seen) || isSwitch(seen) || seen == INVOKESPECIAL
                || seen == INVOKEINTERFACE || seen == INVOKEDYNAMIC || seen == PUTFIELD || seen == PUTSTATIC;
    }

    private void reset() {
        emptyStackLocations.clear();
        prevOpcodeLocations.clear();
        branchTargets.clear();
        oldPC = -1;
    }

}
