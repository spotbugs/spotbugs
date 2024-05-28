/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005 Dave Brosius
 * Copyright (C) 2005 University of Maryland
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
package edu.umd.cs.findbugs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.CheckForNull;

import org.apache.bcel.Const;
import org.apache.bcel.generic.ASTORE;
import org.apache.bcel.generic.CHECKCAST;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.visitclass.DismantleBytecode;

public class SwitchHandler {
    private final List<SwitchDetails> switchOffsetStack;
    /**
     * The set of program counters for the 'switch' instruction of each of the type switches
     */
    private final Set<Integer> typeSwitchPC;

    public SwitchHandler() {
        switchOffsetStack = new ArrayList<>();
        typeSwitchPC = new HashSet<>();
    }

    public int stackSize() {
        return switchOffsetStack.size();
    }

    int numEnumValues(@CheckForNull XClass c) {
        if (c == null) {
            return -1;
        }
        int total = 0;
        for (XField f : c.getXFields()) {
            if (f.isEnum()) {
                total++;
            }
        }
        return total;
    }

    public void enterSwitch(DismantleBytecode dbc, @CheckForNull XClass enumType) {
        int[] switchOffsets = dbc.getSwitchOffsets();
        enterSwitch(dbc.getOpcode(), dbc.getPC(), switchOffsets, dbc.getDefaultSwitchOffset(), switchOffsets.length == numEnumValues(enumType));
    }

    /**
     * @param opCode The op code of the switch, should be <code>TABLESWITCH</code> or <code>LOOKUPSWITCH</code>
     * @param pc The PC of the switch instruction
     * @param switchOffsets The PC offsets of the switch cases
     * @param defaultSwitchOffset The PC of the default case
     * @param exhaustive <code>true</code> if the switch is exhaustive
     */
    public void enterSwitch(int opCode, int pc, int[] switchOffsets, int defaultSwitchOffset, boolean exhaustive) {
        assert opCode == Const.TABLESWITCH || opCode == Const.LOOKUPSWITCH;
        SwitchDetails details = new SwitchDetails(pc, switchOffsets, defaultSwitchOffset, exhaustive);


        int size = switchOffsetStack.size();
        while (--size >= 0) {
            SwitchDetails existingDetail = switchOffsetStack.get(size);
            if (details.switchPC > (existingDetail.switchPC + existingDetail.getLastOffset())) {
                switchOffsetStack.remove(size);
            }
        }
        switchOffsetStack.add(details);
    }

    public boolean isOnSwitchOffset(DismantleBytecode dbc) {
        int pc = dbc.getPC();
        if (pc == getDefaultOffset()) {
            return false;
        }

        return (pc == getNextSwitchOffset(dbc));
    }

    public int getNextSwitchOffset(DismantleBytecode dbc) {
        int size = switchOffsetStack.size();
        while (size > 0) {
            SwitchDetails details = switchOffsetStack.get(size - 1);

            int nextSwitchOffset = details.getNextSwitchOffset(dbc.getPC());
            if (nextSwitchOffset >= 0) {
                return nextSwitchOffset;
            }

            if (dbc.getPC() <= details.getDefaultOffset()) {
                return -1;
            }
            switchOffsetStack.remove(size - 1);
            size--;
        }

        return -1;
    }

    public int getDefaultOffset() {
        int size = switchOffsetStack.size();
        if (size == 0) {
            return -1;
        }

        SwitchDetails details = switchOffsetStack.get(size - 1);
        return details.getDefaultOffset();
    }

    public SourceLineAnnotation getCurrentSwitchStatement(BytecodeScanningDetector detector) {
        if (switchOffsetStack.isEmpty()) {
            throw new IllegalStateException("No current switch statement");
        }
        SwitchDetails details = switchOffsetStack.get(switchOffsetStack.size() - 1);
        return SourceLineAnnotation.fromVisitedInstructionRange(
                detector.getClassContext(), detector, details.switchPC, details.switchPC + details.maxOffset - 1);
    }

    /**
     * For type switches introduced in Java 21 we are using the invocation of a bootstrap 'typeswitch()' method to
     * detect that the switch operates on the class of the object.
     *
     * @param pc
     * @param methodName
     */
    public void sawInvokeDynamic(int pc, String methodName) {
        if ("typeSwitch".equals(methodName)) {
            typeSwitchPC.add(pc + 5);
        }
    }

