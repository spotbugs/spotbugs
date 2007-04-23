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


import java.util.BitSet;

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.StatelessDetector;

public class FindLocalSelfAssignment2 extends BytecodeScanningDetector implements StatelessDetector {

	private BugReporter bugReporter;
	private int previousLoadOf = -1;
	private int previousGotoTarget;
	private int gotoCount;
	public FindLocalSelfAssignment2(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	private BitSet previousStores = new BitSet();


	@Override
	   public void visit(Code obj) {
		previousLoadOf = -1;
		previousGotoTarget = -1;
		gotoCount = 0;
		previousStores.clear();
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
				if (isRegisterStore()) {
					if (previousLoadOf == getRegisterOperand() && gotoCount < 2 && getPC() != previousGotoTarget) {
					int priority = NORMAL_PRIORITY;
					String methodName = getMethodName();
					if (methodName.equals("<init>") || methodName.startsWith("set") && getCode().getCode().length <= 5 ||
							!previousStores.get(getRegisterOperand())) priority = HIGH_PRIORITY;
					   bugReporter.reportBug(
					new BugInstance(this, 
							"SA_LOCAL_SELF_ASSIGNMENT", priority)
											.addClassAndMethod(this)
											.add(LocalVariableAnnotation.getLocalVariableAnnotation(getMethod(), getRegisterOperand(), getPC(), getPC()))

	                                        .addSourceLine(this));
					}
					previousStores.set(getRegisterOperand());
				}


				previousLoadOf = -1;
				gotoCount = 0;
			}
		}
	}
}
