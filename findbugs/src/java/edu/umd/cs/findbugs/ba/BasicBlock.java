/*
 * Bytecode Analysis Framework
 * Copyright (C) 2003, University of Maryland
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

package edu.umd.cs.daveho.ba;

import java.util.*;

// We require BCEL 5.0 or later.
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * Simple basic block abstraction for BCEL.
 *
 * <p> TODO: right now we use an ArrayList to store the instructions
 * in the basic block.  For most basic blocks, it would be sufficent
 * to just store the first and last instruction in the block,
 * since BCEL InstructionHandles have next and prev links.
 * (The explicit ArrayList is only needed to handle inlined JSR
 * subroutines.)  Avoiding the creation of the ArrayList would
 * probably save a significant amount of memory.
 *
 * @see CFG
 * @author David Hovemeyer
 */
public class BasicBlock implements Comparable {

    /* ----------------------------------------------------------------------
     * Static data
     * ---------------------------------------------------------------------- */

    /** Set of instruction opcodes that have an implicit null check. */
    private static final BitSet nullCheckInstructionSet = new BitSet();
    static {
	nullCheckInstructionSet.set(Constants.GETFIELD);
	nullCheckInstructionSet.set(Constants.PUTFIELD);
	nullCheckInstructionSet.set(Constants.INVOKESPECIAL);
	nullCheckInstructionSet.set(Constants.INVOKEVIRTUAL);
	nullCheckInstructionSet.set(Constants.INVOKEINTERFACE);
	nullCheckInstructionSet.set(Constants.AALOAD);
	nullCheckInstructionSet.set(Constants.AASTORE);
	nullCheckInstructionSet.set(Constants.BALOAD);
	nullCheckInstructionSet.set(Constants.BASTORE);
	nullCheckInstructionSet.set(Constants.CALOAD);
	nullCheckInstructionSet.set(Constants.CASTORE);
	nullCheckInstructionSet.set(Constants.DALOAD);
	nullCheckInstructionSet.set(Constants.DASTORE);
	nullCheckInstructionSet.set(Constants.FALOAD);
	nullCheckInstructionSet.set(Constants.FASTORE);
	nullCheckInstructionSet.set(Constants.IALOAD);
	nullCheckInstructionSet.set(Constants.IASTORE);
	nullCheckInstructionSet.set(Constants.LALOAD);
	nullCheckInstructionSet.set(Constants.LASTORE);
	nullCheckInstructionSet.set(Constants.SALOAD);
	nullCheckInstructionSet.set(Constants.SASTORE);
	nullCheckInstructionSet.set(Constants.MONITORENTER);
	nullCheckInstructionSet.set(Constants.MONITOREXIT);
	// FIXME: Can't handle yet: ATHROW (need a fix in BetterCFGBuilder)
	// Any others?
    }

    /* ----------------------------------------------------------------------
     * Fields
     * ---------------------------------------------------------------------- */

    private int id;
    private ArrayList<InstructionHandle> instructionList;
    private InstructionHandle exceptionThrower; // instruction for which this block is the ETB
    private CodeExceptionGen exceptionGen; // set if this block is the entry point of an exception handler
    private int numIncomingEdges;
    private int numOutgoingEdges;

    /* ----------------------------------------------------------------------
     * Public methods
     * ---------------------------------------------------------------------- */

    /**
     * Constructor.
     */
    public BasicBlock(int id) {
	this.id = id;
	instructionList = new ArrayList<InstructionHandle>();
	exceptionThrower = null;
	exceptionGen = null;
	numIncomingEdges = 0;
	numOutgoingEdges = 0;
    }

    /**
     * Get this BasicBlock's unique identifier.
     */
    public int getId() {
	return id;
    }

    /** Get the number of incoming edges. */
    public int getNumIncomingEdges() { return numIncomingEdges; }

    /**
     * Set the number of incoming edges.
     * @param numIncomingEdges the number of incoming edges
     */
    void setNumIncomingEdges(int numIncomingEdges) {
	this.numIncomingEdges = numIncomingEdges;
    }

