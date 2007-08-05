/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

import org.apache.bcel.classfile.*;

public class FindBadForLoop extends OpcodeStackDetector implements  StatelessDetector {


	BugReporter bugReporter;

	public FindBadForLoop(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}



	
				LineNumberTable lineNumbers; 
	@Override
		 public void visit(Code obj) {
			lastRegStore = -1;
			lineNumbers = obj.getLineNumberTable();
			super.visit(obj);
	}


	int lastRegStore;
	@Override
		 public void sawOpcode(int seen) {
		if (seen == ISTORE
			|| seen == ISTORE_0
			|| seen == ISTORE_1
			|| seen == ISTORE_2
			|| seen == ISTORE_3)
			lastRegStore = getRegisterOperand();
		if (lineNumbers!= null && stack.getStackDepth() >= 2 && (
			seen == IF_ICMPGE 
			|| seen == IF_ICMPGT
			|| seen == IF_ICMPLT
			|| seen == IF_ICMPLE
			|| seen == IF_ICMPNE
			|| seen == IF_ICMPEQ)) {
			OpcodeStack.Item item0 = stack.getStackItem(0);
			OpcodeStack.Item item1 = stack.getStackItem(1);
			int r0 = item0.getRegisterNumber();
			int r1 = item1.getRegisterNumber();
			int rMin = Math.min(r0,r1);
			int rMax = Math.max(r0,r1);
			int branchTarget = getBranchTarget();
			if (rMin == -1 && rMax > 0  && rMax == lastRegStore 
				&& branchTarget-6 > getPC())  {
			  int beforeTarget = getCodeByte(branchTarget - 3);
			  int beforeGoto = getCodeByte(branchTarget - 6);
			  if (beforeTarget == GOTO && beforeGoto == IINC) {
				int offset1 = (byte) getCodeByte(branchTarget - 2);
				int offset2 = getCodeByte(branchTarget - 1);
				int offset = offset1 << 8 | offset2;
				 int backTarget = branchTarget - 3 + offset;
				 int reg = getCodeByte(branchTarget - 5);
				int testLineNumber 
					= lineNumbers.getSourceLine(getPC());
				int incLineNumber 
					= lineNumbers.getSourceLine(branchTarget-6);
				int beforeIncLineNumber 
					= lineNumbers.getSourceLine(branchTarget-7);
			  if (backTarget < getPC() &&
				getPC() - 8 < backTarget 
				&& reg != rMax
				&& incLineNumber < testLineNumber+3
				&& beforeIncLineNumber > incLineNumber
				) {

			bugReporter.reportBug(new BugInstance(this, "QF_QUESTIONABLE_FOR_LOOP", NORMAL_PRIORITY)
				.addClassAndMethod(this)
				.addSourceLine(this));
			  }
			  }

			  }
			}
	}

}
