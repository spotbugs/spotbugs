/*
 * FindBugs - Find Bugs in Java programs
 * Copyright (C) 2003-2008 University of Maryland
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

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.GOTO;
import org.apache.bcel.generic.GotoInstruction;
import org.apache.bcel.generic.ICONST;
import org.apache.bcel.generic.IfInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.StringAnnotation;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.bcel.BCELUtil;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.engine.bcel.ValueRangeAnalysisFactory.RedundantCondition;
import edu.umd.cs.findbugs.classfile.engine.bcel.ValueRangeAnalysisFactory.ValueRangeAnalysis;

/**
 * @author Tagir Valeev
 */
public class RedundantConditions implements Detector {
    private final BugAccumulator bugAccumulator;
    private final BugReporter bugReporter;

    public RedundantConditions(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
        this.bugAccumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        for(Method method : classContext.getJavaClass().getMethods()) {
            MethodDescriptor methodDescriptor = BCELUtil.getMethodDescriptor(classContext.getJavaClass(), method);
            ValueRangeAnalysis analysis;
            try {
                analysis = Global.getAnalysisCache().getMethodAnalysis(ValueRangeAnalysis.class, methodDescriptor);
            } catch (CheckedAnalysisException e) {
                bugReporter.logError("ValueRangeAnalysis failed for "+methodDescriptor, e);
                continue;
            }
            if(analysis == null) {
                continue;
            }
            for(RedundantCondition condition : analysis.getRedundantConditions()) {
                int priority = getPriority(methodDescriptor, condition);
                SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(classContext, method,
                        condition.getLocation().getHandle().getPosition());
                BugInstance bug = new BugInstance(condition.isByType()?"UC_USELESS_CONDITION_TYPE":"UC_USELESS_CONDITION", priority)
                .addClassAndMethod(methodDescriptor).add(new StringAnnotation(normalize(condition.getTrueCondition())));
                if(condition.isByType()) {
                    bug.addType(condition.getSignature());
                }
                if(condition.isDeadCodeUnreachable() && condition.getDeadCodeLocation() != null && priority == HIGH_PRIORITY) {
                    bug.addSourceLine(methodDescriptor, condition.getDeadCodeLocation()).describe(SourceLineAnnotation.ROLE_UNREACHABLE_CODE);
                }
                bugAccumulator.accumulateBug(bug, sourceLineAnnotation);
            }
            bugAccumulator.reportAccumulatedBugs();
        }

    }

    private String normalize(String condition) {
        if(condition.startsWith("this.this$")) {
            return condition.substring("this.".length());
        }
        if(condition.startsWith("this.val$")) {
            return condition.substring("this.val$".length());
        }
        return condition;
    }

