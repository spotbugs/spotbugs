/*
 * SpotBugs - Find bugs in Java programs
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package edu.umd.cs.findbugs.detect;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.CheckForNull;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.IINC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.MethodUnprofitableException;
import edu.umd.cs.findbugs.ba.vna.MergeTree;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

public class MixArithmeticAndBitwiseOperationsDetector implements Detector {
    private enum OperationKind {
        ARITHMETIC("arithmetic"),
        BITWISE("bitwise");

        private final String description;

        OperationKind(String description) {
            this.description = description;
        }

        OperationKind opposite() {
            return this == ARITHMETIC ? BITWISE : ARITHMETIC;
        }
    }

    private final BugReporter bugReporter;

    public MixArithmeticAndBitwiseOperationsDetector(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visitClassContext(ClassContext classContext) {
        for (Method method : classContext.getJavaClass().getMethods()) {
            if (method.getCode() == null) {
                continue;
            }
            try {
                analyzeMethod(classContext, method);
            } catch (MethodUnprofitableException e) {
                if (SystemProperties.getBoolean("unprofitable.debug")) {
                    bugReporter.logError("Skipping unprofitable method in " + getClass().getName());
                }
            } catch (CFGBuilderException | DataflowAnalysisException e) {
                bugReporter.logError("Detector " + getClass().getName() + " caught exception", e);
            }
        }
    }

    private void analyzeMethod(ClassContext classContext, Method method)
            throws CFGBuilderException, DataflowAnalysisException {
        CFG cfg = classContext.getCFG(method);
        ValueNumberDataflow dataflow = classContext.getValueNumberDataflow(method);
        MergeTree mergeTree = dataflow.getAnalysis().getMergeTree();
        MethodGen methodGen = classContext.getMethodGen(method);
        String sourceFile = classContext.getJavaClass().getSourceFileName();
        Map<Integer, EnumSet<OperationKind>> usesByValue = new HashMap<>();
        BugAccumulator bugAccumulator = new BugAccumulator(bugReporter);

        List<Location> locations = new ArrayList<>();
        for (Iterator<Location> iterator = cfg.locationIterator(); iterator.hasNext();) {
            locations.add(iterator.next());
        }
        locations.sort(Comparator.comparingInt(location -> location.getHandle().getPosition()));

        for (Location location : locations) {
            ValueNumberFrame before = dataflow.getFactAtLocation(location);
            ValueNumberFrame after = dataflow.getFactAfterLocation(location);
            if (!before.isValid() || !after.isValid()) {
                continue;
            }

            Instruction instruction = location.getHandle().getInstruction();
            int opcode = instruction.getOpcode();
            if (isIntegerCast(opcode)) {
                propagateCast(before, after, mergeTree, usesByValue);
                continue;
            }

            OperationKind operation = getOperationKind(opcode);
            if (operation == null) {
                continue;
            }

            if (opcode == Const.IINC) {
                int slot = ((IINC) instruction).getIndex();
                checkOperation(classContext, methodGen, sourceFile, location, operation,
                        List.of(before.getValue(slot)), after.getValue(slot), mergeTree, usesByValue, bugAccumulator);
            } else {
                checkOperation(classContext, methodGen, sourceFile, location, operation,
                        getOperands(before, opcode), after.getTopValue(), mergeTree, usesByValue, bugAccumulator);
            }
        }

        bugAccumulator.reportAccumulatedBugs();
    }

    private void checkOperation(ClassContext classContext, MethodGen methodGen, String sourceFile, Location location,
            OperationKind operation, List<ValueNumber> operands, ValueNumber result, MergeTree mergeTree,
            Map<Integer, EnumSet<OperationKind>> usesByValue, BugAccumulator bugAccumulator) {
        EnumSet<OperationKind> previousUses = EnumSet.noneOf(OperationKind.class);
        for (ValueNumber operand : operands) {
            if (!isConstant(operand)) {
                previousUses.addAll(getUses(operand, mergeTree, usesByValue));
            }
        }

        if (previousUses.contains(operation.opposite())) {
            BugInstance bug = new BugInstance(this, "MABO_MIXING_ARITHMETIC_AND_BITWISE_OPERATIONS", NORMAL_PRIORITY)
                    .addClassAndMethod(methodGen, sourceFile)
                    .addString(operation.description)
                    .addString(operation.opposite().description);
            bugAccumulator.accumulateBug(bug, classContext, methodGen, sourceFile, location);
        }

        for (ValueNumber operand : operands) {
            if (!isConstant(operand)) {
                markUse(operand, operation, mergeTree, usesByValue);
            }
        }
        previousUses.add(operation);
        mergeUses(result.getNumber(), previousUses, usesByValue);
    }

    private static List<ValueNumber> getOperands(ValueNumberFrame frame, int opcode) throws DataflowAnalysisException {
        if (isUnaryArithmetic(opcode)) {
            return List.of(frame.getStackValue(0));
        }
        if (isBitwiseShift(opcode)) {
            // Stack values are indexed from the top. The shift distance is at 0 and the shifted value starts at 1.
            return List.of(frame.getStackValue(1));
        }

        // A long occupies two stack words, so the second long operand starts two slots below the top operand.
        int secondOperandOffset = isLongBinaryOperation(opcode) ? 2 : 1;
        return List.of(frame.getStackValue(0), frame.getStackValue(secondOperandOffset));
    }

    private static void propagateCast(ValueNumberFrame before, ValueNumberFrame after, MergeTree mergeTree,
            Map<Integer, EnumSet<OperationKind>> usesByValue) throws DataflowAnalysisException {
        ValueNumber input = before.getStackValue(0);
        if (!isConstant(input)) {
            mergeUses(after.getTopValue().getNumber(), getUses(input, mergeTree, usesByValue), usesByValue);
        }
    }

    private static EnumSet<OperationKind> getUses(ValueNumber value, MergeTree mergeTree,
            Map<Integer, EnumSet<OperationKind>> usesByValue) {
        EnumSet<OperationKind> result = EnumSet.noneOf(OperationKind.class);
        addUses(value.getNumber(), result, usesByValue);

        BitSet inputs = mergeTree.getTransitiveInputSet(value);
        for (int input = inputs.nextSetBit(0); input >= 0; input = inputs.nextSetBit(input + 1)) {
            addUses(input, result, usesByValue);
        }
        return result;
    }

    private static void markUse(ValueNumber value, OperationKind operation, MergeTree mergeTree,
            Map<Integer, EnumSet<OperationKind>> usesByValue) {
        mergeUses(value.getNumber(), EnumSet.of(operation), usesByValue);

        BitSet inputs = mergeTree.getTransitiveInputSet(value);
        for (int input = inputs.nextSetBit(0); input >= 0; input = inputs.nextSetBit(input + 1)) {
            mergeUses(input, EnumSet.of(operation), usesByValue);
        }
    }

    private static void addUses(int value, EnumSet<OperationKind> target,
            Map<Integer, EnumSet<OperationKind>> usesByValue) {
        Set<OperationKind> uses = usesByValue.get(value);
        if (uses != null) {
            target.addAll(uses);
        }
    }

    private static void mergeUses(int value, Set<OperationKind> uses,
            Map<Integer, EnumSet<OperationKind>> usesByValue) {
        usesByValue.computeIfAbsent(value, ignored -> EnumSet.noneOf(OperationKind.class)).addAll(uses);
    }

    private static boolean isConstant(ValueNumber value) {
        return value.hasFlag(ValueNumber.CONSTANT_VALUE) || value.hasFlag(ValueNumber.CONSTANT_CLASS_OBJECT);
    }

    private static boolean isIntegerCast(int opcode) {
        return opcode == Const.I2B || opcode == Const.I2C || opcode == Const.I2L || opcode == Const.I2S || opcode == Const.L2I;
    }

    private static @CheckForNull OperationKind getOperationKind(int opcode) {
        switch (opcode) {
        case Const.IINC:
        case Const.INEG:
        case Const.LNEG:
        case Const.IADD:
        case Const.ISUB:
        case Const.IMUL:
        case Const.IDIV:
        case Const.IREM:
        case Const.LADD:
        case Const.LSUB:
        case Const.LMUL:
        case Const.LDIV:
        case Const.LREM:
            return OperationKind.ARITHMETIC;

        case Const.IOR:
        case Const.IXOR:
        case Const.IAND:
        case Const.LOR:
        case Const.LXOR:
        case Const.LAND:
        case Const.ISHL:
        case Const.ISHR:
        case Const.IUSHR:
        case Const.LSHL:
        case Const.LSHR:
        case Const.LUSHR:
            return OperationKind.BITWISE;

        default:
            return null;
        }
    }

    private static boolean isUnaryArithmetic(int opcode) {
        return opcode == Const.INEG || opcode == Const.LNEG;
    }

    private static boolean isBitwiseShift(int opcode) {
        return opcode == Const.ISHL || opcode == Const.ISHR || opcode == Const.IUSHR || opcode == Const.LSHL
                || opcode == Const.LSHR || opcode == Const.LUSHR;
    }

    private static boolean isLongBinaryOperation(int opcode) {
        return opcode == Const.LADD || opcode == Const.LSUB || opcode == Const.LMUL || opcode == Const.LDIV
                || opcode == Const.LREM || opcode == Const.LOR || opcode == Const.LXOR || opcode == Const.LAND;
    }

    @Override
    public void report() {
    }
}
