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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.OpcodeStack.Item;

public class InfiniteLoop extends BytecodeScanningDetector {

	private static final boolean active = true;


	static class ForwardJump {
		final int from, to;
		ForwardJump(int from, int to) {
			this.from = from;
			this.to = to;
		}
	}
	static class BackwardsBranch extends ForwardJump {
		final List<Integer> invariantRegisters = new LinkedList<Integer>();
		final int numLastUpdates;
		BackwardsBranch(OpcodeStack stack, int from, int to) {
			super(from,to);
			numLastUpdates = stack.getNumLastUpdates();
			for(int i = 0; i < numLastUpdates; i++) 
				if (stack.getLastUpdate(i) < to) 
					invariantRegisters.add(i);
			}
					
		}
	static class ForwardConditionalBranch  extends ForwardJump {
		final OpcodeStack.Item item0, item1;
		ForwardConditionalBranch(OpcodeStack.Item item0, OpcodeStack.Item item1, int from, int to) {
			super(from,to);
			this.item0 = item0;
			this.item1 = item1;
		}
		
	}
	BugReporter bugReporter;

	LinkedList<BackwardsBranch> backwardBranches = new LinkedList<BackwardsBranch>();
	
	LinkedList<ForwardConditionalBranch> forwardConditionalBranches = new LinkedList<ForwardConditionalBranch>();
	
	LinkedList<ForwardJump> forwardJumps = new LinkedList<ForwardJump>();
	void purgeForwardJumps(int before) {
		for(Iterator<ForwardJump> i = forwardJumps.iterator(); i.hasNext(); ) {
			ForwardJump j = i.next();
			if (j.to < before) i.remove();
		}
	}
	void addForwardJump(int from, int to) {
		if (from >= to) return;
		purgeForwardJumps(from);
		forwardJumps.add(new ForwardJump(from, to));
	}
	
	int getFurthestJump(int from) {
		int result = Integer.MIN_VALUE;
		for(ForwardJump f : forwardJumps) 
			if (f.from >= from && f.to > result)
				result = f.to;
		return result;
	}
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
		backwardBranches.clear();
		forwardConditionalBranches.clear();
		forwardJumps.clear();
		super.visit(obj);
		for(BackwardsBranch bb : backwardBranches) {
			LinkedList<ForwardConditionalBranch> myForwardBranches = new LinkedList<ForwardConditionalBranch>();
			for(ForwardConditionalBranch fcb : forwardConditionalBranches) 
				if (bb.to < fcb.from && fcb.from < bb.from &&  bb.from < fcb.to)
					myForwardBranches.add(fcb);
			if (myForwardBranches.size() != 1) continue;
			ForwardConditionalBranch fcb = myForwardBranches.get(0);
			if (isConstant(fcb.item0, bb) && 
					isConstant(fcb.item1, bb)) {
				BugInstance bug = new BugInstance(this, "IL_INFINITE_LOOP",
						HIGH_PRIORITY).addClassAndMethod(this).addSourceLine(
						this, fcb.from);
				bugReporter.reportBug(bug);
			}
			
		}
	}
	/**
	 * @param item0
	 * @param invariantRegisters
	 * @return
	 */
	private boolean isConstant(Item item0, BackwardsBranch bb) {
		
		int reg = item0.getRegisterNumber();
		if (reg >= 0 && (bb.invariantRegisters.contains(reg) || reg >= bb.numLastUpdates)) return true;
		if (item0.getConstant() != null) return true;
		return false;
	}
	@Override
	public void sawBranchTo(int target) {
		addForwardJump(getPC(), target);
	}
	@Override
	public void sawOpcode(int seen) {
		System.out.println(getPC() + " " + OPCODE_NAMES[seen] + " " + stack);
		stack.mergeJumps(this);
		switch (seen) {
		case GOTO:
			if (getBranchOffset() < 0) {
				BackwardsBranch bb = new BackwardsBranch(stack, getPC(), getBranchTarget());
				if (bb.invariantRegisters.size() > 0) backwardBranches.add(bb);
			}
			break;
		case ARETURN:
		case IRETURN:
		case RETURN:
		case DRETURN:
		case FRETURN:
		case LRETURN:
			addForwardJump(getPC(), Integer.MAX_VALUE);
			break;
		case IF_ICMPNE:
		case IF_ICMPEQ:
		case IF_ICMPGT:
		case IF_ICMPLE:
		case IF_ICMPLT:
		case IF_ICMPGE:
			OpcodeStack.Item item0 = stack.getStackItem(0);
			OpcodeStack.Item item1 = stack.getStackItem(1);
			if (getBranchOffset() > 0) {
				forwardConditionalBranches.add(new ForwardConditionalBranch(item0, item1, getPC(), getBranchTarget()));
				break;
			}
			if (getFurthestJump(getBranchTarget()) > getPC())
				break;

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
			int reg = item1.getRegisterNumber();
		if (reg >= 0)
		return stack.getLastUpdate(reg) < branchTarget;
		if (item1.getConstant() != null)
			return true;
		return false;
	
	}
}