    private int getPriority(MethodDescriptor methodDescriptor, RedundantCondition condition) {
        if(condition.isByType()) {
            // Skip reports which should be reported by another detector
            long number = condition.getNumber().longValue();
            switch(condition.getSignature()) {
            case "I":
                if(number == Integer.MIN_VALUE || number == Integer.MAX_VALUE) {
                    // Will be reported as INT_VACUOUS_COMPARISON
                    return IGNORE_PRIORITY;
                }
                break;
            case "C":
                if(number <= 0) {
                    // Will be reported as INT_BAD_COMPARISON_WITH_NONNEGATIVE_VALUE
                    return IGNORE_PRIORITY;
                }
                break;
            case "B":
                if(number < Byte.MIN_VALUE || number >= Byte.MAX_VALUE) {
                    // Will be reported as INT_BAD_COMPARISON_WITH_SIGNED_BYTE
                    return IGNORE_PRIORITY;
                }
                break;
            default:
                break;
            }
        }
        int priority = condition.isDeadCodeUnreachable() ? HIGH_PRIORITY : condition.isBorder()
                || condition.getSignature().equals("Z") ? LOW_PRIORITY : NORMAL_PRIORITY;
        // check for boolean conversion
        if(condition.getDeadCodeLocation() != null && condition.getLiveCodeLocation() != null && condition.isDeadCodeUnreachable()) {
            InstructionHandle deadHandle = condition.getDeadCodeLocation().getHandle();
            InstructionHandle liveHandle = condition.getLiveCodeLocation().getHandle();
            int deadValue = getIntValue(deadHandle);
            int liveValue = getIntValue(liveHandle);
            if((deadValue == 0 && liveValue == 1) || (deadValue == 1 && liveValue == 0)) {
                InstructionHandle deadNext = deadHandle.getNext();
                InstructionHandle liveNext = liveHandle.getNext();
                if(deadNext != null && liveNext != null) {
                    InstructionHandle middle, after;
                    if(deadNext.getNext() == liveHandle) {
                        middle = deadNext;
                        after = liveNext;
                    } else if(liveNext.getNext() == deadHandle) {
                        middle = liveNext;
                        after = deadNext;
                    } else {
                        return priority;
                    }
                    if(!(middle.getInstruction() instanceof GOTO) || ((GOTO)middle.getInstruction()).getTarget() != after) {
                        return priority;
                    }
                    MethodGen methodGen;
                    try {
                        methodGen = Global.getAnalysisCache().getMethodAnalysis(MethodGen.class, methodDescriptor);
                    } catch (CheckedAnalysisException e) {
                        return priority;
                    }
                    InstructionHandle consumer = getConsumer(methodGen, after);
                    Instruction consumerInst = consumer == null ? null : consumer.getInstruction();
                    if(consumerInst != null) {
                        short opcode = consumerInst.getOpcode();
                        if(opcode == Constants.IADD || opcode == Constants.ISUB || opcode == Constants.IMUL
                                || opcode == Constants.ISHR || opcode == Constants.ISHL || opcode == Constants.IUSHR) {
                            // It's actually integer expression with explicit ? 1 : 0 or ? 0 : 1 operation
                            return priority;
                        }
                    }
                    if(condition.getSignature().equals("Z")) {
                        // Ignore !flag when flag value is known
                        return IGNORE_PRIORITY;
                    }
                    priority = condition.isBorder() ? LOW_PRIORITY : NORMAL_PRIORITY;
                    if(consumerInst instanceof InvokeInstruction) {
                        ConstantPoolGen constantPool = methodGen.getConstantPool();
                        String methodName = ((InvokeInstruction)consumerInst).getMethodName(constantPool);
                        // Ignore values conditions used in assertion methods
                        if((methodName.equals("assertTrue") || methodName.equals("checkArgument") || methodName.equals("isLegal")
                                || methodName.equals("isTrue"))) {
                            return liveValue == 1 ? condition.isBorder() ? IGNORE_PRIORITY : LOW_PRIORITY : HIGH_PRIORITY;
                        }
                        if((methodName.equals("assertFalse") || methodName.equals("isFalse"))) {
                            return liveValue == 0 ? condition.isBorder() ? IGNORE_PRIORITY : LOW_PRIORITY : HIGH_PRIORITY;
                        }
                    }
                }
            }
        }
        return priority;
    }

    /**
     * @param methodGen method
     * @param start instruction to scan
     * @return instruction which consumes value which was on top of stack before start instruction
     * or null if cannot be determined
     */
    private InstructionHandle getConsumer(MethodGen methodGen, InstructionHandle start) {
        int depth = 1;
        InstructionHandle cur = start;
        while(cur != null) {
            Instruction inst = cur.getInstruction();
            depth -= inst.consumeStack(methodGen.getConstantPool());
            if(depth <= 0) {
                return cur;
            }
            depth += inst.produceStack(methodGen.getConstantPool());
            if(inst instanceof BranchInstruction) {
                if(inst instanceof GotoInstruction) {
                    cur = ((GotoInstruction)inst).getTarget();
                    continue;
                }
                if(!(inst instanceof IfInstruction)) {
                    return null;
                }
            }
            cur = cur.getNext();
        }
        return null;
    }

    private int getIntValue(InstructionHandle handle) {
        Instruction instruction = handle.getInstruction();
        if(instruction instanceof ICONST) {
            return ((ICONST)instruction).getValue().intValue();
        }
        return -1;
    }

    @Override
    public void report() {
    }
}
