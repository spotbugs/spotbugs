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

import java.util.*;

// We require BCEL 5.0 or later.
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * A dataflow analysis to track the production and flow of values in the Java
 * stack frame.  See the {@link ValueNumber ValueNumber} class for an explanation
 * of what the value numbers mean, and when they can be compared.
 *
 * <p> This class is still experimental.
 *
 * @see ValueNumber
 * @see DominatorsAnalysis
 * @author David Hovemeyer
 */
public class ValueNumberAnalysis extends ForwardDataflowAnalysis<ValueNumberFrame> {
	private MethodGen methodGen;
	private ValueNumberFactory factory;
	private ValueNumberCache cache;
	private ValueNumber[] entryLocalValueList;
	private IdentityHashMap<BasicBlock, ValueNumber> exceptionHandlerValueNumberMap;

	public ValueNumberAnalysis(MethodGen methodGen) {
		this.methodGen = methodGen;
		this.factory = new ValueNumberFactory();
		this.cache = new ValueNumberCache();

		int numLocals = methodGen.getMaxLocals();
		this.entryLocalValueList = new ValueNumber[numLocals];
		for (int i = 0; i < numLocals; ++i)
			this.entryLocalValueList[i] = factory.createFreshValue();

		this.exceptionHandlerValueNumberMap = new IdentityHashMap<BasicBlock, ValueNumber>();
	}

	public ValueNumber getEntryValue(int local) {
		return entryLocalValueList[local];
	}

	public ValueNumberFrame createFact() {
		return new ValueNumberFrame(methodGen.getMaxLocals(), factory);
	}

	public void copy(ValueNumberFrame source, ValueNumberFrame dest) {
		dest.copyFrom(source);
	}

	public void initEntryFact(ValueNumberFrame result) {
		// Change the frame from TOP to something valid.
		result.setValid();

		// At entry to the method, each local has (as far as we know) a unique value.
		int numSlots = result.getNumSlots();
		for (int i = 0; i < numSlots; ++i)
			result.setValue(i, entryLocalValueList[i]);
	}

	public void initResultFact(ValueNumberFrame result) {
		result.setTop();
	}

	public void makeFactTop(ValueNumberFrame fact) {
		fact.setTop();
	}

	public boolean isFactValid(ValueNumberFrame fact) {
		return fact.isValid();
	}

	public boolean same(ValueNumberFrame fact1, ValueNumberFrame fact2) {
		return fact1.sameAs(fact2);
	}

	public void transferInstruction(InstructionHandle handle, ValueNumberFrame fact) throws DataflowAnalysisException {
		ValueNumberFrameModelingVisitor visitor = new ValueNumberFrameModelingVisitor(fact, methodGen.getConstantPool(), factory, cache);
		Instruction ins = handle.getInstruction();
		ins.accept(visitor);
	}

	public void meetInto(ValueNumberFrame fact, Edge edge, ValueNumberFrame result) throws DataflowAnalysisException {
		if (edge.getDest().isExceptionHandler() && fact.isValid()) {
			// Special case: when merging predecessor facts for entry to
			// an exception handler, we clear the stack and push a
			// single entry for the exception object.  That way, the locals
			// can still be merged.

			// Get the value number for the exception
			BasicBlock handlerBlock = edge.getDest();
			ValueNumber exceptionValueNumber = getExceptionValueNumber(handlerBlock);

			// Set up the stack frame
			ValueNumberFrame tmpFact = createFact();
			tmpFact.copyFrom(fact);
			tmpFact.clearStack();
			tmpFact.pushValue(exceptionValueNumber);
			fact = tmpFact;
		}

		result.mergeWith(fact);
	}

	/**
	 * Test driver.
	 */
	public static void main(String[] argv) {
		try {
			if (argv.length != 1) {
				System.out.println("Usage: edu.umd.cs.daveho.ba.ValueNumberAnalysis <filename>");
				System.exit(1);
			}

			DataflowTestDriver<ValueNumberFrame> driver = new DataflowTestDriver<ValueNumberFrame>() {
				public AbstractDataflowAnalysis<ValueNumberFrame> createAnalysis(MethodGen methodGen, CFG cfg) {
					return new ValueNumberAnalysis(methodGen);
				}
			};

			driver.execute(argv[0]);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private ValueNumber getExceptionValueNumber(BasicBlock handlerBlock) {
		ValueNumber valueNumber = exceptionHandlerValueNumberMap.get(handlerBlock);
		if (valueNumber == null) {
			valueNumber = factory.createFreshValue();
			exceptionHandlerValueNumberMap.put(handlerBlock, valueNumber);
		}
		return valueNumber;
	}
}

// vim:ts=4
