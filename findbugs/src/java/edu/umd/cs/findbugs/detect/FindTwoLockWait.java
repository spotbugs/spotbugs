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
import edu.umd.cs.findbugs.*;

import java.util.*;
import java.io.*;

import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

import edu.umd.cs.daveho.ba.*;

public class FindTwoLockWait implements Detector {

	private BugReporter bugReporter;
	private JavaClass javaClass;

	public FindTwoLockWait(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void visitClassContext(ClassContext classContext) {
		javaClass = classContext.getJavaClass();

		try {

			Method[] methodList = javaClass.getMethods();
			for (int i = 0; i < methodList.length; ++i) {
				Method method = methodList[i];

				final MethodGen methodGen = classContext.getMethodGen(method);
				if (methodGen == null)
					continue;

				if (!preScreen(methodGen))
					continue;

				final CFG cfg = classContext.getCFG(method);
				final LockCountDataflow dataflow = classContext.getAnyLockCountDataflow(method);

				new LocationScanner(cfg).scan(new LocationScanner.Callback() {
					public void visitLocation(Location location) {
						visitInstruction(location.getHandle(), location.getBasicBlock(), methodGen, dataflow);
					}
				});
			}

		} catch (DataflowAnalysisException e) {
			throw new AnalysisException("FindTwoLockWait caught exception: " + e.toString(), e);
		} catch (CFGBuilderException e) {
			throw new AnalysisException("FindTwoLockWait caught exception: " + e.toString(), e);
		}
	}

	public boolean preScreen(MethodGen mg) {
		ConstantPoolGen cpg = mg.getConstantPool();

		int lockCount = mg.isSynchronized() ? 1 : 0;
		boolean sawWait = false;

		InstructionHandle handle = mg.getInstructionList().getStart();
		while (handle != null && !(lockCount >= 2 && sawWait)) {
			Instruction ins = handle.getInstruction();
			if (ins instanceof MONITORENTER)
				++lockCount;
			else if (ins instanceof INVOKEVIRTUAL) {
				INVOKEVIRTUAL inv = (INVOKEVIRTUAL) ins;
				if (inv.getMethodName(cpg).equals("wait"))
					sawWait = true;
			}

			handle = handle.getNext();
		}

		return lockCount >= 2 && sawWait;
	}

	public void visitInstruction(InstructionHandle handle, BasicBlock bb, MethodGen methodGen, LockCountDataflow dataflow) {
		try {
			ConstantPoolGen cpg = methodGen.getConstantPool();
	
			if (isWait(handle, cpg)) {
				LockCount count = dataflow.getFactAtLocation(new Location(handle, bb));
				if (count.getCount() > 1) {
					// A wait with multiple locks held?
					String sourceFile = javaClass.getSourceFileName();
					bugReporter.reportBug(new BugInstance("2LW_TWO_LOCK_WAIT", NORMAL_PRIORITY)
						.addClass(javaClass)
						.addMethod(methodGen, sourceFile)
						.addSourceLine(methodGen, sourceFile, handle));
				}
			}
		} catch (DataflowAnalysisException e) {
			throw new AnalysisException(e.getMessage());
		}
	}

	private boolean isWait(InstructionHandle handle, ConstantPoolGen cpg) {
		Instruction ins = handle.getInstruction();
		if (!(ins instanceof INVOKEVIRTUAL))
			return false;
		INVOKEVIRTUAL inv = (INVOKEVIRTUAL) ins;

		String methodName = inv.getMethodName(cpg);
		String methodSig = inv.getSignature(cpg);

		return methodName.equals("wait") &&
			(methodSig.equals("()V") || methodSig.equals("(J)V") || methodSig.equals("(JI)V"));
	}

	public void report() {
	}
}

// vim:ts=4
