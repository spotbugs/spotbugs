/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005-2006 University of Maryland
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
import org.apache.bcel.classfile.Code;

public class FindLocalSelfAssignment2 extends BytecodeScanningDetector implements StatelessDetector {

	private BugReporter bugReporter;
	private int previousLoadOf = -1;
	private int previousGotoTarget;
	private int gotoCount;
	public FindLocalSelfAssignment2(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}



	@Override
         public void visit(Code obj) {
		previousLoadOf = -1;
		previousGotoTarget = -1;
		gotoCount = 0;
		super.visit(obj);
	}


	@Override
    public void sawOpcode(int seen) {
		if (seen == GOTO) {
			previousGotoTarget = getBranchTarget();
			gotoCount++;
			if (previousGotoTarget < getPC())
				previousLoadOf = -1;
		} else {
			if (isRegisterLoad()) 
				previousLoadOf = getRegisterOperand();
			else {
				if (isRegisterStore() && previousLoadOf == getRegisterOperand() && gotoCount < 2 && getPC() != previousGotoTarget)
				       bugReporter.reportBug(
					new BugInstance(this, 
							"SA_LOCAL_SELF_ASSIGNMENT", getMethodName().equals("<init>") ? HIGH_PRIORITY : NORMAL_PRIORITY)
	                                        .addClassAndMethod(this)
	                                        .add(LocalVariableAnnotation.getLocalVariableAnnotation(getMethod(), getRegisterOperand(), getPC(), getPC()))
	                                        .addSourceLine(this));
	
				previousLoadOf = -1;
				gotoCount = 0;
			}
		}
	}
}
