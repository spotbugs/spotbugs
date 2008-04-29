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

import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.FieldAnnotation;

/*
 * This is a very simply written detector. It checks if there is exactly
 * in the bytecode getting a field, DUP, a store, MONITORENTER, get same
 * field, and check if null.
 * 
 * Author: Kristin Stephens
 */

public class SynchronizeAndNullCheckField extends BytecodeScanningDetector {

	BugReporter bugReporter;

	public SynchronizeAndNullCheckField(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}


	@Override
	public void visit(Method obj) {
		gottenField = null;
		currState = 0;
		syncField = null;
	}

	FieldAnnotation gottenField;
	FieldAnnotation syncField;
	int currState;
	@Override
	public void sawOpcode(int seen) {
		// System.out.println(getPC() + " " + OPCODE_NAMES[seen] + " " + currState);
		switch(currState){
		case 0:
			if(seen == GETFIELD || seen == GETSTATIC){
				syncField = FieldAnnotation.fromReferencedField(this);
				currState = 1;
			}
			break;
		case 1:
			if (seen == DUP){
				currState = 2;
			} else currState = 0;
			break;
		case 2:
			if(seen == ASTORE || seen == ASTORE_0 || seen == ASTORE_1
					|| seen == ASTORE_2 || seen == ASTORE_3)
				currState = 3;
			else currState = 0;
			break;
		case 3:
			if(seen == MONITORENTER){
				currState = 4;
			} else currState = 0;
			break;
		case 4:
			if(seen == GETFIELD || seen == GETSTATIC){
				gottenField = FieldAnnotation.fromReferencedField(this);
				currState = 5;
			} else currState = 0;
			break;
		case 5:
			if((seen == IFNONNULL || seen == IFNULL) && gottenField.equals(syncField)){
				BugInstance bug = new BugInstance(this, "NP_SYNC_AND_NULL_CHECK_FIELD", NORMAL_PRIORITY)
				.addClass(this).addMethod(this).addField(syncField).addSourceLine(this);
				bugReporter.reportBug(bug);
			} else currState = 0;
			break;
		default:
			currState = 0;
		}
	}
}
