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


import edu.umd.cs.findbugs.*;

import org.apache.bcel.classfile.*;

public class FindFinalizeInvocations extends BytecodeScanningDetector implements StatelessDetector {
	private static final boolean DEBUG = SystemProperties.getBoolean("ffi.debug");

	private BugReporter bugReporter;

	public FindFinalizeInvocations(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}



	boolean sawSuperFinalize;

	@Override
		 public void visit(Method obj) {
		if (DEBUG) System.out.println("FFI: visiting " + getFullyQualifiedMethodName());
		if (getMethodName().equals("finalize")
				&& getMethodSig().equals("()V")
				&& (obj.getAccessFlags() & (ACC_PUBLIC)) != 0
		)
			bugReporter.reportBug(new BugInstance(this, "FI_PUBLIC_SHOULD_BE_PROTECTED", NORMAL_PRIORITY).addClassAndMethod(this));
	}

	@Override
		 public void visit(Code obj) {
		sawSuperFinalize = false;
		super.visit(obj);
		if (!getMethodName().equals("finalize")
				|| !getMethodSig().equals("()V"))
			return;
		String overridesFinalizeIn
				= Lookup.findSuperImplementor(getDottedClassName(),
						"finalize",
						"()V",
		                bugReporter);
		boolean superHasNoFinalizer = overridesFinalizeIn.equals("java.lang.Object");
		// System.out.println("superclass: " + superclassName);
		if (obj.getCode().length == 1) {
			if (superHasNoFinalizer) {
				if (!getMethod().isFinal())
				bugReporter.reportBug(new BugInstance(this, "FI_EMPTY", NORMAL_PRIORITY).addClassAndMethod(this));
			} else
				bugReporter.reportBug(new BugInstance(this, "FI_NULLIFY_SUPER", NORMAL_PRIORITY)
						.addClassAndMethod(this)
						.addClass(overridesFinalizeIn));
		} else if (obj.getCode().length == 5 && sawSuperFinalize)
			bugReporter.reportBug(new BugInstance(this, "FI_USELESS", NORMAL_PRIORITY).addClassAndMethod(this));
		else if (!sawSuperFinalize && !superHasNoFinalizer)
			bugReporter.reportBug(new BugInstance(this, "FI_MISSING_SUPER_CALL", NORMAL_PRIORITY).addClassAndMethod(this)
					.addClass(overridesFinalizeIn));
	}

	@Override
		 public void sawOpcode(int seen) {
		if (seen == INVOKEVIRTUAL && getNameConstantOperand().equals("finalize"))
			bugReporter.reportBug(new BugInstance(this, "FI_EXPLICIT_INVOCATION", 
						getMethodName().equals("finalize") && getMethodSig().equals("()V") ? HIGH_PRIORITY : NORMAL_PRIORITY)
					.addClassAndMethod(this)
					.addCalledMethod(this).describe("METHOD_CALLED")
					.addSourceLine(this, getPC()));
		if (seen == INVOKESPECIAL && getNameConstantOperand().equals("finalize"))
			sawSuperFinalize = true;
	}
}
