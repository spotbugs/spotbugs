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

			final JavaClass jclass = classContext.getJavaClass();
			Method[] methodList = jclass.getMethods();

			for (int i = 0; i < methodList.length; ++i) {
				Method method = methodList[i];
				final MethodGen methodGen = classContext.getMethodGen(method);
				if (methodGen == null)
					continue;

				// Prescreening - must have IF_ACMPEQ or IF_ACMPNE
				BitSet bytecodeSet = classContext.getBytecodeSet(method);
				if (!(bytecodeSet.get(Constants.IF_ACMPEQ) || bytecodeSet.get(Constants.IF_ACMPNE)))
					continue;

				if (DEBUG) System.out.println("FindRefComparison: analyzing " +
					SignatureConverter.convertMethodSignature(methodGen));

				// Scan for calls to String.intern().
				// If we find any, assume the programmer knew what he/she
				// was doing.
				if (callsIntern(methodGen))
					continue;

				final CFG cfg = classContext.getCFG(method);
				final TypeDataflow typeDataflow = classContext.getTypeDataflow(method);

				new LocationScanner(cfg).scan(new LocationScanner.Callback() {
					public void visitLocation(Location location) {
						try {
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
										//System.out.println("String/String comparison!");
	
										String sourceFile = jclass.getSourceFileName();
										bugReporter.reportBug(new BugInstance("RC_REF_COMPARISON", NORMAL_PRIORITY)
											.addClassAndMethod(methodGen, sourceFile)
											.addSourceLine(methodGen, sourceFile, handle)
											.addClass(ot1.getClassName()).describe("CLASS_REFTYPE")
										);
	
									}
								}
							}
						} catch (DataflowAnalysisException e) {
							throw new AnalysisException("Caught exception: " + e.toString(), e);
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

	private static boolean callsIntern(MethodGen methodGen) {
		InstructionHandle handle = methodGen.getInstructionList().getStart();
		while (handle != null) {
			Instruction ins = handle.getInstruction();
			short opcode = ins.getOpcode();
			if (opcode == Constants.INVOKEVIRTUAL) {
				INVOKEVIRTUAL inv = (INVOKEVIRTUAL) ins;
				ConstantPoolGen cpg = methodGen.getConstantPool();
				if (inv.getClassName(cpg).equals("java.lang.String") &&
					inv.getName(cpg).equals("intern") &&
					inv.getSignature(cpg).equals("()Ljava/lang/String;"))
					return true;
			}

			handle = handle.getNext();
		}
		return false;
	}

	public void report() {
	}
}

// vim:ts=4
