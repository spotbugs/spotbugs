/*
 * FindBugs - Find bugs in Java programs
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

package edu.umd.cs.findbugs.detect;

import java.util.*;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.ba.*;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.*;

public class FindTwoLockWait implements Detector {

	private BugReporter bugReporter;
	//private AnalysisContext analysisContext;
	private JavaClass javaClass;

	public FindTwoLockWait(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void setAnalysisContext(AnalysisContext analysisContext) {
		//this.analysisContext = analysisContext;
	}

	public void visitClassContext(ClassContext classContext) {
		javaClass = classContext.getJavaClass();

		Method[] methodList = javaClass.getMethods();
		for (int i = 0; i < methodList.length; ++i) {
			Method method = methodList[i];

			MethodGen methodGen = classContext.getMethodGen(method);
			if (methodGen == null)
				continue;

			if (!preScreen(methodGen))
				continue;

			try {
				analyzeMethod(classContext, method);
			} catch (DataflowAnalysisException e) {
				bugReporter.logError(e.toString());
			} catch (CFGBuilderException e) {
				bugReporter.logError(e.toString());
			}
		}
	}

	private void analyzeMethod(ClassContext classContext, Method method)
	        throws CFGBuilderException, DataflowAnalysisException {

		MethodGen methodGen = classContext.getMethodGen(method);
		CFG cfg = classContext.getCFG(method);
		LockDataflow dataflow = classContext.getLockDataflow(method);

		for (Iterator<Location> j = cfg.locationIterator(); j.hasNext();) {
			Location location = j.next();
			visitInstruction(location, methodGen, dataflow);
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

	public void visitInstruction(Location location, MethodGen methodGen, LockDataflow dataflow) {
		try {
			ConstantPoolGen cpg = methodGen.getConstantPool();

			if (isWait(location.getHandle(), cpg)) {
				int count = dataflow.getFactAtLocation(location).getNumLockedObjects();
				if (count > 1) {
					// A wait with multiple locks held?
					String sourceFile = javaClass.getSourceFileName();
					bugReporter.reportBug(new BugInstance(this, "2LW_TWO_LOCK_WAIT", NORMAL_PRIORITY)
					        .addClass(javaClass)
					        .addMethod(methodGen, sourceFile)
					        .addSourceLine(methodGen, sourceFile, location.getHandle()));
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

// vim:ts=3
