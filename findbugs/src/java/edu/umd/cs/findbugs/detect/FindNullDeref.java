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

		bugReporter.reportBug(new BugInstance(type, priority)
			.addClassAndMethod(methodGen)
			.addSourceLine(methodGen, exceptionThrowerHandle)
			.addInt(exceptionThrowerHandle.getPosition()).describe("INT_BYTECODE_OFFSET")
		);
	}

	public void report() {
	}

}

// vim:ts=4
