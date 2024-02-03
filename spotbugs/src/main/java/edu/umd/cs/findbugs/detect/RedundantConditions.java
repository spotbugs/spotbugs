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

import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.BranchInstruction;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.GOTO;
import org.apache.bcel.generic.GotoInstruction;
import org.apache.bcel.generic.ICONST;
import org.apache.bcel.generic.IFNE;
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
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.Branch;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.EdgeTypes;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.LongRangeSet;
import edu.umd.cs.findbugs.ba.ValueRangeDataflow;
import edu.umd.cs.findbugs.ba.ValueRangeMap;
import edu.umd.cs.findbugs.bcel.BCELUtil;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.Global;
import edu.umd.cs.findbugs.classfile.MethodDescriptor;
import edu.umd.cs.findbugs.classfile.engine.bcel.FinallyDuplicatesInfoFactory.FinallyDuplicatesInfo;

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
        for (Method method : classContext.getJavaClass().getMethods()) {
            if (method.getCode() == null) {
                continue;
            }

            MethodDescriptor methodDescriptor = BCELUtil.getMethodDescriptor(classContext.getJavaClass(), method);
            ValueRangeDataflow dataflow = null;

            try {
                FinallyDuplicatesInfo fi = Global.getAnalysisCache().getMethodAnalysis(FinallyDuplicatesInfo.class, methodDescriptor);
                dataflow = classContext.getValueRangeDataflow(method);
                CFG cfg = dataflow.getCFG();

                BitSet assertionBlocks = new BitSet();
                MethodGen methodGen = cfg.getMethodGen();
                for (InstructionHandle ih : methodGen.getInstructionList()) {
                    if (ih.getInstruction() instanceof GETSTATIC) {
                        Instruction next = ih.getNext().getInstruction();
                        if (next instanceof IFNE) {
                            GETSTATIC getStatic = (GETSTATIC) ih.getInstruction();
                            if ("$assertionsDisabled".equals(getStatic.getFieldName(methodGen.getConstantPool()))
                                    && "Z".equals(getStatic.getSignature(methodGen.getConstantPool()))) {
                                int end = ((IFNE) next).getTarget().getPosition();
                                assertionBlocks.set(ih.getNext().getPosition(), end);
                            }
                        }
                    }
                }

                edgeLoop: for (Iterator<Edge> it = cfg.edgeIterator(); it.hasNext();) {
                    Edge edge = it.next();
                    if (edge.getType() != EdgeTypes.IFCMP_EDGE) {
                        continue;
                    }

                    if (assertionBlocks.get(Location.getLastLocation(edge.getSource()).getHandle().getPosition())) {
                        continue;
                    }

                    ValueRangeMap vrmEdge = dataflow.getFactOnEdge(edge);
                    BasicBlock source = edge.getSource();
                    ValueRangeMap vrmBefore = dataflow.getFactAfterLocation(new Location(source.getLastInstruction(), source));
                    Branch branch = vrmEdge.getBranch();
                    if (branch != null) {
                        LongRangeSet rangeBefore = vrmBefore.getRange(branch.getValueNumber());
                        LongRangeSet rangeOnEdge = vrmEdge.getRange(branch.getValueNumber());

                        if (rangeOnEdge.isEmpty() || rangeOnEdge.isFull() || rangeBefore != null && rangeOnEdge.same(rangeBefore)) {
                            if (fi != null && !fi.getDuplicates(cfg, edge).isEmpty()) {
                                rangeBefore = new LongRangeSet(rangeBefore);
                                rangeOnEdge = new LongRangeSet(rangeOnEdge);
                                List<Edge> duplicates = fi.getDuplicates(cfg, edge);
                                for (Edge dup : duplicates) {
                                    ValueRangeMap vrmEdgeDup = dataflow.getFactOnEdge(dup);
                                    BasicBlock sourceDup = dup.getSource();
                                    ValueRangeMap vrmBeforeDup = dataflow.getFactAfterLocation(new Location(sourceDup.getLastInstruction(),
                                            sourceDup));
                                    Branch branchDup = vrmEdgeDup.getBranch();
                                    if (branchDup != null) {
                                        LongRangeSet rangeBeforeDup = vrmBeforeDup.getRange(branchDup.getValueNumber());
                                        if (rangeBeforeDup != null) {
                                            rangeBefore.add(rangeBeforeDup);
                                        }
                                        LongRangeSet rangeOnEdgeDup = vrmEdgeDup.getRange(branchDup.getValueNumber());
                                        if (rangeOnEdgeDup != null) {
                                            rangeOnEdge.add(rangeOnEdgeDup);
                                        }
                                        if (!rangeOnEdge.isEmpty() && !rangeOnEdge.isFull() &&
                                                (rangeBefore == null || !rangeOnEdge.same(rangeBefore))) {
                                            continue edgeLoop;
                                        }
                                    } else {
                                        continue edgeLoop;
                                    }
                                }
                            }

                            boolean isByType = isByType(branch, rangeOnEdge);
                            String bugType = isByType ? "UC_USELESS_CONDITION_TYPE" : "UC_USELESS_CONDITION";

                            Edge fallThroughEdge = getFallThroughEdge(cfg, edge);
                            Edge deadEdge = rangeOnEdge.isEmpty() ? edge : fallThroughEdge;
                            Edge liveEdge = rangeOnEdge.isEmpty() ? fallThroughEdge : edge;
                            boolean isDeadCodeUnreachable = isSoleEdgeToTarget(cfg, deadEdge);
                            Location deadCodeLocation = getTargetLocation(cfg, deadEdge);
                            Location liveCodeLocation = getTargetLocation(cfg, liveEdge);
                            Set<Long> borders = rangeBefore.getBorders();

                            int priority = getPriority(methodDescriptor, branch, rangeOnEdge, isByType, isDeadCodeUnreachable,
                                    deadCodeLocation, liveCodeLocation, borders);
                            SourceLineAnnotation sourceLineAnnotation = SourceLineAnnotation.fromVisitedInstruction(classContext, method,
                                    new Location(source.getLastInstruction(), source));
                            BugInstance bug = new BugInstance(bugType, priority)
                                    .addClassAndMethod(methodDescriptor).add(new StringAnnotation(assembleCondition(normalize(branch.getVarName()),
                                            rangeOnEdge.isEmpty() ? branch.getFalseCondition() : branch.getTrueCondition(),
                                            convertNumber(rangeOnEdge.getSignature(), branch.getNumber()))));
                            if (isByType) {
                                bug.addType(rangeOnEdge.getSignature());
                            }
                            if (isDeadCodeUnreachable && deadCodeLocation != null && priority == HIGH_PRIORITY) {
                                bug.addSourceLine(methodDescriptor, deadCodeLocation).describe(SourceLineAnnotation.ROLE_UNREACHABLE_CODE);
                            }
                            bugAccumulator.accumulateBug(bug, sourceLineAnnotation);
                        }
                    }
                }
                bugAccumulator.reportAccumulatedBugs();
            } catch (CheckedAnalysisException e) {
                AnalysisContext.logError("Could not get value range data flow", e);
            }
        }
    }

    private boolean isByType(Branch branch, LongRangeSet range) {
        return !range.inTypeRange(branch.getNumber().longValue()) ||
                (">=".equals(branch.getTrueCondition()) || "<".equals(branch.getTrueCondition())) &&
                        branch.getNumber().longValue() == range.getTypeMin() ||
                ("<=".equals(branch.getTrueCondition()) || ">".equals(branch.getTrueCondition()))
                        && branch.getNumber().longValue() == range.getTypeMax();
    }

    private String normalize(String condition) {
        if (condition.startsWith("this.this$")) {
            return condition.substring("this.".length());
        }
        if (condition.startsWith("this.val$")) {
            return condition.substring("this.val$".length());
        }
        return condition;
    }

    private static String convertNumber(String signature, Number number) {
        long val = number.longValue();
        switch (signature) {
        case "Z":
            if (val == 0) {
                return "false";
            }
            return "true";
        case "C":
            if (val == '\n') {
                return "'\\n'";
            }
            if (val == '\r') {
                return "'\\r'";
            }
            if (val == '\b') {
                return "'\\b'";
            }
            if (val == '\t') {
                return "'\\t'";
            }
            if (val == '\'') {
                return "'\\''";
            }
            if (val == '\\') {
                return "'\\\\'";
            }
            if (val >= 32 && val < 128) {
                return "'" + ((char) val) + "'";
            }
            return convertNumber(val);
        case "I":
            if (val >= 32 && val < 128) {
                return val + " ('" + ((char) val) + "')";
            }
            return convertNumber(val);
        default:
            return convertNumber(val);
        }
    }

    private static String convertNumber(long val) {
        if (val == Long.MIN_VALUE) {
            return "Long.MIN_VALUE";
        }
        if (val == Long.MAX_VALUE) {
            return "Long.MAX_VALUE";
        }
        String suffix = "";
        if (val > Integer.MAX_VALUE || val < Integer.MIN_VALUE) {
            suffix = "L";
        }
        if (val > 128) {
            return val + suffix + " (0x" + Long.toHexString(val) + suffix + ")";
        }
        return val + suffix;
    }

    private String assembleCondition(String varName, String condition, String number) {
        if ("!=".equals(condition)) {
            if ("true".equals(number)) {
                return varName + " == false";
            }
            if ("false".equals(number)) {
                return varName + " == true";
            }
        }
        return varName + " " + condition + " " + number;
    }

    private Edge getFallThroughEdge(CFG cfg, Edge ifcmpEdge) {
        BasicBlock source = ifcmpEdge.getSource();
        return cfg.getOutgoingEdgeWithType(source, EdgeTypes.FALL_THROUGH_EDGE);
    }

    private boolean isSoleEdgeToTarget(CFG cfg, Edge edge) {
        BasicBlock target = edge.getTarget();
        return cfg.getNumIncomingEdges(target) == 1;
    }

    private Location getTargetLocation(CFG cfg, Edge edge) {
        BasicBlock target = edge.getTarget();
        if (target.isEmpty()) {
            Edge fallThroughEdge = cfg.getOutgoingEdgeWithType(target, EdgeTypes.FALL_THROUGH_EDGE);
            if (fallThroughEdge == null) {
                return null;
            }
            target = fallThroughEdge.getTarget();
        }

        if (!target.isEmpty()) {
            return new Location(target.getFirstInstruction(), target);
        }

        return null;
    }

    private int getPriority(MethodDescriptor methodDescriptor, Branch branch, LongRangeSet range, boolean isByType,
            boolean isDeadCodeUnreachable, Location deadCodeLocation, Location liveCodeLocation, Set<Long> borders) {
        if (isByType) {
            // Skip reports which should be reported by another detector
            long number = branch.getNumber().longValue();
            switch (range.getSignature()) {
            case "I":
                if (number == Integer.MIN_VALUE || number == Integer.MAX_VALUE) {
                    // Will be reported as INT_VACUOUS_COMPARISON
                    return IGNORE_PRIORITY;
                }
                break;
            case "C":
                if (number <= 0) {
                    // Will be reported as INT_BAD_COMPARISON_WITH_NONNEGATIVE_VALUE
                    return IGNORE_PRIORITY;
                }
                break;
            case "B":
                if (number < Byte.MIN_VALUE || number >= Byte.MAX_VALUE) {
                    // Will be reported as INT_BAD_COMPARISON_WITH_SIGNED_BYTE
                    return IGNORE_PRIORITY;
                }
                break;
            default:
                break;
            }
        }

        boolean isBorder = borders.contains(branch.getNumber().longValue());
        int priority = isDeadCodeUnreachable ? HIGH_PRIORITY
                : isBorder
                        || range.getSignature().equals("Z") ? LOW_PRIORITY : NORMAL_PRIORITY;
        // check for boolean conversion
        if (deadCodeLocation != null && liveCodeLocation != null && isDeadCodeUnreachable) {
            InstructionHandle deadHandle = deadCodeLocation.getHandle();
            InstructionHandle liveHandle = liveCodeLocation.getHandle();

            int deadValue = getIntValue(deadHandle);
            int liveValue = getIntValue(liveHandle);
            if ((deadValue == 0 && liveValue == 1) || (deadValue == 1 && liveValue == 0)) {
                InstructionHandle deadNext = deadHandle.getNext();
                InstructionHandle liveNext = liveHandle.getNext();
                if (deadNext != null && liveNext != null) {
                    InstructionHandle middle, after;
                    if (deadNext.getNext() == liveHandle) {
                        middle = deadNext;
                        after = liveNext;
                    } else if (liveNext.getNext() == deadHandle) {
                        middle = liveNext;
                        after = deadNext;
                    } else {
                        return priority;
                    }
                    if (!(middle.getInstruction() instanceof GOTO) || ((GOTO) middle.getInstruction()).getTarget() != after) {
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
                    if (consumerInst != null) {
                        short opcode = consumerInst.getOpcode();
                        if (opcode == Const.IADD || opcode == Const.ISUB || opcode == Const.IMUL
                                || opcode == Const.ISHR || opcode == Const.ISHL || opcode == Const.IUSHR) {
                            // It's actually integer expression with explicit ? 1 : 0 or ? 0 : 1 operation
                            return priority;
                        }
                    }
                    if (range.getSignature().equals("Z")) {
                        // Ignore !flag when flag value is known
                        return IGNORE_PRIORITY;
                    }
                    priority = isBorder ? LOW_PRIORITY : NORMAL_PRIORITY;
                    if (consumerInst instanceof InvokeInstruction) {
                        ConstantPoolGen constantPool = methodGen.getConstantPool();
                        String methodName = ((InvokeInstruction) consumerInst).getMethodName(constantPool);
                        // Ignore values conditions used in assertion methods
                        if ((methodName.equals("assertTrue") || methodName.equals("checkArgument") || methodName.equals("isLegal")
                                || methodName.equals("isTrue"))) {
                            return liveValue == 1 ? isBorder ? IGNORE_PRIORITY : LOW_PRIORITY : HIGH_PRIORITY;
                        }
                        if ((methodName.equals("assertFalse") || methodName.equals("isFalse"))) {
                            return liveValue == 0 ? isBorder ? IGNORE_PRIORITY : LOW_PRIORITY : HIGH_PRIORITY;
                        }
                    }
                }
            }
        }
        return priority;
    }

    /*
     * @param methodGen method
     * @param start instruction to scan
     * @return instruction which consumes value which was on top of stack before start instruction
     * or null if cannot be determined
     */
    private InstructionHandle getConsumer(MethodGen methodGen, InstructionHandle start) {
        int depth = 1;
        InstructionHandle cur = start;
        while (cur != null) {
            Instruction inst = cur.getInstruction();
            depth -= inst.consumeStack(methodGen.getConstantPool());
            if (depth <= 0) {
                return cur;
            }
            depth += inst.produceStack(methodGen.getConstantPool());
            if (inst instanceof BranchInstruction) {
                if (inst instanceof GotoInstruction) {
                    cur = ((GotoInstruction) inst).getTarget();
                    continue;
                }
                if (!(inst instanceof IfInstruction)) {
                    return null;
                }
            }
            cur = cur.getNext();
        }
        return null;
    }

    private int getIntValue(InstructionHandle handle) {
        Instruction instruction = handle.getInstruction();
        if (instruction instanceof ICONST) {
            return ((ICONST) instruction).getValue().intValue();
        }
        return -1;
    }

    @Override
    public void report() {
    }
}
