/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004 University of Maryland
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

/**
 * Find places where ordinary (balanced) synchronization is performed
 * on JSR166 Lock objects.  Suggested by Doug Lea.
 *
 * @author David Hovemeyer
 */
public class FindJSR166LockMonitorenter implements Detector {
	private BugReporter bugReporter;
	//private AnalysisContext analysisContext;

	private static final ObjectType LOCK_TYPE = new ObjectType("java.util.concurrent.locks.Lock");

	public FindJSR166LockMonitorenter(BugReporter bugReporter) {
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
			if (method.getCode() == null)
				continue;

			analyzeMethod(classContext, method);
		}
	}

	private void analyzeMethod(ClassContext classContext, Method method) {
		ConstantPoolGen cpg = classContext.getConstantPoolGen();
		try {
			BitSet bytecodeSet = classContext.getBytecodeSet(method);
			if (!bytecodeSet.get(Constants.MONITORENTER))
				return;

			CFG cfg = classContext.getCFG(method);
			TypeDataflow typeDataflow = classContext.getTypeDataflow(method);

			for (Iterator<BasicBlock> i = cfg.blockIterator(); i.hasNext();) {
				BasicBlock basicBlock = i.next();
				for (Iterator<InstructionHandle> j = basicBlock.instructionIterator(); j.hasNext();) {
					InstructionHandle handle = j.next();
					Instruction ins = handle.getInstruction();

					if (ins.getOpcode() != Constants.MONITORENTER)
						continue;

					Type type = typeDataflow.getFactAtLocation(new Location(handle, basicBlock)).getInstance(ins, cpg);

					if (!(type instanceof ReferenceType)) {
						// FIXME:
						// Something is deeply wrong if a non-reference type
						// is used for a monitorenter.  But, that's really a
						// verification problem.
						return;
					}

					boolean isSubtype;
					try {
						isSubtype = Hierarchy.isSubtype((ReferenceType) type, LOCK_TYPE);
					} catch (ClassNotFoundException e) {
						bugReporter.reportMissingClass(e);
						return;
					}

					if (isSubtype) {
						MethodGen mg = classContext.getMethodGen(method);
						String sourceFile = classContext.getJavaClass().getSourceFileName();

						bugReporter.reportBug(new BugInstance(this, "JLM_JSR166_LOCK_MONITORENTER", NORMAL_PRIORITY)
						        .addClassAndMethod(mg, sourceFile)
						        .addSourceLine(mg, sourceFile, handle));
					}
				}
			}
		} catch (CFGBuilderException e) {
			throw new AnalysisException("FindJSR166LockMonitorenter: caught exception " + e.toString(), e);
		} catch (DataflowAnalysisException e) {
			throw new AnalysisException("FindJSR166LockMonitorenter: caught exception " + e.toString(), e);
		}
	}

	public void report() {
	}
}

// vim:ts=4
