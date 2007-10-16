/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
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


import org.apache.bcel.classfile.Code;
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


public class FindNakedNotify extends BytecodeScanningDetector implements  StatelessDetector {
	int stage = 0;
	private BugReporter bugReporter;
	boolean synchronizedMethod;
	private int notifyPC;

	public FindNakedNotify(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}



	@Override
		 public void visit(Method obj) {
		int flags = obj.getAccessFlags();
		synchronizedMethod = (flags & ACC_SYNCHRONIZED) != 0;
	}

	@Override
		 public void visit(Code obj) {
		stage = synchronizedMethod ? 1 : 0;
		super.visit(obj);
		if (synchronizedMethod && stage == 4)
			bugReporter.reportBug(new BugInstance(this, "NN_NAKED_NOTIFY", NORMAL_PRIORITY)
					.addClassAndMethod(this)
					.addSourceLine(this, notifyPC));
	}

	@Override
		 public void sawOpcode(int seen) {
		switch (stage) {
		case 0:
			if (seen == MONITORENTER)
				stage = 1;
			break;
		case 1:
			stage = 2;
			break;
		case 2:
			if (seen == INVOKEVIRTUAL
					&& (getNameConstantOperand().equals("notify")
					|| getNameConstantOperand().equals("notifyAll"))
					&& getSigConstantOperand().equals("()V")) {
				stage = 3;
				notifyPC = getPC();
			} else
				stage = 0;
			break;
		case 3:
			stage = 4;
			break;
		case 4:
			if (seen == MONITOREXIT) {
				bugReporter.reportBug(new BugInstance(this, "NN_NAKED_NOTIFY", NORMAL_PRIORITY)
						.addClassAndMethod(this)
						.addSourceLine(this, notifyPC));
				stage = 5;
			} else
				stage = 0;
			break;
		case 5:
			break;
		default:
			assert false;
		}

	}
}
