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
				if (method.isAbstract() || method.isNative())
					continue;

				if (DEBUG) System.out.println(method.getName());

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

						if (refValue.equals(IsNullValue.nullValue()))
							reportNullDeref(classContext, method, exceptionThrowerHandle, "NP_ALWAYS_NULL");
						else if (refValue.equals(IsNullValue.nullOnSomePathValue()))
							reportNullDeref(classContext, method, exceptionThrowerHandle, "NP_NULL_ON_SOME_PATH");
					}
				}
			}

		} catch (DataflowAnalysisException e) {
			throw new AnalysisException("FindNullDeref caught exception", e);
		}
	}

	private void reportNullDeref(ClassContext classContext, Method method, InstructionHandle exceptionThrowerHandle, String type) {
		MethodGen methodGen = classContext.getMethodGen(method);

		bugReporter.reportBug(new BugInstance(type, NORMAL_PRIORITY)
			.addClassAndMethod(methodGen)
			.addSourceLine(methodGen, exceptionThrowerHandle)
		);
	}

	public void report() {
	}

}

// vim:ts=4
