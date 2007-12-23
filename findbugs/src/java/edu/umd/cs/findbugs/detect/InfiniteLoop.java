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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.visitclass.Util;

public class InfiniteLoop extends OpcodeStackDetector {

	private static final boolean active = true;


	ArrayList<BitSet> regModifiedAt = new ArrayList<BitSet>();
	@NonNull BitSet getModifiedBitSet(int reg) {
		while (regModifiedAt.size() <= reg)
			regModifiedAt.add(new BitSet());
		return regModifiedAt.get(reg);
	}
	private void regModifiedAt(int reg, int pc) {
		BitSet b = getModifiedBitSet(reg);
		b.set(pc);
	}
	private void clearRegModified() {
		for(BitSet b : regModifiedAt )
			b.clear();
	}
	private boolean isRegModified(int reg, int firstPC, int lastPC) {
		if (reg < 0) return false;
		BitSet b = getModifiedBitSet(reg);
		int modified = b.nextSetBit(firstPC);
		return (modified >= firstPC && modified <= lastPC);
	}
	static class Jump {
		final int from, to;
		Jump(int from, int to) {
			this.from = from;
			this.to = to;
		}
		@Override
		public String toString() {
			return from + " -> " + to;
		}
		public int hashCode() {
			return from * 37 + to;
		}
		public boolean equals(Object o) {
			if (o == null) return false;
			if (this.getClass() != o.getClass()) return false;
			Jump that = (Jump) o;
			return this.from == that.from && this.to == that.to;
		}
	}
	static class BackwardsBranch extends Jump {
		final List<Integer> invariantRegisters = new LinkedList<Integer>();
		final int numLastUpdates;
		BackwardsBranch(OpcodeStack stack, int from, int to) {
			super(from,to);
			numLastUpdates = stack.getNumLastUpdates();
			for(int i = 0; i < numLastUpdates; i++) 
				if (stack.getLastUpdate(i) < to) 
					invariantRegisters.add(i);
			}
		public int hashCode() {
			return 37*super.hashCode() + 17*invariantRegisters.hashCode() + numLastUpdates;
		}
		public boolean equals(Object o) {
			if (!super.equals(o)) return false;
			BackwardsBranch that = (BackwardsBranch) o;
			return this.invariantRegisters.equals(that.invariantRegisters) && this.numLastUpdates == that.numLastUpdates;
		}
		}
	static class ForwardConditionalBranch  extends Jump {
		final OpcodeStack.Item item0, item1;
		ForwardConditionalBranch(OpcodeStack.Item item0, OpcodeStack.Item item1, int from, int to) {
			super(from,to);
			this.item0 = item0;
			this.item1 = item1;
		}
		public int hashCode() {
			return 37*super.hashCode() + 17*item0.hashCode() + item1.hashCode();
		}
		public boolean equals(Object o) {
			if (!super.equals(o)) return false;
			ForwardConditionalBranch that = (ForwardConditionalBranch) o;
			return this.item0.equals(that.item0) && this.item1.equals(that.item1);
		}

	}
	BugReporter bugReporter;

	HashSet<Jump> backwardReach = new HashSet<Jump>();
	HashSet<BackwardsBranch> backwardBranches = new HashSet<BackwardsBranch>();

	HashSet<ForwardConditionalBranch> forwardConditionalBranches = new HashSet<ForwardConditionalBranch>();

	LinkedList<Jump> forwardJumps = new LinkedList<Jump>();
	void purgeForwardJumps(int before) {
		if (true) return;
		for(Iterator<Jump> i = forwardJumps.iterator(); i.hasNext(); ) {
			Jump j = i.next();
			if (j.to < before) i.remove();
		}
	}
	void addForwardJump(int from, int to) {
		if (from >= to) return;
		purgeForwardJumps(from);
		forwardJumps.add(new Jump(from, to));
	}

	int getFurthestJump(int from) {
		int result = Integer.MIN_VALUE;
		int from2 = getBackwardsReach(from);
		assert from2 <= from;
		from = from2;
		for(Jump f : forwardJumps) 
			if (f.from >= from && f.to > result)
				result = f.to;
		return result;
	}
	
