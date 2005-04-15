/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004,2005 University of Maryland
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
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.ObjectType;
import org.apache.bcel.generic.ReferenceType;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.ba.CFG;
import edu.umd.cs.findbugs.ba.CFGBuilderException;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Hierarchy;
import edu.umd.cs.findbugs.ba.Location;
import edu.umd.cs.findbugs.ba.TypeDataflow;
import edu.umd.cs.findbugs.ba.TypeFrame;

/**
 * Find places where ordinary (balanced) synchronization is performed
 * on JSR166 Lock objects.  Suggested by Doug Lea.
 *
 * @author David Hovemeyer
 */
public class FindJSR166LockMonitorenter implements Detector, StatelessDetector {
	private BugReporter bugReporter;

	private static final ObjectType LOCK_TYPE = new ObjectType("java.util.concurrent.locks.Lock");

	public FindJSR166LockMonitorenter(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public void visitClassContext(ClassContext classContext) {
		JavaClass jclass = classContext.getJavaClass();
		Method[] methodList = jclass.getMethods();

		for (int i = 0; i < methodList.length; ++i) {
			Method method = methodList[i];
			if (method.getCode() == null)
				continue;

			// We can ignore methods that don't contain a monitorenter
			BitSet bytecodeSet = classContext.getBytecodeSet(method);
			if (!bytecodeSet.get(Constants.MONITORENTER))
				continue;

			try {
				analyzeMethod(classContext, method);
			} catch (CFGBuilderException e) {
				bugReporter.logError("FindJSR166LockMonitorEnter caught exception", e);
			} catch (DataflowAnalysisException e) {
				bugReporter.logError("FindJSR166LockMonitorEnter caught exception", e);
			}
		}
	}

	private void analyzeMethod(ClassContext classContext, Method method)
			throws CFGBuilderException, DataflowAnalysisException {
		ConstantPoolGen cpg = classContext.getConstantPoolGen();
		CFG cfg = classContext.getCFG(method);
		TypeDataflow typeDataflow = classContext.getTypeDataflow(method);

		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
			Location location = i.next();
			
			InstructionHandle handle = location.getHandle();
			Instruction ins = handle.getInstruction();
			
			if (ins.getOpcode() != Constants.MONITORENTER)
				continue;
			
			TypeFrame frame = typeDataflow.getFactAtLocation(location);
			if (!frame.isValid())
				continue;
			Type type = frame.getInstance(ins, cpg);
			
			if (!(type instanceof ReferenceType)) {
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

	public void report() {
	}
}

// vim:ts=4
