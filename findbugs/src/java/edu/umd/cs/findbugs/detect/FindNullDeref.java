/*
 * FindBugs - Find bugs in Java programs
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

package edu.umd.cs.findbugs.detect;

import java.util.*;
import org.apache.bcel.Constants;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import edu.umd.cs.daveho.ba.*;
import edu.umd.cs.findbugs.*;

/**
 * A Detector to find instructions where a NullPointerException
 * might be raised.  We also look for useless reference comparisons
 * involving null and non-null values.
 *
 * @see IsNullValueAnalysis
 * @author David Hovemeyer
 */
public class FindNullDeref implements Detector {

	private static final boolean DEBUG = Boolean.getBoolean("fnd.debug");

	private BugReporter bugReporter;

	public FindNullDeref(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void visitClassContext(ClassContext classContext) {
		try {

			JavaClass jclass = classContext.getJavaClass();
			Method[] methodList = jclass.getMethods();
			for (int i = 0; i < methodList.length; ++i) {
				Method method = methodList[i];
				if (method.isAbstract() || method.isNative() || method.getCode() == null)
					continue;

				if (DEBUG) System.out.println(SignatureConverter.convertMethodSignature(jclass.getClassName(), method.getName(), method.getSignature()));

				// Get the IsNullValueAnalysis for the method from the ClassContext
				IsNullValueDataflow invDataflow = classContext.getIsNullValueDataflow(method);

				// Look for null check blocks where the reference being checked
				// is definitely null, or null on some path
				Iterator<BasicBlock> bbIter = invDataflow.getCFG().blockIterator();
				while (bbIter.hasNext()) {
					BasicBlock basicBlock = bbIter.next();

					if (basicBlock.isNullCheck()) {
						// Look for null checks where the value checked is definitely
						// null or null on some path.

						InstructionHandle exceptionThrowerHandle = basicBlock.getExceptionThrower();
						Instruction exceptionThrower = exceptionThrowerHandle.getInstruction();

						// Figure out where the reference operand is in the stack frame.
						int consumed = exceptionThrower.consumeStack(classContext.getConstantPoolGen());
						if (consumed == Constants.UNPREDICTABLE)
							throw new DataflowAnalysisException("Unpredictable stack consumption for " + exceptionThrower);

						// Get the stack values at entry to the null check.
						IsNullValueFrame frame = invDataflow.getStartFact(basicBlock);

						// Could the reference be null?
						IsNullValue refValue = frame.getValue(frame.getNumSlots() - consumed);

						boolean onExceptionPath = refValue.isException();
						if (refValue.isDefinitelyNull()) {
							String type = onExceptionPath ? "NP_ALWAYS_NULL_EXCEPTION" : "NP_ALWAYS_NULL";
							int priority = onExceptionPath ? LOW_PRIORITY : HIGH_PRIORITY;
							reportNullDeref(classContext, method, exceptionThrowerHandle, type, priority);
						} else if (refValue.isNullOnSomePath()) {
							String type = onExceptionPath ? "NP_NULL_ON_SOME_PATH_EXCEPTION" : "NP_NULL_ON_SOME_PATH";
							int priority = onExceptionPath ? LOW_PRIORITY : NORMAL_PRIORITY;
							reportNullDeref(classContext, method, exceptionThrowerHandle, type, priority);
						}
					} else if (!basicBlock.isEmpty()) {
						// Look for all reference comparisons where
						//    - both values compared are definitely null, or
						//    - one value is definitely null and one is definitely not null
						// These cases are not null dereferences,
						// but they are quite likely to indicate an error, so while we've got
						// information about null values, we may as well report them.
						InstructionHandle lastHandle = basicBlock.getLastInstruction();
						Instruction last = lastHandle.getInstruction();
						short opcode = last.getOpcode();
						if (opcode == Constants.IF_ACMPEQ || opcode == Constants.IF_ACMPNE) {
							IsNullValueFrame frame = invDataflow.getFactAtLocation(new Location(lastHandle, basicBlock));
							if (frame.getStackDepth() < 2)
								throw new AnalysisException("Stack underflow at " + lastHandle);
							int numSlots = frame.getNumSlots();
							IsNullValue top = frame.getValue(numSlots - 1);
							IsNullValue topNext = frame.getValue(numSlots - 2);
							if ((top.isDefinitelyNull() && topNext.isDefinitelyNull()) ||
								(top.isDefinitelyNull() && topNext.isDefinitelyNotNull()) ||
								(top.isDefinitelyNotNull() && topNext.isDefinitelyNull())) {
								reportUselessControlFlow(classContext, method, lastHandle);
							}
						} else if (opcode == Constants.IFNULL || opcode == Constants.IFNONNULL) {
							IsNullValueFrame frame = invDataflow.getFactAtLocation(new Location(lastHandle, basicBlock));
							IsNullValue top = frame.getTopValue();
							if (top.isDefinitelyNull() || top.isDefinitelyNotNull()) {
								reportUselessControlFlow(classContext, method, lastHandle);
							}
						}
					}
				}
			}

		} catch (DataflowAnalysisException e) {
			throw new AnalysisException("FindNullDeref caught exception", e);
		} catch (CFGBuilderException e) {
			throw new AnalysisException(e.getMessage());
		}
	}

	private void reportNullDeref(ClassContext classContext, Method method, InstructionHandle exceptionThrowerHandle,
		String type, int priority) {
		MethodGen methodGen = classContext.getMethodGen(method);
		String sourceFile = classContext.getJavaClass().getSourceFileName();

		bugReporter.reportBug(new BugInstance(type, priority)
			.addClassAndMethod(methodGen, sourceFile)
			.addSourceLine(methodGen, sourceFile, exceptionThrowerHandle)
			//.addInt(exceptionThrowerHandle.getPosition()).describe("INT_BYTECODE_OFFSET")
		);
	}

	private void reportUselessControlFlow(ClassContext classContext, Method method, InstructionHandle handle) {
		String sourceFile = classContext.getJavaClass().getSourceFileName();
		MethodGen methodGen = classContext.getMethodGen(method);

		bugReporter.reportBug(new BugInstance("UCF_USELESS_NULL_REF_COMPARISON", NORMAL_PRIORITY)
			.addClassAndMethod(methodGen, sourceFile)
			.addSourceLine(methodGen, sourceFile, handle));
	}

	public void report() {
	}

}

// vim:ts=4
