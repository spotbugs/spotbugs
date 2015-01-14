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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;
import org.objectweb.asm.Opcodes;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.detect.FindNoSideEffectMethods.MethodSideEffectStatus;
import edu.umd.cs.findbugs.detect.FindNoSideEffectMethods.NoSideEffectMethodsDatabase;

public class RepeatedConditionals extends OpcodeStackDetector {
    BugReporter bugReporter;

    private final NoSideEffectMethodsDatabase noSideEffectMethods;

    public RepeatedConditionals(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.noSideEffectMethods = Global.getAnalysisCache().getDatabase(NoSideEffectMethodsDatabase.class);
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
            if (emptyStackLocations.size() > 1) {
                for(int n=1; n<=emptyStackLocations.size()/2; n++) {
                    int first = emptyStackLocations.get(emptyStackLocations.size() - 2*n);
                    int second = emptyStackLocations.get(emptyStackLocations.size() - n);
                    int third = getPC();
                    if (third - second == second - first) {
                        int endOfFirstSegment = prevOpcodeLocations.get(emptyStackLocations.size() - n);
                        int endOfSecondSegment = oldPC;
                        int opcodeAtEndOfFirst = getCodeByte(endOfFirstSegment);
                        int opcodeAtEndOfSecond = getCodeByte(endOfSecondSegment);

                        if (!isBranch(opcodeAtEndOfFirst) || !isBranch(opcodeAtEndOfSecond)) {
                            continue;
                        }
                        if (opcodeAtEndOfFirst == Opcodes.GOTO || opcodeAtEndOfSecond == Opcodes.GOTO) {
                            continue;
                        }
                        if (opcodeAtEndOfFirst != opcodeAtEndOfSecond
                                && !areOppositeBranches(opcodeAtEndOfFirst, opcodeAtEndOfSecond)) {
                            continue;
                        }

                        if (first == endOfFirstSegment) {
                            continue;
                        }
                        Integer firstTarget = branchTargets.get(endOfFirstSegment);
                        Integer secondTarget = branchTargets.get(endOfSecondSegment);
                        if (firstTarget == null || secondTarget == null) {
                            continue;
                        }
                        if (firstTarget >= second && firstTarget <= endOfSecondSegment) {
                            // first jumps inside second
                            continue;
                        }
                        boolean identicalCheck = firstTarget.equals(secondTarget) && opcodeAtEndOfFirst == opcodeAtEndOfSecond
                                || (firstTarget.intValue() == getPC() && opcodeAtEndOfFirst != opcodeAtEndOfSecond);
                        if(!compareCode(first, endOfFirstSegment, second, endOfSecondSegment, !identicalCheck)) {
                            continue;
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
                        if (!identicalCheck) {
                            // opposite checks
                            priority += 2;
                        }

                        BugInstance bug = new BugInstance(this, "RpC_REPEATED_CONDITIONAL_TEST", priority).addClassAndMethod(this)
                                .add(firstSourceLine).add(secondSourceLine);
                        bugReporter.reportBug(bug);
                    }
                }
            }
            emptyStackLocations.add(getPC());
            prevOpcodeLocations.add(oldPC);

        }
        oldPC = getPC();
    }

    private boolean compareCode(int first, int endOfFirstSegment, int second,
            int endOfSecondSegment, boolean oppositeChecks) {
        if(endOfFirstSegment-first != endOfSecondSegment-second) {
            return false;
        }
        MethodGen methodGen = null;
        try {
            methodGen = Global.getAnalysisCache().getMethodAnalysis(MethodGen.class, getMethodDescriptor());
        } catch (CheckedAnalysisException e) {
            // Ignore
        }
        if(methodGen == null) {
            // MethodGen is absent for some reason: fallback to byte-to-byte comparison
            byte[] code = getCode().getCode();
            for (int i = first; i < endOfFirstSegment; i++) {
                if (code[i] != code[i - first + second]) {
                    return false;
                }
            }
            return true;
        }
        InstructionHandle firstHandle = methodGen.getInstructionList().findHandle(first);
        InstructionHandle secondHandle = methodGen.getInstructionList().findHandle(second);
        while(true) {
            if(firstHandle == null || secondHandle == null) {
                return false;
            }
            if(firstHandle.getPosition() >= endOfFirstSegment) {
                return secondHandle.getPosition() >= endOfSecondSegment;
            }
            if(secondHandle.getPosition() >= endOfSecondSegment) {
                return firstHandle.getPosition() >= endOfFirstSegment;
            }
            Instruction firstInstruction = firstHandle.getInstruction();
            Instruction secondInstruction = secondHandle.getInstruction();
            if(firstInstruction instanceof BranchInstruction && secondInstruction instanceof BranchInstruction) {
                int firstOpcode = firstInstruction.getOpcode();
                int secondOpcode = secondInstruction.getOpcode();
                if(firstOpcode != secondOpcode) {
                    return false;
                }
                int firstTarget = ((BranchInstruction)firstInstruction).getTarget().getPosition();
                int secondTarget = ((BranchInstruction)secondInstruction).getTarget().getPosition();
                if(firstTarget == second) {
                    if(oppositeChecks || secondTarget <= endOfSecondSegment) {
                        return false;
                    }
                } else {
                    if(!((firstTarget >= first && firstTarget <= endOfFirstSegment && firstTarget - first == secondTarget - second)
                            || firstTarget == secondTarget)) {
                        return false;
                    }
                }
            } else {
                if(!firstInstruction.equals(secondInstruction)) {
                    return false;
                }
            }
            firstHandle = firstHandle.getNext();
            secondHandle = secondHandle.getNext();
        }
    }

    private boolean hasSideEffect(int seen) {
        if(seen == INVOKEVIRTUAL || seen == INVOKESPECIAL || seen == INVOKEINTERFACE || seen == INVOKESTATIC) {
            return noSideEffectMethods.is(getMethodDescriptorOperand(), MethodSideEffectStatus.SE, MethodSideEffectStatus.OBJ);
        }
        return isRegisterStore() || isReturn(seen) || isSwitch(seen) || seen == INVOKEDYNAMIC || seen == PUTFIELD
                || seen == PUTSTATIC;
    }

    private void reset() {
        emptyStackLocations.clear();
        prevOpcodeLocations.clear();
        branchTargets.clear();
        oldPC = -1;
    }

}
