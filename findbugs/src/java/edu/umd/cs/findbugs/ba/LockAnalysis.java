/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003,2004 University of Maryland
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

package edu.umd.cs.findbugs.ba;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ReturnInstruction;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberAnalysis;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

/**
 * Analysis to determine where particular values are locked in a method. The
 * dataflow values are maps of value numbers to the number of times those values
 * are locked.
 *
 * @author David Hovemeyer
 * @see ValueNumberAnalysis
 */
public class LockAnalysis extends ForwardDataflowAnalysis<LockSet> {
    private static final boolean DEBUG = SystemProperties.getBoolean("la.debug");

    private final MethodGen methodGen;

    private final ValueNumberDataflow vnaDataflow;

    private final ValueNumberAnalysis vna;

    private final boolean isSynchronized;

    private final boolean isStatic;

    public LockAnalysis(MethodGen methodGen, ValueNumberDataflow vnaDataflow, DepthFirstSearch dfs) {
        super(dfs);
        this.methodGen = methodGen;
        this.vnaDataflow = vnaDataflow;
        this.vna = vnaDataflow.getAnalysis();
        this.isSynchronized = methodGen.isSynchronized();
        this.isStatic = methodGen.isStatic();
        if (DEBUG) {
            System.out.println("Analyzing Locks in " + methodGen.getClassName() + "." + methodGen.getName());
        }
    }

    @Override
    public LockSet createFact() {
        return new LockSet();
    }

    @Override
    public void copy(LockSet source, LockSet dest) {
        dest.copyFrom(source);
    }

    @Override
    public void initEntryFact(LockSet result) {
        result.clear();
        result.setDefaultLockCount(0);

        if (isSynchronized && !isStatic) {
            ValueNumber thisValue = vna.getThisValue();
            result.setLockCount(thisValue.getNumber(), 1);
        } else if (isSynchronized && isStatic) {
            ValueNumber thisValue = vna.getClassObjectValue(methodGen.getClassName());
            result.setLockCount(thisValue.getNumber(), 1);
        }
    }

    @Override
    public void makeFactTop(LockSet fact) {
        fact.clear();
        fact.setDefaultLockCount(LockSet.TOP);
    }

    @Override
    public boolean isTop(LockSet fact) {
        return fact.isTop();
    }

    @Override
    public boolean same(LockSet fact1, LockSet fact2) {
        return fact1.sameAs(fact2);
    }

    @Override
    public void meetInto(LockSet fact, Edge edge, LockSet result) throws DataflowAnalysisException {
        result.meetWith(fact);
    }

    @Override
    public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, LockSet fact)
            throws DataflowAnalysisException {

        Instruction ins = handle.getInstruction();
        short opcode = ins.getOpcode();
        if (opcode == Constants.MONITORENTER || opcode == Constants.MONITOREXIT) {
            ValueNumberFrame frame = vnaDataflow.getFactAtLocation(new Location(handle, basicBlock));

            modifyLock(frame, fact, opcode == Constants.MONITORENTER ? 1 : -1);

        } else if (opcode == Constants.INVOKEVIRTUAL || opcode == Constants.INVOKEINTERFACE) {

            InvokeInstruction inv = (InvokeInstruction) ins;
            String name = inv.getMethodName(methodGen.getConstantPool());
            String sig = inv.getSignature(methodGen.getConstantPool());
            ValueNumberFrame frame = vnaDataflow.getFactAtLocation(new Location(handle, basicBlock));

            if ("()V".equals(sig) && ("lock".equals(name) || "lockInterruptibly".equals(name))) {
                modifyLock(frame, fact, 1);
            } else if ("()V".equals(sig) && ("unlock".equals(name))) {
                modifyLock(frame, fact, -1);
            }

        } else if ((ins instanceof ReturnInstruction) && isSynchronized && !isStatic) {

            lockOp(fact, vna.getThisValue().getNumber(), -1);
        }
    }

    private void modifyLock(ValueNumberFrame frame, LockSet fact, int delta) throws DataflowAnalysisException {
        if (frame.isValid()) {
            int lockNumber = frame.getTopValue().getNumber();
            lockOp(fact, lockNumber, delta);
        }
    }

    private void lockOp(LockSet fact, int lockNumber, int delta) {
        int value = fact.getLockCount(lockNumber);
        if (value < 0) {
            return;
        }
        value += delta;
        if (value < 0) {
            value = LockSet.BOTTOM;
        }
        if (DEBUG) {
            System.out.println("Setting " + lockNumber + " to " + value + " in " + methodGen.getClassName() + "."
                    + methodGen.getName());
        }
        fact.setLockCount(lockNumber, value);
    }

    @Override
    public boolean isFactValid(LockSet fact) {
        return true;
    }

    // public static void main(String[] argv) throws Exception {
    // if (argv.length != 1) {
    // System.err.println("Usage: " + LockAnalysis.class.getName() +
    // " <classfile>");
    // System.exit(1);
    // }
    //
    // DataflowTestDriver<LockSet, LockAnalysis> driver = new
    // DataflowTestDriver<LockSet, LockAnalysis>() {
    // @Override
    // public Dataflow<LockSet, LockAnalysis> createDataflow(ClassContext
    // classContext, Method method)
    // throws CFGBuilderException, DataflowAnalysisException {
    // return classContext.getLockDataflow(method);
    // }
    // };
    //
    // driver.execute(argv[0]);
    // }
}

