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


import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.ba.Hierarchy;

public class StartInConstructor extends BytecodeScanningDetector implements StatelessDetector {
	private BugReporter bugReporter;

	public StartInConstructor(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}



	boolean isFinal;

	@Override
		 public void visit(JavaClass obj) {
		isFinal = (obj.getAccessFlags() & ACC_FINAL) != 0
				|| (obj.getAccessFlags() & ACC_PUBLIC) == 0;
	}

	@Override
		 public void visit(Code obj) {
		if (getMethodName().equals("<init>")) super.visit(obj);
	}

	@Override
		 public void sawOpcode(int seen) {
		if (!isFinal && seen == INVOKEVIRTUAL && getNameConstantOperand().equals("start")
				&& getSigConstantOperand().equals("()V")) {
			try {
				if (Hierarchy.isSubtype(getDottedClassConstantOperand(), "java.lang.Thread")) {
					bugReporter.reportBug(new BugInstance(this, "SC_START_IN_CTOR", NORMAL_PRIORITY)
							.addClassAndMethod(this)
							.addCalledMethod(this)
							.addSourceLine(this));
				}
			} catch (ClassNotFoundException e) {
				bugReporter.reportMissingClass(e);
			}
		}
	}
}
