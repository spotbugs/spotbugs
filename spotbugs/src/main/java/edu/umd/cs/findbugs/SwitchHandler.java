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

import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.visitclass.DismantleBytecode;

public class SwitchHandler {
    private final List<SwitchDetails> switchOffsetStack;
    private final Set<Integer> typeSwitchPcs;

    public SwitchHandler() {
        switchOffsetStack = new ArrayList<>();
        typeSwitchPcs = new HashSet<>();
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

    public void enterSwitch(int opCode, int pc, int[] switchOffsets, int defaultSwitchOffset, @CheckForNull boolean exhaustive) {
        assert opCode == Const.TABLESWITCH || opCode == Const.LOOKUPSWITCH;
        SwitchDetails details = new SwitchDetails(pc, switchOffsets, defaultSwitchOffset, exhaustive);


        int size = switchOffsetStack.size();
        while (--size >= 0) {
            SwitchDetails existingDetail = switchOffsetStack.get(size);
            if (details.switchPC > (existingDetail.switchPC + existingDetail.swOffsets[existingDetail.swOffsets.length - 1])) {
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

    public void sawInvokeDynamic(int pc, String methodName) {
        if ("typeSwitch".equals(methodName)) {
            typeSwitchPcs.add(pc + 5);
        }
    }

    /**
     * @param opCode The operation code
     * @param pc The program counter
     * @return <code>true</code>If this instruction is a cast for a type switch
     */
    public boolean isTypeSwitchCaseCheckCast(int opCode, int pc) {
        if (opCode != Const.CHECKCAST) {
            return false;
        }

        // For a type switch each case starts with an aload_2 followed by a checkcast
        // Since we're on a checkcast look for a switch with a target on the previous PC
        SwitchDetails switchDetails = findSwitchDetailsByPc(pc - 1);

        if (switchDetails != null) {
            return typeSwitchPcs.contains(switchDetails.switchPC);
        }

        return false;
    }

    private SwitchDetails findSwitchDetailsByPc(int pc) {
        for (SwitchDetails switchDetails : switchOffsetStack) {
            for (int i = 0; i < switchDetails.swOffsets.length; i++) {
                if (pc == switchDetails.swOffsets[i] + switchDetails.switchPC) {
                    return switchDetails;
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
    }
}
