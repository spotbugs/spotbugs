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

public class PreferZeroLengthArrays extends BytecodeScanningDetector implements Constants2 {
	boolean nullOnTOS = false;
	private BugReporter bugReporter;

	public PreferZeroLengthArrays(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void visit(Code obj) {
		String returnType = getMethodSig().substring(getMethodSig().indexOf(")") + 1);
		if (returnType.startsWith("[")) {
			nullOnTOS = false;
			super.visit(obj);
		}
	}


	public void sawOpcode(int seen) {

		switch (seen) {
		case ACONST_NULL:
			nullOnTOS = true;
			return;
		case ARETURN:
			if (nullOnTOS)
				bugReporter.reportBug(new BugInstance(this, "PZLA_PREFER_ZERO_LENGTH_ARRAYS", LOW_PRIORITY)
				        .addClassAndMethod(this)
				        .addSourceLine(this, getPC()));
			break;
		}
		nullOnTOS = false;
	}
}
