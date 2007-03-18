/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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
import java.util.Iterator;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.LockDataflow;
import edu.umd.cs.findbugs.ba.LockSet;

/**
 * Find calls to Thread.sleep() made with a lock held.
 * 
 * @author David Hovemeyer
 */
public class FindSleepWithLockHeld implements Detector {
	
	private BugReporter bugReporter;
	
	public FindSleepWithLockHeld(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void visitClassContext(ClassContext classContext) {
		JavaClass javaClass = classContext.getJavaClass();
		
		Method[] methodList = javaClass.getMethods();
		for (Method method : methodList) {
			if (method.getCode() == null)
				continue;

			if (!prescreen(classContext, method))
				continue;

			try {
				analyzeMethod(classContext, method);
			} catch (CFGBuilderException e) {
				bugReporter.logError("FindSleepWithLockHeld caught exception", e);
			} catch (DataflowAnalysisException e) {
				bugReporter.logError("FindSleepWithLockHeld caught exception", e);
			}
		}
	}

	private boolean prescreen(ClassContext classContext, Method method) {
		BitSet bytecodeSet = classContext.getBytecodeSet(method);
		if (bytecodeSet == null) return false;
		// method must acquire a lock
		if (!bytecodeSet.get(Constants.MONITORENTER) && !method.isSynchronized())
			return false;
		
		// and contain a static method invocation
		if (!bytecodeSet.get(Constants.INVOKESTATIC))
			return false;
		
		return true;
	}

	private void analyzeMethod(ClassContext classContext, Method method) throws CFGBuilderException, DataflowAnalysisException {
//		System.out.println("Checking " + method);
		
		CFG cfg = classContext.getCFG(method);
		LockDataflow lockDataflow = classContext.getLockDataflow(method);

		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
			Location location = i.next();
			Instruction ins = location.getHandle().getInstruction(); 

			if (!(ins instanceof INVOKESTATIC))
				continue;
			
			if (!isSleep((INVOKESTATIC) ins, classContext.getConstantPoolGen()))
				continue;
			
//			System.out.println("Found sleep at " + location.getHandle());
			
			LockSet lockSet = lockDataflow.getFactAtLocation(location);
			if (lockSet.getNumLockedObjects() > 0) {
				bugReporter.reportBug(new BugInstance("SWL_SLEEP_WITH_LOCK_HELD", NORMAL_PRIORITY)
						.addClassAndMethod(classContext.getJavaClass(), method)
						.addSourceLine(classContext,method, location));
			}
		}
	}

	private boolean isSleep(INVOKESTATIC ins, ConstantPoolGen cpg) {
		String className = ins.getClassName(cpg);
		if (!className.equals("java.lang.Thread"))
			return false;
		String methodName = ins.getMethodName(cpg);
		String signature = ins.getSignature(cpg);
		
		return methodName.equals("sleep")
				&& (signature.equals("(J)V") || signature.equals("(JI)V"));
	}

	public void report() {
	}

}
