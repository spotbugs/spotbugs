/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004, Tom Truscott <trt@unx.sas.com>
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

import java.text.NumberFormat;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.visitclass.Constants2;
import org.apache.bcel.classfile.*;

public class FindLocalSelfAssignment2 extends BytecodeScanningDetector implements Constants2 {

	private BugReporter bugReporter;
	private int previousLoadOf = -1;

	public FindLocalSelfAssignment2(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}



	public void visit(Code obj) {
		previousLoadOf = -1;
		super.visit(obj);
	}


	public void sawOpcode(int seen) {
		if (isRegisterLoad()) 
			previousLoadOf = getRegisterOperand();
		else {
			if (isRegisterStore() && previousLoadOf == getRegisterOperand())
			       bugReporter.reportBug(
				new BugInstance(this, 
						"SA_LOCAL_SELF_ASSIGNMENT", NORMAL_PRIORITY)
                                        .addClassAndMethod(this)
                                        .addSourceLine(this));

			previousLoadOf = -1;
			}

	}

}
