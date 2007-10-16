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


import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.StatelessDetector;

//   2:   astore_1
//   3:   monitorenter
//   4:   aload_0
//   5:   invokevirtual   #13; //Method java/lang/Object.notify:()V
//   8:   aload_1
//   9:   monitorexit


public class FindUnconditionalWait extends BytecodeScanningDetector implements StatelessDetector {
	int stage = 0;
	private BugReporter bugReporter;

	public FindUnconditionalWait(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}



	@Override
		 public void visit(Method obj) {
		stage = 0;
	}

	@Override
		 public void sawOffset(int offset) {
		if (stage == 1) stage = 0;
	}

	@Override
		 public void sawOpcode(int seen) {
		switch (stage) {
		case 0:
			if (seen == MONITORENTER)
				stage = 1;
			break;
		case 1:
			if (seen == INVOKEVIRTUAL && getNameConstantOperand().equals("wait")) {
				bugReporter.reportBug(new BugInstance(this, "UW_UNCOND_WAIT",
						getSigConstantOperand().equals("()V") ? NORMAL_PRIORITY : LOW_PRIORITY)
						.addClassAndMethod(this)
						.addSourceLine(this));
				stage = 2;
			}
			break;
		}
	}
}
