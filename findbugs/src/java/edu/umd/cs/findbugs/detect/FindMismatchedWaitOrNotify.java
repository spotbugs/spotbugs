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
import org.apache.bcel.Constants;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.*;

public class FindMismatchedWaitOrNotify implements Detector {
	private BugReporter bugReporter;
	//private AnalysisContext analysisContext;

	public FindMismatchedWaitOrNotify(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void setAnalysisContext(AnalysisContext analysisContext) {
		//this.analysisContext = analysisContext;
	}

	public void visitClassContext(ClassContext classContext) {
		JavaClass jclass = classContext.getJavaClass();

		Method[] methodList = jclass.getMethods();
		for (int i = 0; i < methodList.length; ++i) {
			Method method = methodList[i];
			MethodGen methodGen = classContext.getMethodGen(method);
			if (methodGen == null)
				continue;

			// Don't bother analyzing the method unless there is both locking
			// and a method call.
			BitSet bytecodeSet = classContext.getBytecodeSet(method);
			if (!(bytecodeSet.get(Constants.MONITORENTER) && bytecodeSet.get(Constants.INVOKEVIRTUAL)))
				continue;

			try {
				analyzeMethod(classContext, method);
			} catch (DataflowAnalysisException e) {
				throw new AnalysisException("FindMismatchedWaitOrNotify: caught exception " + e.toString(), e);
			} catch (CFGBuilderException e) {
				throw new AnalysisException("FindMismatchedWaitOrNotify: caught exception " + e.toString(), e);
			}
		}
	}

	private void analyzeMethod(ClassContext classContext, Method method)
	        throws CFGBuilderException, DataflowAnalysisException {

		MethodGen methodGen = classContext.getMethodGen(method);
		ConstantPoolGen cpg = methodGen.getConstantPool();
		CFG cfg = classContext.getCFG(method);
		ValueNumberDataflow vnaDataflow = classContext.getValueNumberDataflow(method);
		LockDataflow dataflow = classContext.getLockDataflow(method);

		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
			Location location = i.next();

			BasicBlock basicBlock = location.getBasicBlock();
			InstructionHandle handle = location.getHandle();

			Instruction ins = handle.getInstruction();
			if (!(ins instanceof INVOKEVIRTUAL))
				continue;
			INVOKEVIRTUAL inv = (INVOKEVIRTUAL) ins;

			String methodName = inv.getName(cpg);
			String methodSig = inv.getSignature(cpg);

			if (Hierarchy.isMonitorWait(methodName, methodSig)
			        || Hierarchy.isMonitorNotify(methodName, methodSig)) {
				int numConsumed = inv.consumeStack(cpg);
				if (numConsumed == Constants.UNPREDICTABLE)
					throw new AnalysisException("Unpredictable stack consumption", methodGen, handle);

				ValueNumberFrame frame = vnaDataflow.getFactAtLocation(location);
				if (!frame.isValid())
				// Probably dead code
					continue;
				if (frame.getStackDepth() - numConsumed < 0)
					throw new AnalysisException("Stack underflow", methodGen, handle);
				ValueNumber ref = frame.getValue(frame.getNumSlots() - numConsumed);

				LockSet lockSet = dataflow.getFactAtLocation(location);
				int lockCount = lockSet.getLockCount(ref.getNumber());

				if (lockCount == 0) {
					String sourceFile = classContext.getJavaClass().getSourceFileName();
					String type = methodName.equals("wait")
					        ? "MWN_MISMATCHED_WAIT"
					        : "MWN_MISMATCHED_NOTIFY";
					bugReporter.reportBug(new BugInstance(this, type, NORMAL_PRIORITY)
					        .addClassAndMethod(methodGen, sourceFile)
					        .addSourceLine(methodGen, sourceFile, handle));
				}
			}
		}
	}

	public void report() {
	}
}

// vim:ts=3
