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

public class IteratorIdioms extends BytecodeScanningDetector implements Constants2 {

	private BugReporter bugReporter;

	public IteratorIdioms(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	boolean sawNoSuchElement;

	public void visit(Code obj) {
		if (getMethodName().equals("next")
		        && getMethodSig().equals("()Ljava/lang/Object;")) {
			sawNoSuchElement = false;
			super.visit(obj);
			if (!sawNoSuchElement)
//			bugReporter.reportBug(BugInstance.inMethod("IT_NO_SUCH_ELEMENT", UNKNOWN_PRIORITY, this));
				bugReporter.reportBug(new BugInstance(this, "IT_NO_SUCH_ELEMENT", NORMAL_PRIORITY).addClassAndMethod(this));
		}
	}


	public void sawOpcode(int seen) {
		if (seen == NEW
		        && getClassConstantOperand().equals("java/util/NoSuchElementException"))
			sawNoSuchElement = true;
		else if (seen == INVOKESPECIAL
		        || seen == INVOKEVIRTUAL
		        || seen == INVOKEINTERFACE) {
			// System.out.println("Saw call to " + nameConstant);
			if (getNameConstantOperand().indexOf("next") >= 0)
				sawNoSuchElement = true;
		}
	}
}
