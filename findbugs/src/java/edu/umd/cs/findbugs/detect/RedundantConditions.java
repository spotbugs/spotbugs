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

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.GOTO;
import org.apache.bcel.generic.ICONST;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.SourceLineAnnotation;
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

    public RedundantConditions(BugReporter bugReporter) {
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
                .addClassAndMethod(methodDescriptor).addString(condition.getTrueCondition());
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
        int priority = condition.isDeadCodeUnreachable() ? HIGH_PRIORITY : NORMAL_PRIORITY;
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
                    priority = NORMAL_PRIORITY;
                    if(after.getInstruction() instanceof InvokeInstruction) {
                        MethodGen methodGen;
                        try {
                            methodGen = Global.getAnalysisCache().getMethodAnalysis(MethodGen.class, methodDescriptor);
                        } catch (CheckedAnalysisException e) {
                            return priority;
                        }
                        ConstantPoolGen constantPool = methodGen.getConstantPool();
                        String methodName = ((InvokeInstruction)after.getInstruction()).getMethodName(constantPool);
                        // Ignore values conditions used in assertion methods
                        if((methodName.equals("assertTrue") || methodName.equals("checkArgument") || methodName.equals("isLegal")
                                || methodName.equals("isTrue"))
                                && liveValue == 1) {
                            return IGNORE_PRIORITY;
                        }
                        if((methodName.equals("assertFalse") || methodName.equals("isFalse"))
                                && liveValue == 0) {
                            return IGNORE_PRIORITY;
                        }
                    }
                }
            }
        }
        return priority;
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
