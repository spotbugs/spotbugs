/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003, University of Maryland
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

package edu.umd.cs.daveho.ba;

import java.util.Arrays;
import org.apache.bcel.Constants;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * Analysis to determine where particular values are locked in a method.
 * The dataflow values are maps of value numbers to the number of times
 * those values are locked.
 *
 * @see ValueNumberAnalysis
 * @author David Hovemeyer
 */
public class LockAnalysis extends ForwardDataflowAnalysis<int[]> {
	private MethodGen methodGen;
	private ValueNumberDataflow vnaDataflow;
	private ValueNumberAnalysis vna;
	private boolean isSynchronized;
	private boolean isStatic;

	/** An uninitialized lock count. */
	public static final int TOP = -1;

	/** A lock count resulting from a flow merge of conflicting lock counts. */
	public static final int BOTTOM = -2;

	public LockAnalysis(MethodGen methodGen, ValueNumberDataflow vnaDataflow) {
		this.methodGen = methodGen;
		this.vnaDataflow = vnaDataflow;
		this.vna = vnaDataflow.getAnalysis();
		this.isSynchronized = methodGen.isSynchronized();
		this.isStatic = methodGen.isStatic();
	}

	public int[] createFact() {
		int numValues = vna.getNumValuesAllocated();
		return new int[numValues];
	}

	public void copy(int[] source, int[] dest) {
		System.arraycopy(source, 0, dest, 0, source.length);
	}

	public void initEntryFact(int[] result) {
		// FIXME: we don't try to do anything for static methods at the moment,
		// because we don't yet have a way of figuring out when a Class object
		// is loaded as a value.

		Arrays.fill(result, 0);

		if (isSynchronized && !isStatic) {
			ValueNumber thisValue = vna.getThisValue();
			result[thisValue.getNumber()] = 1;
		}
	}

	public void initResultFact(int[] result) {
		Arrays.fill(result, TOP);
	}

	public void makeFactTop(int[] fact) {
		Arrays.fill(fact, TOP);
	}

	public boolean same(int[] fact1, int[] fact2) {
		return Arrays.equals(fact1, fact2);
	}

	public void meetInto(int[] fact, Edge edge, int[] result) throws DataflowAnalysisException {
		for (int i = 0; i < fact.length; ++i)
			result[i] = mergeValues(result[i], fact[i]);
	}

	private static int mergeValues(int a, int b) {
		if (a == TOP)
			return b;
		else if (b == TOP)
			return a;
		else if (a == BOTTOM || b == BOTTOM)
			return BOTTOM;
		else if (a == b)
			return a;
		else
			return BOTTOM;
	}

	public void transferInstruction(InstructionHandle handle, BasicBlock basicBlock, int[] fact)
		throws DataflowAnalysisException {

		Instruction ins = handle.getInstruction();
		short opcode = ins.getOpcode();
		if (opcode == Constants.MONITORENTER || opcode == Constants.MONITOREXIT) {
			ValueNumberFrame frame = vnaDataflow.getFactAtLocation(new Location(handle, basicBlock));
			int lockNumber = frame.getTopValue().getNumber();
			lockOp(fact, lockNumber, opcode == Constants.MONITORENTER ? 1 : -1);
		} else if ((ins instanceof ReturnInstruction) && isSynchronized && !isStatic) {
			lockOp(fact, vna.getThisValue().getNumber(), -1);
		}
	}

	private static void lockOp(int[] fact, int lockNumber, int delta) {
		int value = fact[lockNumber];
		if (value < 0) // can't modify TOP or BOTTOM value
			return;
		value += delta;
		if (value < 0)
			value = BOTTOM;
		fact[lockNumber] = value;
	}

	public boolean isFactValid(int[] fact) {
		return true;
	}

	public String factToString(int[] fact) {
		StringBuffer buf = new StringBuffer();
		buf.append('[');
		boolean first = true;
		for (int i = 0; i < fact.length; ++i) {
			int value = fact[i]; 
			if (value == 0)
				continue;
			if (first)
				first = false;
			else
				buf.append(',');
			buf.append(i);
			buf.append('=');
			if (value == TOP)
				buf.append("TOP");
			else if (value == BOTTOM)
				buf.append("BOTTOM");
			else
				buf.append(value);
		}
		buf.append(']');
		return buf.toString();
	}

	public static void main(String[] argv) throws Exception {
		if (argv.length != 1) {
			System.err.println("Usage: " + LockAnalysis.class.getName() + " <classfile>");
			System.exit(1);
		}

		DataflowTestDriver<int[], LockAnalysis> driver = new DataflowTestDriver<int[], LockAnalysis>() {
			public LockAnalysis createAnalysis(MethodGen methodGen, CFG cfg) throws DataflowAnalysisException {
				ValueNumberAnalysis vna = new ValueNumberAnalysis(methodGen);
				ValueNumberDataflow vnaDataflow = new ValueNumberDataflow(cfg, vna);
				vnaDataflow.execute();

				return new LockAnalysis(methodGen, vnaDataflow);
			}
		};

		driver.execute(argv[0]);
	}
}

// vim:ts=4
