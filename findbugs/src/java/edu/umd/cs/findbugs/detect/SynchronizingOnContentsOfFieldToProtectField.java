/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004-2006 University of Maryland
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

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class SynchronizingOnContentsOfFieldToProtectField extends OpcodeStackDetector {

	BugReporter bugReporter;

	public SynchronizingOnContentsOfFieldToProtectField(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	@Override
	public void visit(Code code) {
		// System.out.println(getMethodName());

		state = 0;
		super.visit(code); // make callbacks to sawOpcode for all opcodes

	}
	
	 int state = 0;

	XField field, putField;
	XField syncField;
	int putPC;

	@Override
	public void sawOpcode(int seen) {
		// System.out.println(state + " " + getPC() + " " + OPCODE_NAMES[seen]);
		if (seen == PUTFIELD) {
			if (getPrevOpcode(1) == ALOAD_0) 
				putField = null;
			else {
				putField = getXFieldOperand();
				putPC = getPC();
			}
		}
		if (seen == MONITOREXIT && getPrevOpcode(2) == PUTFIELD
				&& putField != null && putField.equals(syncField)) {
			bugReporter.reportBug(new BugInstance(this, "ML_SYNC_ON_FIELD_TO_GUARD_CHANGING_THAT_FIELD", Priorities.HIGH_PRIORITY).addClassAndMethod(this)
			        .addField(syncField).addSourceLine(this, putPC));
		}
		
		if (seen==MONITORENTER)
			syncField = null;
		switch (state) {
		case 0:
			if (seen == ALOAD_0)
				state = 1;
			break;
		case 1:
			if (seen == GETFIELD) {
				state = 2;
				field = getXFieldOperand();
			} else
				state = 0;
			break;
		case 2:
			if (seen == DUP)
				state = 3;
			else
				state = 0;
			break;
		case 3:
			if (isRegisterStore())
				state = 4;
			else
				state = 0;
			break;
		case 4:
			if (seen == MONITORENTER) {
				state = 0;
				syncField = field;
			} else
				state = 0;
			break;
		
		}

	}

}