	public InfiniteLoop(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	@Override
	public void visit(Code obj) {
		clearRegModified();
		backwardBranches.clear();
		forwardConditionalBranches.clear();
		forwardJumps.clear();
		backwardReach.clear();
		super.visit(obj);
		backwardBranchLoop: for(BackwardsBranch bb : backwardBranches) {
			LinkedList<ForwardConditionalBranch> myForwardBranches = new LinkedList<ForwardConditionalBranch>();
			int myBackwardsReach = getBackwardsReach(bb.to);
			for(ForwardConditionalBranch fcb : forwardConditionalBranches) 
				if (myBackwardsReach < fcb.from && fcb.from < bb.from &&  bb.from < fcb.to)
					myForwardBranches.add(fcb);
			if (myForwardBranches.size() != 1) continue;
			ForwardConditionalBranch fcb = myForwardBranches.get(0);
			for(Jump fj : forwardJumps) 
				if (fcb.from != fj.from && myBackwardsReach < fj.from && fj.from < bb.from && bb.from < fj.to) 
					continue backwardBranchLoop;
			if (isConstant(fcb.item0, bb) && 
					isConstant(fcb.item1, bb)) {
				BugInstance bug = new BugInstance(this, "IL_INFINITE_LOOP",
						HIGH_PRIORITY).addClassAndMethod(this).addSourceLine(
						this, fcb.from).addSourceLine(
								this, bb.from).describe(SourceLineAnnotation.DESCRIPTION_LOOP_BOTTOM);
				int reg0 = fcb.item0.getRegisterNumber();
				boolean reg0Invariant = true;
				if (reg0 >= 0) {
					reg0Invariant = !isRegModified(reg0, myBackwardsReach, bb.from);
					bug.add(LocalVariableAnnotation.getLocalVariableAnnotation(getMethod(), reg0, fcb.from, bb.from))
					.addSourceLine(this, constantSince(fcb.item0)).describe(SourceLineAnnotation.DESCRIPTION_LAST_CHANGE);
				}
				int reg1 = fcb.item1.getRegisterNumber();
				if (reg1 >= 0 && reg1 != reg0) 
					bug.add(LocalVariableAnnotation.getLocalVariableAnnotation(getMethod(), reg1, fcb.from, bb.from))
										.addSourceLine(this, constantSince(fcb.item1)).describe(SourceLineAnnotation.DESCRIPTION_LAST_CHANGE);
				  boolean reg1Invariant = true;
				if (reg1 >= 0) 
					reg1Invariant = !isRegModified(reg1, myBackwardsReach, bb.from);
				if (reg0Invariant && reg1Invariant)
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
		if (reg >= 0) return bb.invariantRegisters.contains(reg) || reg >= bb.numLastUpdates;
		if (item0.getConstant() != null) return true;
		return false;
	}
	@Override
	public void sawBranchTo(int target) {
		addForwardJump(getPC(), target);
	}
	@Override
	public void sawOpcode(int seen) {
		if (false) System.out.println(getPC() + " " + OPCODE_NAMES[seen] + " " + stack);
		if (isRegisterStore())  regModifiedAt(getRegisterOperand(), getPC());
		switch (seen) {
		case GOTO:
			if (getBranchOffset() < 0) {
				BackwardsBranch bb = new BackwardsBranch(stack, getPC(), getBranchTarget());
				if (bb.invariantRegisters.size() > 0) backwardBranches.add(bb);
				addBackwardsReach();
				if (false) {
					int target = getBranchTarget();
					if (getFurthestJump(target) > getPC())
						break;
					if (getMethodName().equals("run") || getMethodName().equals("main")) break;
					BugInstance bug = new BugInstance(this, "IL_INFINITE_LOOP",
							LOW_PRIORITY).addClassAndMethod(this).addSourceLine(
									this, getPC());
					reportPossibleBug(bug);
				}
			}

			break;
		case ARETURN:
		case IRETURN:
		case RETURN:
		case DRETURN:
		case FRETURN:
		case LRETURN:
		case ATHROW:
			addForwardJump(getPC(), Integer.MAX_VALUE);
			break;

		case LOOKUPSWITCH:
		case TABLESWITCH: 
		{
			OpcodeStack.Item item0 = stack.getStackItem(0);
			if (getDefaultSwitchOffset() > 0) 
				forwardConditionalBranches.add(new ForwardConditionalBranch(item0, item0, getPC(), getPC() + getDefaultSwitchOffset() ));
			for(int offset : getSwitchOffsets()) if (offset > 0) 
				forwardConditionalBranches.add(new ForwardConditionalBranch(item0, item0, getPC(), getPC() + offset));
			break;
		}
		case IFNE:
		case IFEQ:
		case IFLE:
		case IFLT:
		case IFGE:
		case IFGT:
		case IFNONNULL:
		case IFNULL:
		{
			addBackwardsReach();
			OpcodeStack.Item item0 = stack.getStackItem(0);
			int target = getBranchTarget();
			if (getBranchOffset() > 0) {
				forwardConditionalBranches.add(new ForwardConditionalBranch(item0, item0, getPC(), target));
				break;
			}
			if (getFurthestJump(target) > getPC())
				break;

			if (constantSince(item0, target)) {
				int since0 = constantSince(item0);
				BugInstance bug = new BugInstance(this, "IL_INFINITE_LOOP",
						HIGH_PRIORITY).addClassAndMethod(this).addSourceLine(
						this, getPC());
				int reg0 = item0.getRegisterNumber();
				if (reg0 >= 0) 
					bug.add(LocalVariableAnnotation.getLocalVariableAnnotation(getMethod(), reg0, getPC(), target))
					.addSourceLine(this, since0);
				if (reg0 < 0 || !isRegModified(reg0, target, getPC()))
					reportPossibleBug(bug);

			}
		}
			break;
		case IF_ACMPEQ:
		case IF_ACMPNE:
		case IF_ICMPNE:
		case IF_ICMPEQ:
		case IF_ICMPGT:
		case IF_ICMPLE:
		case IF_ICMPLT:
		case IF_ICMPGE:
		{
			addBackwardsReach();
			OpcodeStack.Item item0 = stack.getStackItem(0);
			OpcodeStack.Item item1 = stack.getStackItem(1);
			int target = getBranchTarget();
			if (getBranchOffset() > 0) {
				forwardConditionalBranches.add(new ForwardConditionalBranch(item0, item1, getPC(), target));
				break;
			}
			if (getFurthestJump(target) > getPC())
				break;

			if (constantSince(item0, target)
					&& constantSince(item1, target)) {
				// int since0 = constantSince(item0);
				// int since1 = constantSince(item1);
				BugInstance bug = new BugInstance(this, "IL_INFINITE_LOOP",
						HIGH_PRIORITY).addClassAndMethod(this).addSourceLine(
						this, getPC());
				int reg0 = item0.getRegisterNumber();
				if (reg0 >= 0)
					bug.add(LocalVariableAnnotation.getLocalVariableAnnotation(getMethod(), reg0, getPC(), target));
				int reg1 = item1.getRegisterNumber();
				if (reg1 >= 0)
					bug.add(LocalVariableAnnotation.getLocalVariableAnnotation(getMethod(), reg1, getPC(), target));

				reportPossibleBug(bug);
				}

		}
			break;
		}

	}

	/**
	 * 
	 */
	private void addBackwardsReach() {
		if (getBranchOffset() >= 0) return;
		int target = getBranchTarget();
		for(Jump j : backwardReach) 
			if (j.to < target && target <= j.from) target = j.to;
		assert target <= getBranchTarget();
		assert target < getPC();
		for(Iterator<Jump> i = backwardReach.iterator(); i.hasNext(); ) {
			Jump j = i.next();
			if (target <= j.to && getPC() >= j.from) i.remove();
		}
		backwardReach.add(new Jump(getPC(), target));
	}

	private int getBackwardsReach(int target) {
		int originalTarget = target;
		for(Jump j : backwardReach) 
			if (j.to < target && target <= j.from) target = j.to;
		assert target <= originalTarget;
		return target;
	}



	/**
	 * @param item1
	 * @param branchTarget
	 * @return
	 */
	private boolean constantSince(Item item1, int branchTarget) {
		int reg = item1.getRegisterNumber();
		if (reg >= 0)
		return stack.getLastUpdate(reg) < getBackwardsReach(branchTarget);
		if (item1.getConstant() != null)
			return true;
		return false;
	}
	private int constantSince(Item item1) {
		int reg = item1.getRegisterNumber();
		if (reg >= 0) return stack.getLastUpdate(reg);
		return Integer.MAX_VALUE;

	}

	void reportPossibleBug(BugInstance bug) {
		int catchSize = Util.getSizeOfSurroundingTryBlock(getConstantPool(), getCode(), "java/io/EOFException", getPC());
		if (catchSize < Integer.MAX_VALUE) bug.lowerPriorityALot();
		else {
			catchSize = Util.getSizeOfSurroundingTryBlock(getConstantPool(), getCode(), "java/lang/NoSuchElementException", getPC());
			if (catchSize < Integer.MAX_VALUE) bug.lowerPriorityALot();
			else {
				LocalVariableAnnotation lv = bug.getPrimaryLocalVariableAnnotation();
				if (lv == null && getMethodName().equals("run")) bug.lowerPriority();
			} 
		}
		bugReporter.reportBug(bug);
	}
}
