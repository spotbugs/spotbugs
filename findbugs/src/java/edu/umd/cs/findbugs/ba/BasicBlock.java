package edu.umd.cs.daveho.ba;

import java.util.*;

// We require BCEL 5.0 or later.
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * Simple basic block abstraction for BCEL.
 * Does not take exception control edges into account.
 * @see CFG
 */
public class BasicBlock implements Comparable {
    private int id;
    private LinkedList<InstructionHandle> instructionList;
    private CodeExceptionGen exceptionGen; // set if this block is the entry point of an exception handler

    /**
     * Constructor.
     */
    public BasicBlock(int id) {
	this.id = id;
	instructionList = new LinkedList<InstructionHandle>();
	exceptionGen = null;
    }

    /**
     * Get this BasicBlock's unique identifier.
     */
    public int getId() {
	return id;
    }

    /** Get the first instruction in the basic block. */
    public InstructionHandle getFirstInstruction() {
	return instructionList.isEmpty() ? null : instructionList.getFirst();
    }

    /** Get the last instruction in the basic block. */
    public InstructionHandle getLastInstruction() {
	return instructionList.isEmpty() ? null : instructionList.getLast();
    }

    /**
     * Add an InstructionHandle to the basic block.
     * @param handle the InstructionHandle
     */
    public void addInstruction(InstructionHandle handle) {
	// Only add the instruction if it hasn't already been added.
	if (instructionList.isEmpty() || instructionList.getLast() != handle)
	    instructionList.addLast(handle);
    }

    /**
     * Get an Iterator over the instructions in the basic block.
     */
    public Iterator<InstructionHandle> instructionIterator() {
	return instructionList.iterator();
    }

    private static final PCRange[] EMPTY_RANGE_LIST = new PCRange[0];

    /**
     * Get the ranges of instructions constituting this basic block.
     * @return array of PCRange objects representing all instruction ranges
     *   in this basic block (because of JSR/RET, there may be multiple
     *   discontiguous ranges)
     */
    public PCRange[] getRangeList() {
	ArrayList<PCRange> rangeList = new ArrayList<PCRange>();

	Iterator<InstructionHandle> iter = instructionIterator();
	if (!iter.hasNext())
	    return EMPTY_RANGE_LIST;

	InstructionHandle first = iter.next();
	InstructionHandle current = first, prev = null;

	while (true) {
	    // Start of new range?
	    if (prev != null && current != prev.getNext()) {
		rangeList.add(new PCRange(first, prev));
		first = current;
	    }

	    // End of list?
	    if (!iter.hasNext()) {
		rangeList.add(new PCRange(first, current));
		break;
	    }

	    // Continuation of current range
	    prev = current;

	    // Advance to next instruction in block
	    current = iter.next();
	}

	return (PCRange[]) rangeList.toArray(EMPTY_RANGE_LIST);
    }

    /**
     * Return true if there are no Instructions in this basic block.
     */
    public boolean isEmpty() {
	return instructionList.isEmpty();
    }

    /**
     * For implementation of Comparable interface.
     * Basic blocks are ordered by their unique id.
     */
    public int compareTo(Object o) {
	BasicBlock other = (BasicBlock) o;
	return this.id - other.id;
    }

    /** Is this block an exception handler? */
    public boolean isExceptionHandler() { return exceptionGen != null; }

    /** Get CodeExceptionGen object; returns null if this basic block is
        not the entry point of an exception handler. */
    public CodeExceptionGen getExceptionGen() {
	return exceptionGen;
    }

    /** Set the CodeExceptionGen object.  Marks this basic block as
       the entry point of an exception handler. */
    public void setExceptionGen(CodeExceptionGen exceptionGen) {
	this.exceptionGen = exceptionGen;
    }

}
