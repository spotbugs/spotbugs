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


import edu.umd.cs.findbugs.*;
import java.util.*;
import org.apache.bcel.classfile.Method;

public class MutableLock extends BytecodeScanningDetector implements  StatelessDetector {
	Set<String> setFields = new HashSet<String>();
	boolean thisOnTOS = false;
	private BugReporter bugReporter;

	public MutableLock(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	@Override
         public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
         public void visit(Method obj) {
		super.visit(obj);
		setFields.clear();
		thisOnTOS = false;
	}

	@Override
         public void sawOpcode(int seen) {

		switch (seen) {
		case ALOAD_0:
			thisOnTOS = true;
			return;
		case MONITOREXIT:
			setFields.clear();
			break;
		case PUTFIELD:
			if (getClassConstantOperand().equals(getClassName()))
				setFields.add(getNameConstantOperand());
			break;
		case GETFIELD:
			if (thisOnTOS && getClassConstantOperand().equals(getClassName())
			        && setFields.contains(getNameConstantOperand())
			        && asUnsignedByte(codeBytes[getPC() + 3]) == DUP
			        && asUnsignedByte(codeBytes[getPC() + 5]) == MONITORENTER
			)
				bugReporter.reportBug(new BugInstance(this, "ML_SYNC_ON_UPDATED_FIELD", NORMAL_PRIORITY)
				        .addClassAndMethod(this)
				        .addReferencedField(this)
				        .addSourceLine(this, getPC() + 5));
			break;
		default:
		}
		thisOnTOS = false;
	}
}
