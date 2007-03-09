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


import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.ba.*;
import edu.umd.cs.findbugs.ba.type.*;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;

import java.util.*;
import org.apache.bcel.Constants;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * Find places where ordinary (balanced) synchronization is performed
 * on JSR166 Lock objects.  Suggested by Doug Lea.
 *
 * @author David Hovemeyer
 */
public final class FindJSR166LockMonitorenter implements Detector, StatelessDetector {
	private BugReporter bugReporter;

	private static final ObjectType LOCK_TYPE = ObjectTypeFactory.getInstance("java.util.concurrent.locks.Lock");

	public FindJSR166LockMonitorenter(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}

	public void visitClassContext(ClassContext classContext) {
		JavaClass jclass = classContext.getJavaClass();
		Method[] methodList = jclass.getMethods();

		for (Method method : methodList) {
			if (method.getCode() == null)
				continue;

			// We can ignore methods that don't contain a monitorenter
			BitSet bytecodeSet = classContext.getBytecodeSet(method);
			if (bytecodeSet == null) continue;
			if (!bytecodeSet.get(Constants.MONITORENTER))
				continue;

			
			analyzeMethod(classContext, method);
			
		}
	}

	private void analyzeMethod(ClassContext classContext, Method method) 
			 {
		ConstantPoolGen cpg = classContext.getConstantPoolGen();
		CFG cfg;
        try {
            cfg = classContext.getCFG(method);
        } catch (CFGBuilderException e1) {
            AnalysisContext.logError("Coult not get CFG", e1);
            return;
        }
		TypeDataflow typeDataflow;
        try {
            typeDataflow = classContext.getTypeDataflow(method);
        } catch (CheckedAnalysisException e1) {
            AnalysisContext.logError("Coult not get Type dataflow", e1);
            return;
        }

		for (Iterator<Location> i = cfg.locationIterator(); i.hasNext();) {
			Location location = i.next();
			
			InstructionHandle handle = location.getHandle();
			Instruction ins = handle.getInstruction();
			
			if (ins.getOpcode() != Constants.MONITORENTER)
				continue;
            Type type;
            try {
			TypeFrame frame = typeDataflow.getFactAtLocation(location);
			if (!frame.isValid())
				continue;
			 type = frame.getInstance(ins, cpg);
            } catch (CheckedAnalysisException e) {
                AnalysisContext.logError("Coult not get Type dataflow", e);
                continue;
            }
			
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
						.addSourceLine(classContext, mg, sourceFile, handle));
			}
		}
	}

	public void report() {
	}
}

// vim:ts=4
