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

import java.util.BitSet;
import edu.umd.cs.findbugs.*;
import edu.umd.cs.daveho.ba.*;
import org.apache.bcel.Constants;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

public class FindRefComparison implements Detector {
	private static final boolean DEBUG = Boolean.getBoolean("frc.debug");

	private BugReporter bugReporter;

	public FindRefComparison(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void visitClassContext(ClassContext classContext) {
		try {

			JavaClass jclass = classContext.getJavaClass();
			Method[] methodList = jclass.getMethods();

			for (int i = 0; i < methodList.length; ++i) {
				Method method = methodList[i];
				MethodGen methodGen = classContext.getMethodGen(method);
				if (methodGen == null)
					continue;

				// Prescreening - must have IF_ACMPEQ or IF_ACMPNE
				BitSet bytecodeSet = classContext.getBytecodeSet(method);
				if (!(bytecodeSet.get(Constants.IF_ACMPEQ) || bytecodeSet.get(Constants.IF_ACMPNE)))
					continue;

				if (DEBUG) System.out.println("FindRefComparison: analyzing " +
					SignatureConverter.convertMethodSignature(methodGen));

				final CFG cfg = classContext.getCFG(method);
				final TypeDataflow typeDataflow = classContext.getTypeDataflow(method);

				new LocationScanner(cfg).scan(new LocationScanner.Callback() {
					public void visitLocation(Location location) {
						InstructionHandle handle = location.getHandle();
						Instruction ins = handle.getInstruction();
						short opcode = ins.getOpcode();
						if (opcode == Constants.IF_ACMPEQ || opcode == Constants.IF_ACMPNE) {
							TypeFrame frame = typeDataflow.getFactAtLocation(location);
							if (frame.getStackDepth() < 2)
								throw new AnalysisException("Stack underflow at " + handle);
							int numSlots = frame.getNumSlots();
							Type op1 = frame.getValue(numSlots - 1);
							Type op2 = frame.getValue(numSlots - 2);

							if (op1 instanceof ObjectType && op2 instanceof ObjectType) {
								ObjectType ot1 = (ObjectType) op1;
								ObjectType ot2 = (ObjectType) op2;

								if (ot1.getClassName().equals("java.lang.String") &&
									ot2.getClassName().equals("java.lang.String")) {
									System.out.println("String/String comparison!");
									// TODO: report the bug
								}
							}
						}
					}
				});
			}

		} catch (DataflowAnalysisException e) {
			throw new AnalysisException("Exception in FindRefComparison: " + e.getMessage(), e);
		} catch (CFGBuilderException e) {
			throw new AnalysisException("Exception in FindRefComparison: " + e.getMessage(), e);
		}
	}

	public void report() {
	}
}

// vim:ts=4
