/*
 * Bytecode Analysis Framework
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

package edu.umd.cs.findbugs.ba;

import java.util.Iterator;
import java.util.LinkedList;

import org.apache.bcel.generic.ATHROW;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.GotoInstruction;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.IfInstruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.ReturnInstruction;
import org.apache.bcel.generic.Select;

/**
 * Visitor to find all of the targets of an instruction
 * whose InstructionHandle is given.
 * Note that we don't consider exception edges.
 *
 * @author David Hovemeyer
 * @author Chadd Williams
 */
public class TargetEnumeratingVisitor extends org.apache.bcel.generic.EmptyVisitor
		implements EdgeTypes {

	private InstructionHandle handle;
	private ConstantPoolGen constPoolGen;
	private LinkedList<Target> targetList;
	private boolean isBranch, isReturn, isThrow, isExit;

	/**
	 * Constructor.
	 *
	 * @param handle       the handle of the instruction whose targets should be enumerated
	 * @param constPoolGen the ConstantPoolGen object for the class
	 */
	public TargetEnumeratingVisitor(InstructionHandle handle, ConstantPoolGen constPoolGen) {
		this.handle = handle;
		this.constPoolGen = constPoolGen;
		targetList = new LinkedList<Target>();
		isBranch = isReturn = isThrow = isExit = false;

		handle.getInstruction().accept(this);
	}

	/**
	 * Is the instruction the end of a basic block?
	 */
	public boolean isEndOfBasicBlock() {
		return isBranch || isReturn || isThrow || isExit;
	}

	/**
	 * Is the analyzed instruction a method return?
	 */
	public boolean instructionIsReturn() {
		return isReturn;
	}

	/**
	 * Is the analyzed instruction an explicit throw?
	 */
	public boolean instructionIsThrow() {
		return isThrow;
	}

	/**
	 * Is the analyzed instruction an exit (call to System.exit())?
	 */
	public boolean instructionIsExit() {
		return isExit;
	}

	/**
	 * Iterate over Target objects representing control flow targets
	 * and their edge types.
	 */
	public Iterator<Target> targetIterator() {
		return targetList.iterator();
	}

	@Override
		 public void visitGotoInstruction(GotoInstruction ins) {
		isBranch = true;
		InstructionHandle target = ins.getTarget();
		if (target == null) throw new IllegalStateException();
		targetList.add(new Target(target, GOTO_EDGE));
	}

	@Override
		 public void visitIfInstruction(IfInstruction ins) {
		isBranch = true;
		InstructionHandle target = ins.getTarget();
		if (target == null) throw new IllegalStateException();
		targetList.add(new Target(target, IFCMP_EDGE));
		InstructionHandle fallThrough = handle.getNext();
		targetList.add(new Target(fallThrough, FALL_THROUGH_EDGE));
	}

	@Override
		 public void visitSelect(Select ins) {
		isBranch = true;

		// Add non-default switch edges.
		InstructionHandle[] targets = ins.getTargets();
		for (InstructionHandle target : targets) {
			targetList.add(new Target(target, SWITCH_EDGE));
		}

		// Add default switch edge.
		InstructionHandle defaultTarget = ins.getTarget();
		if (defaultTarget == null) {
			throw new IllegalStateException();
		}
		targetList.add(new Target(defaultTarget, SWITCH_DEFAULT_EDGE));
	}

	@Override
		 public void visitReturnInstruction(ReturnInstruction ins) {
		isReturn = true;
	}

	@Override
		 public void visitATHROW(ATHROW ins) {
		isThrow = true;
	}

	@Override
		 public void visitINVOKESTATIC(INVOKESTATIC ins) {
		// Find calls to System.exit(), since this effectively terminates the basic block.

		String className = ins.getClassName(constPoolGen);
		String methodName = ins.getName(constPoolGen);
		String methodSig = ins.getSignature(constPoolGen);

		if (className.equals("java.lang.System") && methodName.equals("exit") && methodSig.equals("(I)V"))
			isExit = true;
	}

}
