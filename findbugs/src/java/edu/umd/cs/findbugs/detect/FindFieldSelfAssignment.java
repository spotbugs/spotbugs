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


import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.StatelessDetector;

public class FindFieldSelfAssignment extends BytecodeScanningDetector implements StatelessDetector {
	private BugReporter bugReporter;
	int state;


	public FindFieldSelfAssignment(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	


	@Override
         public void visit(Code obj) {
		state = 0;
		super.visit(obj);
		initializedFields.clear();
	}


	String f;
	String className;
	Set<String> initializedFields = new HashSet<String>();

	@Override
         public void sawOpcode(int seen) {

		switch (state) {
		case 0:
			if (seen == ALOAD_0)
				state = 1;
			break;
		case 1:
			if (seen == ALOAD_0)
				state = 2;
			else
				state = 0;
			break;
		case 2:
			if (seen == GETFIELD) {
				state = 3;
				f = getRefConstantOperand();
				className = getClassConstantOperand();
			} else
				state = 0;
			break;
		case 3:
			if (seen == PUTFIELD && getRefConstantOperand().equals(f) && getClassConstantOperand().equals(className)) {

				int priority = NORMAL_PRIORITY;
				if (getMethodName().equals("<init>") && !initializedFields.contains(getRefConstantOperand()))
						priority = HIGH_PRIORITY;
				bugReporter.reportBug(new BugInstance(this, "SA_FIELD_SELF_ASSIGNMENT", priority)
				        .addClassAndMethod(this)
				        .addReferencedField(this)
				        .addSourceLine(this));
			}
			state = 0;
		}
		
		if (seen == PUTFIELD  && getClassConstantOperand().equals(className))
			initializedFields.add(getRefConstantOperand());

	}

}