    /** Get the number of outgoing edges */
    public int getNumOutgoingEdges() { return numOutgoingEdges; }

    /**
     * Set the number of outgoing edges.
     * @param numOutgoingEdges the number of outgoing edges
     */
    void setNumOutgoingEdges(int numOutgoingEdges) {
	this.numOutgoingEdges = numOutgoingEdges;
    }

    /**
     * Set the instruction for which this block is the ETB.
     * @param exceptionThrower the instruction
     */
    public void setExceptionThrower(InstructionHandle exceptionThrower) {
	this.exceptionThrower = exceptionThrower;
    }

    /**
     * Return whether or not this block is an exception thrower.
     */
    public boolean isExceptionThrower() {
	return exceptionThrower != null;
    }

    /**
     * Return whether or not this block is a null check.
     */
    public boolean isNullCheck() {
	// Null check blocks must be exception throwers,
	// and are always empty.  (The only kind of non-empty
	// exception throwing block is one terminated by an ATHROW).
	if (!isExceptionThrower() || getFirstInstruction() != null)
	    return false;
	short opcode = exceptionThrower.getInstruction().getOpcode();
	return nullCheckInstructionSet.get(opcode);
    }

    /** Get the first instruction in the basic block. */
    public InstructionHandle getFirstInstruction() {
	return instructionList.isEmpty() ? null : instructionList.get(0);
    }

    /** Get the last instruction in the basic block. */
    public InstructionHandle getLastInstruction() {
	return instructionList.isEmpty() ? null : instructionList.get(instructionList.size() - 1);
    }

    /**
     * Add an InstructionHandle to the basic block.
     * @param handle the InstructionHandle
     */
    public void addInstruction(InstructionHandle handle) {
	assert handle != getLastInstruction();
	instructionList.add(handle);
    }

    /**
     * A forward Iterator over the instructions of a basic block.
     * The duplicate() method can be used to make an exact copy of
     * this iterator.  Calling next() on the duplicate will not affect
     * the original, and vice versa.
     */
    public class InstructionIterator implements Iterator<InstructionHandle> {
	private int index = 0;

	public boolean hasNext() {
	    return index < instructionList.size();
	}

	public InstructionHandle next() {
	    if (!hasNext())
		throw new NoSuchElementException();
	    InstructionHandle handle = instructionList.get(index);
	    ++index;
	    return handle;
	}

	public void remove() {
	    throw new UnsupportedOperationException();
	}

	public InstructionIterator duplicate() {
	    InstructionIterator dup = new InstructionIterator();
	    dup.index = this.index;
	    return dup;
	}

	public boolean equals(Object o) {
	    if (!(o instanceof InstructionIterator))
		return false;
	    InstructionIterator other = (InstructionIterator) o;
	    return index == other.index && getBasicBlock() == other.getBasicBlock();
	}

	public int hashCode() {
	    return getBasicBlock().hashCode() + index;
	}

	private BasicBlock getBasicBlock() {
	    return BasicBlock.this;
	}

	public String toString() {
	    StringBuffer buf = new StringBuffer();
	    buf.append("[basicBlock=");
	    buf.append(getBasicBlock().getId());
	    buf.append(", index=");
	    buf.append(index);
	    buf.append(']');
	    return buf.toString();
	}
    }

    /**
     * Get an Iterator over the instructions in the basic block.
     */
    public InstructionIterator instructionIterator() {
	return new InstructionIterator();
    }

    /**
     * Get an Iterator over the instructions in the basic block in reverse order.
     * This is useful for backwards dataflow analyses.
     */
    public Iterator<InstructionHandle> instructionReverseIterator() {
	return new Iterator<InstructionHandle>() {
	    private ListIterator<InstructionHandle> realIter = instructionList.listIterator(instructionList.size());

	    public boolean hasNext() {
		return realIter.hasPrevious();
	    }

	    public InstructionHandle next() throws NoSuchElementException {
		return realIter.previous();
	    }

	    public void remove() {
		throw new UnsupportedOperationException();
	    }
	};
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
