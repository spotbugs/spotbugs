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

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.visitclass.Constants2;
import org.apache.bcel.classfile.Code;

public class WaitInLoop extends BytecodeScanningDetector implements Constants2 {

	boolean sawWait = false;
	boolean waitHasTimeout = false;
	boolean sawNotify = false;
	int notifyPC;
	int earliestJump = 0;
	int waitAt = 0;
	private BugReporter bugReporter;

	public WaitInLoop(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void visit(Code obj) {
		sawWait = false;
		waitHasTimeout = false;
		sawNotify = false;
		earliestJump = 9999999;
		super.visit(obj);
		if (sawWait && waitAt < earliestJump)
			bugReporter.reportBug(new BugInstance(this, "WA_NOT_IN_LOOP", waitHasTimeout ? LOW_PRIORITY : NORMAL_PRIORITY)
			        .addClassAndMethod(this)
			        .addSourceLine(this, waitAt));
		if (sawNotify)
			bugReporter.reportBug(new BugInstance(this, "NO_NOTIFY_NOT_NOTIFYALL", LOW_PRIORITY)
			        .addClassAndMethod(this)
			        .addSourceLine(this, notifyPC));
	}

	public void sawOpcode(int seen) {

		if ((seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE)
		        && getNameConstantOperand().equals("notify")
		        && getSigConstantOperand().equals("()V")) {
			sawNotify = true;
			notifyPC = getPC();
		}
		if (!sawWait && (seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE)
		        && getNameConstantOperand().equals("wait")
		        && (getSigConstantOperand().equals("()V")
		        || getSigConstantOperand().equals("(J)V")
		        || getSigConstantOperand().equals("(JI)V"))
		) {
			/*
			System.out.println("Saw invocation of "
				+ nameConstant + "("
				+sigConstant
				+")");
			*/

			sawWait = true;
			waitHasTimeout = !getSigConstantOperand().equals("()V");
			waitAt = getPC();
			earliestJump = getPC() + 1;
			return;
		}
		if (seen >= IFEQ && seen <= GOTO
		        || seen >= IFNULL && seen <= GOTO_W)
			earliestJump = Math.min(earliestJump, getBranchTarget());

	}


}
