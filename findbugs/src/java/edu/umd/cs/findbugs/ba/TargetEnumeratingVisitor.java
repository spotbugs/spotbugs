package edu.umd.cs.daveho.ba;

import java.util.*;

// We require BCEL 5.0 or later.
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * Visitor to find all of the targets of an instruction
 * whose InstructionHandle is given.
 * Note that we don't consider exception edges.
 */
public class TargetEnumeratingVisitor extends org.apache.bcel.generic.EmptyVisitor
    implements EdgeTypes {

    private InstructionHandle handle;
    private ConstantPoolGen constPoolGen;
    private LinkedList<Target> targetList;
    private boolean isBranch, isReturn, isThrow, isExit;

    /**
     * Constructor.
     * @param handle the handle of the instruction whose targets should be enumerated
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

    public void visitGotoInstruction(GotoInstruction ins) {
	isBranch = true;
	InstructionHandle target = ins.getTarget();
	if (target == null) throw new IllegalStateException();
	targetList.add(new Target(target, GOTO_EDGE));
    }

    public void visitIfInstruction(IfInstruction ins) {
	isBranch = true;
	InstructionHandle target = ins.getTarget();
	if (target == null) throw new IllegalStateException();
	targetList.add(new Target(target, IFCMP_EDGE));
	InstructionHandle fallThrough = handle.getNext();
	targetList.add(new Target(fallThrough, FALL_THROUGH_EDGE));
    }

    public void visitSelect(Select ins) {
	isBranch = true;

	// Add non-default switch edges.
	InstructionHandle[] targets = ins.getTargets();
	for (int i = 0; i < targets.length; ++i) {
	    targetList.add(new Target(targets[i], SWITCH_EDGE));
	}

	// Add default switch edge.
	InstructionHandle defaultTarget = ins.getTarget();
	if(defaultTarget == null){
		throw new IllegalStateException();
	}
	targetList.add(new Target(defaultTarget, SWITCH_DEFAULT_EDGE));
    }

    public void visitReturnInstruction(ReturnInstruction ins) {
	isReturn = true;
    }

    public void visitATHROW(ATHROW ins) {
	isThrow = true;
    }

    public void visitINVOKESTATIC(INVOKESTATIC ins) {
	// Find calls to System.exit(), since this effectively terminates the basic block.

	String className = ins.getClassName(constPoolGen);
	String methodName = ins.getName(constPoolGen);
	String methodSig = ins.getSignature(constPoolGen);

	if (className.equals("java.lang.System") && methodName.equals("exit") && methodSig.equals("(I)V"))
	    isExit = true;
    }

}
