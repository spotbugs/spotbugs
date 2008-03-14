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
		System.out.println(getMethodName());

		state = 0;
		super.visit(code); // make callbacks to sawOpcode for all opcodes

	}
	
	
	/** Want to look for the following pattern 
	
	 	ALOAD 0
	    GETFIELD BadSynchronization.x : Ljava/lang/Integer;
	    DUP
	    ASTORE x
	    MONITORENTER
	    ALOAD 0
	    GETFIELD BadSynchronization.x : Ljava/lang/Integer;
	*/
	
	 int state = 0;

	XField field, putField;
	XField syncField;

	@Override
	public void sawOpcode(int seen) {
		System.out.println(state + " " + getPC() + " " + OPCODE_NAMES[seen]);
		if (seen == PUTFIELD) {
			if (getPrevOpcode(1) == ALOAD_0) 
				putField = null;
			else putField = getXFieldOperand();
		}
		if (seen == MONITOREXIT && getPrevOpcode(2) == PUTFIELD
				&& putField != null && putField.equals(syncField)) {
			bugReporter.reportBug(new BugInstance(this, "TESTING", Priorities.HIGH_PRIORITY).addClassAndMethod(this)
			        .addField(syncField).addSourceLine(this));
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
				state = 5;
				syncField = field;
			} else
				state = 0;
			break;
		case 5:
			if (seen == ALOAD_0)
				state = 6;
			else
				state = 0;
			break;
		case 6:
			if (seen == GETFIELD && field.equals(getXFieldOperand()))
				bugReporter.reportBug(new BugInstance(this, "TESTING", Priorities.NORMAL_PRIORITY).addClassAndMethod(this)
				        .addField(field).addSourceLine(this));
			state = 0;
			break;
		}

	}

}
