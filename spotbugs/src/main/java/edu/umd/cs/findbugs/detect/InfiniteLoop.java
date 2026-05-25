/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2006 University of Maryland
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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import jakarta.annotation.Nonnull;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.visitclass.Util;

public class InfiniteLoop extends OpcodeStackDetector {

    //    private static final boolean active = true;

    ArrayList<BitSet> regModifiedAt = new ArrayList<>();

    @Nonnull
    BitSet getModifiedBitSet(int reg) {
        while (regModifiedAt.size() <= reg) {
            regModifiedAt.add(new BitSet());
        }
        return regModifiedAt.get(reg);
    }

    private void regModifiedAt(int reg, int pc) {
        BitSet b = getModifiedBitSet(reg);
        b.set(pc);
    }

    private void clearRegModified() {
        for (BitSet b : regModifiedAt) {
            b.clear();
        }
    }

    private boolean isRegModified(int reg, int firstPC, int lastPC) {
        if (reg < 0) {
            return false;
        }
        BitSet b = getModifiedBitSet(reg);
        int modified = b.nextSetBit(firstPC);
        return (modified >= firstPC && modified <= lastPC);
    }

    static class Jump {
        final int from;
        final int to;

        Jump(int from, int to) {
            this.from = from;
            this.to = to;
        }

        @Override
        public String toString() {
            return from + " -> " + to;
        }

