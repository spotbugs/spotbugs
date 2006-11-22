/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2006 University of Maryland
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
import edu.umd.cs.findbugs.OpcodeStack.Item;

import org.apache.bcel.classfile.*;

public class InfiniteLoop extends BytecodeScanningDetector {

	private static final boolean active = true;

	BugReporter bugReporter;

	OpcodeStack stack = new OpcodeStack();

	public InfiniteLoop(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	@Override
	public void visit(JavaClass obj) {
	}

	@Override
	public void visit(Method obj) {
	}

	@Override
	public void visit(Code obj) {
		stack.resetForMethodEntry(this);
		super.visit(obj);
	}

	int lastBranch;

	@Override
	public void sawOffset(int offset) {
		lastBranch = getPC();
	}

	@Override
	public void sawOpcode(int seen) {
		stack.mergeJumps(this);
		switch (seen) {
		case ARETURN:
		case IRETURN:
		case RETURN:
		case DRETURN:
		case FRETURN:
		case LRETURN:
			lastBranch = getPC();
			break;
		case IF_ICMPNE:
		case IF_ICMPEQ:
		case IF_ICMPGT:
		case IF_ICMPLE:
		case IF_ICMPLT:
		case IF_ICMPGE:
			if (getBranchOffset() > 0)
				break;
			if (getBranchTarget() < lastBranch)
				break;
			OpcodeStack.Item item0 = stack.getStackItem(0);
			OpcodeStack.Item item1 = stack.getStackItem(1);
			if (constantSince(item0, getBranchTarget())
					&& constantSince(item1, getBranchTarget())) {
				BugInstance bug = new BugInstance(this, "IL_INFINITE_LOOP",
						HIGH_PRIORITY).addClassAndMethod(this).addSourceLine(
						this, getPC());

				bugReporter.reportBug(bug);
			}

			break;
		}

		stack.sawOpcode(this, seen);
	}

	/**
	 * @param item1
	 * @param branchTarget
	 * @return
	 */
	private boolean constantSince(Item item1, int branchTarget) {
		if (item1.getConstant() != null)
			return true;
		int reg = item1.getRegisterNumber();
		if (reg < 0)
			return false;
		return stack.getLastUpdate(reg) < branchTarget;
	}
}
