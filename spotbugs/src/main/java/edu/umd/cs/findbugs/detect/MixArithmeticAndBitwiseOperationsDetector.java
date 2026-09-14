/*
 * SpotBugs - Find bugs in Java programs
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 */

package edu.umd.cs.findbugs.detect;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.IINC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.MethodGen;

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
    private static final int ARITHMETIC = 1;
    private static final int BITWISE = 2;

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
        Map<Integer, Integer> uses = new HashMap<>();

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
            if (isCastOpcode(opcode)) {
                propagateCast(before, after, mergeTree, uses);
            } else if (opcode == Const.IINC) {
                int slot = ((IINC) instruction).getIndex();
                checkOperation(classContext, methodGen, sourceFile, location, ARITHMETIC,
                        List.of(before.getValue(slot)), after.getValue(slot), mergeTree, uses);
            } else if (isArithmeticOpcode(opcode) || isBitwiseOpcode(opcode)) {
                int operation = isArithmeticOpcode(opcode) ? ARITHMETIC : BITWISE;
                checkOperation(classContext, methodGen, sourceFile, location, operation,
                        getOperands(before, opcode), after.getTopValue(), mergeTree, uses);
            }
        }
    }

    private void checkOperation(ClassContext classContext, MethodGen methodGen, String sourceFile, Location location,
            int operation, List<ValueNumber> operands, ValueNumber result, MergeTree mergeTree, Map<Integer, Integer> uses) {
        int previousUses = 0;
        for (ValueNumber operand : operands) {
            if (!isConstant(operand)) {
                previousUses |= getUses(operand, mergeTree, uses);
            }
        }

        int opposite = operation == ARITHMETIC ? BITWISE : ARITHMETIC;
        if ((previousUses & opposite) != 0) {
            String currentName = operation == ARITHMETIC ? "arithmetic" : "bitwise";
            String previousName = operation == ARITHMETIC ? "bitwise" : "arithmetic";
            bugReporter.reportBug(new BugInstance(this, "MABO_MIXING_ARITHMETIC_AND_BITWISE_OPERATIONS", NORMAL_PRIORITY)
                    .addClassAndMethod(methodGen, sourceFile)
                    .addString(currentName)
                    .addString(previousName)
                    .addSourceLine(classContext, methodGen, sourceFile, location.getHandle()));
        }

        for (ValueNumber operand : operands) {
            if (!isConstant(operand)) {
                markUses(operand, operation, mergeTree, uses);
            }
        }
        uses.merge(result.getNumber(), previousUses | operation, (left, right) -> left | right);
    }

    private static List<ValueNumber> getOperands(ValueNumberFrame frame, int opcode) throws DataflowAnalysisException {
        if (isUnaryArithmeticOpcode(opcode)) {
            return List.of(frame.getStackValue(0));
        }
        if (isBitwiseShiftOpcode(opcode)) {
            // The shift distance is the top stack value. It does not inherit the shifted value's use.
            return List.of(frame.getStackValue(1));
        }
        int secondOperandOffset = isLongBinaryOpcode(opcode) ? 2 : 1;
        return List.of(frame.getStackValue(0), frame.getStackValue(secondOperandOffset));
    }

    private static void propagateCast(ValueNumberFrame before, ValueNumberFrame after, MergeTree mergeTree,
            Map<Integer, Integer> uses) throws DataflowAnalysisException {
        ValueNumber input = before.getStackValue(0);
        ValueNumber output = after.getTopValue();
        if (!isConstant(input)) {
            uses.merge(output.getNumber(), getUses(input, mergeTree, uses), (left, right) -> left | right);
        }
    }

    private static int getUses(ValueNumber value, MergeTree mergeTree, Map<Integer, Integer> uses) {
        int result = uses.getOrDefault(value.getNumber(), 0);
        BitSet inputs = mergeTree.getTransitiveInputSet(value);
        for (int input = inputs.nextSetBit(0); input >= 0; input = inputs.nextSetBit(input + 1)) {
            result |= uses.getOrDefault(input, 0);
        }
        return result;
    }

    private static void markUses(ValueNumber value, int operation, MergeTree mergeTree, Map<Integer, Integer> uses) {
        uses.merge(value.getNumber(), operation, (left, right) -> left | right);
        BitSet inputs = mergeTree.getTransitiveInputSet(value);
        for (int input = inputs.nextSetBit(0); input >= 0; input = inputs.nextSetBit(input + 1)) {
            uses.merge(input, operation, (left, right) -> left | right);
        }
    }

    private static boolean isConstant(ValueNumber value) {
        return value.hasFlag(ValueNumber.CONSTANT_VALUE) || value.hasFlag(ValueNumber.CONSTANT_CLASS_OBJECT);
    }

    private static boolean isCastOpcode(int opcode) {
        return opcode == Const.I2B || opcode == Const.I2C || opcode == Const.I2L || opcode == Const.I2S || opcode == Const.L2I;
    }

    private static boolean isUnaryArithmeticOpcode(int opcode) {
        return opcode == Const.INEG || opcode == Const.LNEG;
    }

    private static boolean isArithmeticOpcode(int opcode) {
        return isUnaryArithmeticOpcode(opcode) || opcode == Const.IADD || opcode == Const.ISUB || opcode == Const.IMUL
                || opcode == Const.IDIV || opcode == Const.IREM || opcode == Const.LADD || opcode == Const.LSUB
                || opcode == Const.LMUL || opcode == Const.LDIV || opcode == Const.LREM;
    }

    private static boolean isBitwiseOpcode(int opcode) {
        return opcode == Const.IOR || opcode == Const.IXOR || opcode == Const.IAND || opcode == Const.LOR
                || opcode == Const.LXOR || opcode == Const.LAND || isBitwiseShiftOpcode(opcode);
    }

    private static boolean isBitwiseShiftOpcode(int opcode) {
        return opcode == Const.ISHL || opcode == Const.ISHR || opcode == Const.IUSHR || opcode == Const.LSHL
                || opcode == Const.LSHR || opcode == Const.LUSHR;
    }

    private static boolean isLongBinaryOpcode(int opcode) {
        return opcode == Const.LADD || opcode == Const.LSUB || opcode == Const.LMUL || opcode == Const.LDIV
                || opcode == Const.LREM || opcode == Const.LOR || opcode == Const.LXOR || opcode == Const.LAND;
    }

    @Override
    public void report() {
    }
}