        @Override
        public int hashCode() {
            return from * 37 + to;
        }

        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            if (this.getClass() != o.getClass()) {
                return false;
            }
            Jump that = (Jump) o;
            return this.from == that.from && this.to == that.to;
        }
    }

    static class BackwardsBranch extends Jump {
        final List<Integer> invariantRegisters = new LinkedList<>();

        final int numLastUpdates;

        BackwardsBranch(OpcodeStack stack, int from, int to) {
            super(from, to);
            numLastUpdates = stack.getNumLastUpdates();
            for (int i = 0; i < numLastUpdates; i++) {
                if (stack.getLastUpdate(i) < to) {
                    invariantRegisters.add(i);
                }
            }
        }

        @Override
        public int hashCode() {
            return 37 * super.hashCode() + 17 * invariantRegisters.hashCode() + numLastUpdates;
        }

        @Override
        public boolean equals(Object o) {
            if (!super.equals(o)) {
                return false;
            }
            BackwardsBranch that = (BackwardsBranch) o;
            return this.invariantRegisters.equals(that.invariantRegisters) && this.numLastUpdates == that.numLastUpdates;
        }
    }

    static class ForwardConditionalBranch extends Jump {
        final int opcode;
        final OpcodeStack.Item item0;
        final OpcodeStack.Item item1;

        ForwardConditionalBranch(int opcode, OpcodeStack.Item item0, OpcodeStack.Item item1, int from, int to) {
            super(from, to);
            this.opcode = opcode;
            this.item0 = item0;
            this.item1 = item1;
        }

        @Override
        public int hashCode() {
            return 37 * super.hashCode() + opcode + 17 * item0.hashCode() + item1.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (!super.equals(o)) {
                return false;
            }
            ForwardConditionalBranch that = (ForwardConditionalBranch) o;
            return this.opcode == that.opcode && this.item0.sameValue(that.item0) && this.item1.sameValue(that.item1);
        }

    }

    BugReporter bugReporter;

    HashSet<Jump> backwardReach = new HashSet<>();

    HashSet<BackwardsBranch> backwardBranches = new HashSet<>();

    HashSet<ForwardConditionalBranch> forwardConditionalBranches = new HashSet<>();

    LinkedList<Jump> forwardJumps = new LinkedList<>();

    void addForwardJump(int from, int to) {
        if (from >= to) {
            return;
        }
        forwardJumps.add(new Jump(from, to));
    }

    int getFurthestJump(int from) {
        int result = Integer.MIN_VALUE;
        int from2 = getBackwardsReach(from);
        assert from2 <= from;
        from = from2;
        for (Jump f : forwardJumps) {
            if (f.from >= from && f.to > result) {
                result = f.to;
            }
        }
        return result;
    }

    public InfiniteLoop(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visit(Code obj) {
        if (DEBUG) {
            System.out.println(getFullyQualifiedMethodName());
        }
        clearRegModified();
        backwardBranches.clear();
        forwardConditionalBranches.clear();
        forwardJumps.clear();
        backwardReach.clear();
        super.visit(obj);
        backwardBranchLoop: for (BackwardsBranch bb : backwardBranches) {
            LinkedList<ForwardConditionalBranch> myForwardBranches = new LinkedList<>();
            int myBackwardsReach = getBackwardsReach(bb.to);

            for (ForwardConditionalBranch fcb : forwardConditionalBranches) {
                if (myBackwardsReach < fcb.from && fcb.from < bb.from && bb.from < fcb.to) {
                    myForwardBranches.add(fcb);
                }
            }

            if (myForwardBranches.size() != 1) {
                continue;
            }
            ForwardConditionalBranch fcb = myForwardBranches.get(0);
            for (Jump fj : forwardJumps) {
                if (fcb.from != fj.from && myBackwardsReach < fj.from && fj.from < bb.from && bb.from < fj.to) {
                    continue backwardBranchLoop;
                }
            }

            if (isConstant(fcb.item0, bb) && isConstant(fcb.item1, bb)) {
                if (constantForwardBranchExitsLoop(fcb, bb)) {
                    continue backwardBranchLoop;
                }
                SourceLineAnnotation loopBottom = SourceLineAnnotation.fromVisitedInstruction(getClassContext(), this, bb.from);
                int loopBottomLine = loopBottom.getStartLine();
                SourceLineAnnotation loopTop = SourceLineAnnotation.fromVisitedInstruction(getClassContext(), this, bb.to);
                int loopTopLine = loopTop.getStartLine();
                BugInstance bug = new BugInstance(this, "IL_INFINITE_LOOP", HIGH_PRIORITY).addClassAndMethod(this)
                        .addSourceLine(this, fcb.from).addSourceLine(loopBottom)
                        .describe(SourceLineAnnotation.DESCRIPTION_LOOP_BOTTOM);
                int reg0 = fcb.item0.getRegisterNumber();
                boolean reg0Invariant = true;
                if (reg0 >= 0 && fcb.item0.getConstant() == null) {
                    reg0Invariant = !isRegModified(reg0, myBackwardsReach, bb.from);
                    SourceLineAnnotation lastChange = SourceLineAnnotation.fromVisitedInstruction(getClassContext(), this,
                            constantSince(fcb.item0));
                    int lastChangeLine = lastChange.getEndLine();
                    if (loopBottomLine != -1 && lastChangeLine != -1 && loopTopLine != -1 && loopTopLine <= lastChangeLine
                            && lastChangeLine < loopBottomLine) {
                        continue backwardBranchLoop;
                    }
                    bug.add(LocalVariableAnnotation.getLocalVariableAnnotation(getMethod(), reg0, fcb.from, bb.from))
                            .addSourceLine(lastChange).describe(SourceLineAnnotation.DESCRIPTION_LAST_CHANGE);
                }
                int reg1 = fcb.item1.getRegisterNumber();
                if (reg1 >= 0 && reg1 != reg0 && fcb.item1.getConstant() == null) {
                    SourceLineAnnotation lastChange = SourceLineAnnotation.fromVisitedInstruction(getClassContext(), this,
                            constantSince(fcb.item1));
                    int lastChangeLine = lastChange.getEndLine();
                    if (loopBottomLine != -1 && lastChangeLine != -1 && loopTopLine != -1 && loopTopLine <= lastChangeLine
                            && lastChangeLine < loopBottomLine) {
                        continue backwardBranchLoop;
                    }
                    bug.add(LocalVariableAnnotation.getLocalVariableAnnotation(getMethod(), reg1, fcb.from, bb.from))
                            .addSourceLine(lastChange).describe(SourceLineAnnotation.DESCRIPTION_LAST_CHANGE);
                }
                boolean reg1Invariant = true;
                if (reg1 >= 0) {
                    reg1Invariant = !isRegModified(reg1, myBackwardsReach, bb.from);
                }
                if (reg0Invariant && reg1Invariant) {
                    bugReporter.reportBug(bug);
                }
            }

        }
        if (DEBUG) {
            System.out.println();
        }

    }

    private boolean isConstant(Item item0, BackwardsBranch bb) {

        int reg = item0.getRegisterNumber();
        if (reg >= 0) {
            return bb.invariantRegisters.contains(reg) || reg >= bb.numLastUpdates;
        }
        return item0.getConstant() != null;
    }

    /**
     * Returns true when a constant forward branch leaves the loop on every iteration.
     */
    private boolean constantForwardBranchExitsLoop(ForwardConditionalBranch fcb, BackwardsBranch bb) {
        if (fcb.to <= bb.from || !exitsLoop(fcb.to, bb)) {
            return false;
        }
        return forwardBranchAlwaysTaken(fcb, bb);
    }

    private boolean exitsLoop(int pc, BackwardsBranch bb) {
        for (BackwardsBranch back : backwardBranches) {
            if (back.from >= pc && back.to == bb.to) {
                return false;
            }
        }
        return true;
    }

    private boolean forwardBranchAlwaysTaken(ForwardConditionalBranch fcb, BackwardsBranch bb) {
        Integer v0 = getKnownIntValue(fcb.item0, bb);
        Integer v1 = getKnownIntValue(fcb.item1, bb);
        switch (fcb.opcode) {
        case Const.IFEQ:
            return v0 != null && v0 == 0;
        case Const.IFNE:
            return v0 != null && v0 != 0;
        case Const.IFLT:
            return v0 != null && v0 < 0;
        case Const.IFGE:
            return v0 != null && v0 >= 0;
        case Const.IFGT:
            return v0 != null && v0 > 0;
        case Const.IFLE:
            return v0 != null && v0 <= 0;
        case Const.IF_ICMPEQ:
            return v0 != null && v1 != null && v0.intValue() == v1.intValue();
        case Const.IF_ICMPNE:
            return v0 != null && v1 != null && v0.intValue() != v1.intValue();
        case Const.IF_ICMPLT:
            return v0 != null && v1 != null && v0.intValue() < v1.intValue();
        case Const.IF_ICMPGE:
            return v0 != null && v1 != null && v0.intValue() >= v1.intValue();
        case Const.IF_ICMPGT:
            return v0 != null && v1 != null && v0.intValue() > v1.intValue();
        case Const.IF_ICMPLE:
            return v0 != null && v1 != null && v0.intValue() <= v1.intValue();
        default:
            return false;
        }
    }

    private Integer getKnownIntValue(Item item, BackwardsBranch bb) {
        Object constant = item.getConstant();
        if (constant instanceof Number) {
            return ((Number) constant).intValue();
        }
        int reg = item.getRegisterNumber();
        if (reg >= 0 && isConstant(item, bb)) {
            return getIntValueStoredInRegister(reg, bb.to);
        }
        return null;
    }

    private Integer getIntValueStoredInRegister(int reg, int beforePC) {
        BitSet modified = getModifiedBitSet(reg);
        int storePc = modified.nextSetBit(0);
        while (storePc >= 0 && storePc < beforePC) {
            Integer value = getIntConstantBeforeStore(storePc, reg);
            if (value != null) {
                return value;
            }
            storePc = modified.nextSetBit(storePc + 1);
        }
        return null;
    }

    private Integer getIntConstantBeforeStore(int storePc, int reg) {
        if (!isRegisterStoreAt(storePc, reg)) {
            return null;
        }
        byte[] code = getCode().getCode();
        int loadPc = storePc - 1;
        if (loadPc < 0) {
            return null;
        }
        int opcode = code[loadPc] & 0xFF;
        switch (opcode) {
        case Const.ICONST_M1:
            return -1;
        case Const.ICONST_0:
            return 0;
        case Const.ICONST_1:
            return 1;
        case Const.ICONST_2:
            return 2;
        case Const.ICONST_3:
            return 3;
        case Const.ICONST_4:
            return 4;
        case Const.ICONST_5:
            return 5;
        case Const.BIPUSH:
            return (int) code[loadPc + 1];
        case Const.SIPUSH:
            return (int) (short) ((code[loadPc + 1] << 8) | (code[loadPc + 2] & 0xFF));
        default:
            return null;
        }
    }

    private boolean isRegisterStoreAt(int pc, int reg) {
        byte[] code = getCode().getCode();
        if (pc < 0 || pc >= code.length) {
            return false;
        }
        int opcode = code[pc] & 0xFF;
        if (opcode >= Const.ISTORE_0 && opcode <= Const.ISTORE_3) {
            return opcode - Const.ISTORE_0 == reg;
        }
        if (opcode == Const.ISTORE) {
            return reg == (code[pc + 1] & 0xFF);
        }
        return false;
    }

    @Override
    public void sawBranchTo(int target) {
        addForwardJump(getPC(), target);
    }

    static final boolean DEBUG = false;

    @Override
    public void sawOpcode(int seen) {
        if (DEBUG) {
            System.out.printf("%3d %-15s %s%n", getPC(), Const.getOpcodeName(seen), stack);
        }
        if (isRegisterStore()) {
            regModifiedAt(getRegisterOperand(), getPC());
        }
        switch (seen) {
        case Const.GOTO:
            if (getBranchOffset() < 0) {
                BackwardsBranch bb = new BackwardsBranch(stack, getPC(), getBranchTarget());
                if (!bb.invariantRegisters.isEmpty()) {
                    backwardBranches.add(bb);
                }
                addBackwardsReach();
                /*
                if (false) {
                    int target = getBranchTarget();
                    if (getFurthestJump(target) > getPC())
                        break;
                    if (getMethodName().equals("run") || getMethodName().equals("main"))
                        break;
                    BugInstance bug = new BugInstance(this, "IL_INFINITE_LOOP", LOW_PRIORITY).addClassAndMethod(this)
                            .addSourceLine(this, getPC());
                    reportPossibleBug(bug);
                }
                 */
            }

            break;
        case Const.ARETURN:
        case Const.IRETURN:
        case Const.RETURN:
        case Const.DRETURN:
        case Const.FRETURN:
        case Const.LRETURN:
        case Const.ATHROW:
            addForwardJump(getPC(), Integer.MAX_VALUE);
            break;

        case Const.LOOKUPSWITCH:
        case Const.TABLESWITCH: {
            OpcodeStack.Item item0 = stack.getStackItem(0);
            if (getDefaultSwitchOffset() > 0) {
                forwardConditionalBranches.add(new ForwardConditionalBranch(seen, item0, item0, getPC(), getPC()
                        + getDefaultSwitchOffset()));
            }
            for (int offset : getSwitchOffsets()) {
                if (offset > 0) {
                    forwardConditionalBranches.add(new ForwardConditionalBranch(seen, item0, item0, getPC(), getPC() + offset));
                }
            }
            break;
        }
        case Const.IFNE:
        case Const.IFEQ:
        case Const.IFLE:
        case Const.IFLT:
        case Const.IFGE:
        case Const.IFGT:
        case Const.IFNONNULL:
        case Const.IFNULL: {
            addBackwardsReach();
            OpcodeStack.Item item0 = stack.getStackItem(0);
            int target = getBranchTarget();
            if (getBranchOffset() > 0) {
                forwardConditionalBranches.add(new ForwardConditionalBranch(seen, item0, item0, getPC(), target));
                break;
            }
            if (getFurthestJump(target) > getPC()) {
                break;
            }

            if (constantSince(item0, target)) {
                int since0 = constantSince(item0);
                BugInstance bug = new BugInstance(this, "IL_INFINITE_LOOP", HIGH_PRIORITY).addClassAndMethod(this).addSourceLine(
                        this, getPC());
                int reg0 = item0.getRegisterNumber();
                if (reg0 >= 0) {
                    bug.add(LocalVariableAnnotation.getLocalVariableAnnotation(getMethod(), reg0, getPC(), target))
                            .addSourceLine(this, since0);
                }
                if (reg0 < 0 || !isRegModified(reg0, target, getPC())) {
                    reportPossibleBug(bug);
                }

            }
        }
            break;
        case Const.IF_ACMPEQ:
        case Const.IF_ACMPNE:
        case Const.IF_ICMPNE:
        case Const.IF_ICMPEQ:
        case Const.IF_ICMPGT:
        case Const.IF_ICMPLE:
        case Const.IF_ICMPLT:
        case Const.IF_ICMPGE: {
            addBackwardsReach();
            OpcodeStack.Item item0 = stack.getStackItem(0);
            OpcodeStack.Item item1 = stack.getStackItem(1);
            int target = getBranchTarget();
            if (getBranchOffset() > 0) {
                forwardConditionalBranches.add(new ForwardConditionalBranch(seen, item0, item1, getPC(), target));
                break;
            }
            if (getFurthestJump(target) > getPC()) {
                break;
            }

            if (constantSince(item0, target) && constantSince(item1, target)) {
                // int since0 = constantSince(item0);
                // int since1 = constantSince(item1);
                BugInstance bug = new BugInstance(this, "IL_INFINITE_LOOP", HIGH_PRIORITY).addClassAndMethod(this).addSourceLine(
                        this, getPC());
                int reg0 = item0.getRegisterNumber();
                if (reg0 >= 0) {
                    bug.add(LocalVariableAnnotation.getLocalVariableAnnotation(getMethod(), reg0, getPC(), target));
                }
                int reg1 = item1.getRegisterNumber();
                if (reg1 >= 0) {
                    bug.add(LocalVariableAnnotation.getLocalVariableAnnotation(getMethod(), reg1, getPC(), target));
                }

                reportPossibleBug(bug);
            }

        }
            break;
        default:
            break;
        }

    }

    /**
     *
     */
    private void addBackwardsReach() {
        if (getBranchOffset() >= 0) {
            return;
        }
        int target = getBranchTarget();
        for (Jump j : backwardReach) {
            if (j.to < target && target <= j.from) {
                target = j.to;
            }
        }
        assert target <= getBranchTarget();
        assert target < getPC();
        for (Iterator<Jump> i = backwardReach.iterator(); i.hasNext();) {
            Jump j = i.next();
            if (target <= j.to && getPC() >= j.from) {
                i.remove();
            }
        }
        backwardReach.add(new Jump(getPC(), target));
    }

    private int getBackwardsReach(int target) {
        int originalTarget = target;
        for (Jump j : backwardReach) {
            if (j.to < target && target <= j.from) {
                target = j.to;
            }
        }
        assert target <= originalTarget;
        return target;
    }

    private boolean constantSince(Item item1, int branchTarget) {
        int reg = item1.getRegisterNumber();
        if (reg >= 0) {
            return stack.getLastUpdate(reg) < getBackwardsReach(branchTarget);
        }
        return item1.getConstant() != null;
    }

    private int constantSince(Item item1) {
        int reg = item1.getRegisterNumber();
        if (reg >= 0) {
            return stack.getLastUpdate(reg);
        }
        return Integer.MAX_VALUE;

    }

    void reportPossibleBug(BugInstance bug) {
        int catchSize = Util.getSizeOfSurroundingTryBlock(getConstantPool(), getCode(), "java/io/EOFException", getPC());
        if (catchSize < Integer.MAX_VALUE) {
            bug.lowerPriorityALot();
        } else {
            catchSize = Util.getSizeOfSurroundingTryBlock(getConstantPool(), getCode(), "java/lang/NoSuchElementException",
                    getPC());
            if (catchSize < Integer.MAX_VALUE) {
                bug.lowerPriorityALot();
            } else {
                LocalVariableAnnotation lv = bug.getPrimaryLocalVariableAnnotation();
                if (lv == null && "run".equals(getMethodName())) {
                    bug.lowerPriority();
                }
            }
        }
        bugReporter.reportBug(bug);
    }
}
