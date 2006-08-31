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
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ReturnInstruction;

import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberAnalysis;
import edu.umd.cs.findbugs.ba.vna.ValueNumberDataflow;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

/**
 * Analysis to determine where particular values are locked in a method.
 * The dataflow values are maps of value numbers to the number of times
 * those values are locked.
 *
 * @author David Hovemeyer
 * @see ValueNumberAnalysis
 */
public class LockAnalysis extends ForwardDataflowAnalysis<LockSet> {
	private static final boolean DEBUG = SystemProperties.getBoolean("la.debug");

	private MethodGen methodGen;
	private ValueNumberDataflow vnaDataflow;
	private ValueNumberAnalysis vna;
	private boolean isSynchronized;
	private boolean isStatic;

	public LockAnalysis(MethodGen methodGen, ValueNumberDataflow vnaDataflow, DepthFirstSearch dfs) {
		super(dfs);
		this.methodGen = methodGen;
		this.vnaDataflow = vnaDataflow;
		this.vna = vnaDataflow.getAnalysis();
		this.isSynchronized = methodGen.isSynchronized();
		this.isStatic = methodGen.isStatic();
		if (DEBUG) System.out.println("Analyzing Locks in " +  methodGen.getClassName() + "." + methodGen.getName());
	}

	public LockSet createFact() {
		return new LockSet();
	}

	public void copy(LockSet source, LockSet dest) {
		dest.copyFrom(source);
	}

	public void initEntryFact(LockSet result) {
		// FIXME: we don't try to do anything for static methods at the moment,
		// because we don't yet have a way of figuring out when a Class object
		// is loaded as a value.
 
		result.clear();
		result.setDefaultLockCount(0);

		if (isSynchronized && !isStatic) {
			ValueNumber thisValue = vna.getThisValue();
			result.setLockCount(thisValue.getNumber(), 1);
		}
	}

	public void initResultFact(LockSet result) {
		result.clear();
		result.setDefaultLockCount(LockSet.TOP);
	}

	public void makeFactTop(LockSet fact) {
		fact.clear();
		fact.setDefaultLockCount(LockSet.TOP);
	}
	public boolean isTop(LockSet fact) {
		return fact.isTop();
	}
	public boolean same(LockSet fact1, LockSet fact2) {
		return fact1.sameAs(fact2);
	}

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

			// NOTE: if the CFG is pruned, there may be unreachable instructions,
			// so make sure frame is valid.
			if (frame.isValid()) {
				int lockNumber = frame.getTopValue().getNumber();
				lockOp(fact, lockNumber, opcode == Constants.MONITORENTER ? 1 : -1);
			}
		} else if ((ins instanceof ReturnInstruction) && isSynchronized && !isStatic) {
			lockOp(fact, vna.getThisValue().getNumber(), -1);
		}
	}

	private  void lockOp(LockSet fact, int lockNumber, int delta) {
		int value = fact.getLockCount(lockNumber);
		if (value < 0) // can't modify TOP or BOTTOM value
			return;
		value += delta;
		if (value < 0)
			value = LockSet.BOTTOM;
		if (DEBUG) System.out.println("Setting " + lockNumber + " to " + value + " in " + methodGen.getClassName() + "." + methodGen.getName());
		fact.setLockCount(lockNumber, value);
	}

	@Override
         public boolean isFactValid(LockSet fact) {
		return true;
	}

	public static void main(String[] argv) throws Exception {
		if (argv.length != 1) {
			System.err.println("Usage: " + LockAnalysis.class.getName() + " <classfile>");
			System.exit(1);
		}

		DataflowTestDriver<LockSet, LockAnalysis> driver = new DataflowTestDriver<LockSet, LockAnalysis>() {
			@Override
                         public Dataflow<LockSet, LockAnalysis> createDataflow(ClassContext classContext, Method method)
			        throws CFGBuilderException, DataflowAnalysisException {
				return classContext.getLockDataflow(method);
			}
		};

		driver.execute(argv[0]);
	}
}

// vim:ts=4