    /**
     * In type switches a <code>CHECKCAST</code> is inserted by the compiler for each case. This method checks if the
     * instruction is one of these casts and then checks if the corresponding switch is a type switch.
     *
     * @param opCode The operation code
     * @param pc The program counter
     * @return <code>true</code>If this instruction is a cast for a type switch
     */
    public boolean isTypeSwitchCaseCheckCast(int opCode, int pc) {
        if (opCode != Const.CHECKCAST) {
            return false;
        }

        // For a type switch each case starts with an aload or an aload_<n> followed by a checkcast
        // aload_<n> does not have an operand so we're trying the previous pc (i.e. pc - 1)
        // aload takes and index operand so we're trying pc - 2
        // Since we're on a checkcast look for a switch with a target on the previous PC
        SwitchDetails switchDetails = findSwitchDetailsByPc(pc - 1, pc - 2);

        return switchDetails != null && typeSwitchPC.contains(switchDetails.switchPC);
    }

    /**
     * In type switches an <code>ASTORE</code> is inserted by the compiler for each case. This method checks if the
     * instruction is one of these loads and then checks if the corresponding switch is a type switch.
     * We're looking for: an ASTORE preceded by a CHECKCAST preceded by an instruction at the offset of a switch case
     *
     * @param location The Location
     * @return <code>true</code>If this instruction is a load for a type switch
     */
    public boolean isTypeSwitchCaseLoad(Location location) {
        Instruction ins = location.getHandle().getInstruction();
        if (!(ins instanceof ASTORE)) {
            return false;
        }

        BasicBlock block = location.getBasicBlock();
        InstructionHandle prev = block.getPredecessorOf(location.getHandle());

        if (prev == null) {
            return false;
        }

        if (!(prev.getInstruction() instanceof CHECKCAST)) {
            return false;
        }

        SwitchDetails switchDetails = findSwitchDetailsByPc(prev.getPosition() - 1, prev.getPosition() - 2);

        return switchDetails != null && typeSwitchPC.contains(switchDetails.switchPC);
    }

    /**
     * Finds a switch from the first PC of a case
     *
     * @param possiblePC The possible first PC of a switch case
     * @return The <code>SwitchDetails</code> of the switch corresponding to the case or null if there was no case at this PC
     */
    private SwitchDetails findSwitchDetailsByPc(int... possiblePC) {
        for (SwitchDetails switchDetails : switchOffsetStack) {
            for (int offset : switchDetails.swOffsets) {
                for (int pc : possiblePC) {
                    if (pc == offset + switchDetails.switchPC) {
                        return switchDetails;
                    }
                }
            }
        }

        return null;
    }

    public static class SwitchDetails {
        final int switchPC;

        final int[] swOffsets;

        final int defaultOffset;
        final int maxOffset;

        int nextOffset;

        final boolean exhaustive;

        public SwitchDetails(int pc, int[] offsets, int defOffset, boolean exhaustive) {
            switchPC = pc;
            int uniqueOffsets = 0;
            int lastValue = -1;
            int maxOffset = defOffset;
            for (int offset : offsets) {
                if (maxOffset < offset) {
                    maxOffset = offset;
                }
                if (offset == defOffset) {
                    exhaustive = false;
                }
                if (offset != lastValue) {
                    uniqueOffsets++;
                    lastValue = offset;
                }
            }

            this.maxOffset = maxOffset;
            swOffsets = new int[uniqueOffsets];
            int insertPos = 0;
            lastValue = -1;
            for (int offset1 : offsets) {
                if (offset1 != lastValue) {
                    swOffsets[insertPos++] = offset1;
                    lastValue = offset1;
                }
            }
            defaultOffset = defOffset;
            nextOffset = 0;
            this.exhaustive = exhaustive;
        }

        public int getNextSwitchOffset(int currentPC) {
            while ((nextOffset < swOffsets.length) && (currentPC > (switchPC + swOffsets[nextOffset]))) {
                nextOffset++;
            }

            if (nextOffset >= swOffsets.length) {
                return -1;
            }

            return switchPC + swOffsets[nextOffset];
        }

        public int getDefaultOffset() {
            if (exhaustive) {
                return Short.MIN_VALUE;
            }
            return switchPC + defaultOffset;
        }

        private int getLastOffset() {
            return swOffsets.length > 0 ? swOffsets[swOffsets.length - 1] : 0;
        }
    }
}
