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
import org.apache.bcel.Constants;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * Simple basic block abstraction for BCEL.
 *
 * @see CFG
 * @author David Hovemeyer
 */
public class BasicBlock implements Comparable, Debug {

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
		//nullCheckInstructionSet.set(Constants.ATHROW);
		// Any others?
	}

	/* ----------------------------------------------------------------------
	 * Fields
	 * ---------------------------------------------------------------------- */

	private int id;
	private InstructionHandle firstInstruction;
	private InstructionHandle lastInstruction;
	private InstructionHandle exceptionThrower; // instruction for which this block is the ETB
	private CodeExceptionGen exceptionGen; // set if this block is the entry point of an exception handler
	private Edge firstIncomingEdge, lastIncomingEdge;
	private Edge firstOutgoingEdge, lastOutgoingEdge;

	/* ----------------------------------------------------------------------
	 * Public methods
	 * ---------------------------------------------------------------------- */

	/**
	 * Constructor.
	 */
	public BasicBlock(int id) {
		this.id = id;
		this.firstInstruction = null;
		this.lastInstruction = null;
		this.exceptionThrower = null;
		this.exceptionGen = null;

		// Each block maintains a list of incoming and outgoing edges.
		// These are used by the CFG class.
		this.firstIncomingEdge = null;
		this.lastIncomingEdge = null;
		this.firstOutgoingEdge = null;
		this.lastOutgoingEdge = null;
	}

	/**
	 * Get this BasicBlock's unique identifier.
	 */
	public int getId() {
		return id;
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
	 * Get the instruction for which this block is an exception thrower.
	 * @return the instruction, or null if this block is not an exception thrower
	 */
	public InstructionHandle getExceptionThrower() {
		return exceptionThrower;
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
		return firstInstruction;
	}

	/** Get the last instruction in the basic block. */
	public InstructionHandle getLastInstruction() {
		return lastInstruction;
	}

	/**
	 * Add an InstructionHandle to the basic block.
	 * @param handle the InstructionHandle
	 */
	public void addInstruction(InstructionHandle handle) {
		if (firstInstruction == null) {
			firstInstruction = lastInstruction = handle;
		} else {
			if (VERIFY_INTEGRITY && handle != lastInstruction.getNext())
				throw new IllegalStateException("Adding non-consecutive instruction");
			lastInstruction = handle;
		}
	}

	/**
	 * A forward Iterator over the instructions of a basic block.
	 * The duplicate() method can be used to make an exact copy of
	 * this iterator.  Calling next() on the duplicate will not affect
	 * the original, and vice versa.
	 */
	public class InstructionIterator implements Iterator<InstructionHandle> {
		private InstructionHandle next, last;

		public InstructionIterator(InstructionHandle first, InstructionHandle last) {
			this.next = first;
			this.last = last;
		}

		public boolean hasNext() {
			return next != null;
		}

		public InstructionHandle next() {
			if (!hasNext())
				throw new NoSuchElementException();
			InstructionHandle result = next;
			next = (result == last) ? null : next.getNext();
			return result;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}

		public InstructionIterator duplicate() {
			return new InstructionIterator(next, last);
		}

		public boolean equals(Object o) {
			if (!(o instanceof InstructionIterator))
				return false;
			InstructionIterator other = (InstructionIterator) o;
			return this.next == other.next && this.last == other.last;
		}

		public int hashCode() {
			int code = getBasicBlock().hashCode() * 227;
			if (next != null)
				code += next.getPosition() + 1;
			return code;
		}

		private BasicBlock getBasicBlock() {
			return BasicBlock.this;
		}

		public String toString() {
			StringBuffer buf = new StringBuffer();
			buf.append("[basicBlock=");
			buf.append(getBasicBlock().getId());
			buf.append(", index=");
			buf.append(next != null ? "end" : String.valueOf(next.getPosition()));
			buf.append(']');
			return buf.toString();
		}
	}

	/**
	 * Get an Iterator over the instructions in the basic block.
	 */
	public InstructionIterator instructionIterator() {
		return new InstructionIterator(firstInstruction, lastInstruction);
	}

	/**
	 * A reverse Iterator over the instructions in a basic block.
	 */
	private static class InstructionReverseIterator implements Iterator<InstructionHandle> {
		private InstructionHandle next, first;

		public InstructionReverseIterator(InstructionHandle last, InstructionHandle first) {
			this.next = last;
			this.first = first;
		}

		public boolean hasNext() {
			return next != null;
		}

		public InstructionHandle next() throws NoSuchElementException {
			if (!hasNext())
				throw new NoSuchElementException();
			InstructionHandle result = next;
			next = (result == first) ? null : next.getPrev();
			return result;
		}

		public void remove() {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Get an Iterator over the instructions in the basic block in reverse order.
	 * This is useful for backwards dataflow analyses.
	 */
	public Iterator<InstructionHandle> instructionReverseIterator() {
		return new InstructionReverseIterator(lastInstruction, firstInstruction);
	}

	/**
	 * Return true if there are no Instructions in this basic block.
	 */
	public boolean isEmpty() {
		return firstInstruction == null;
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

	/**
	 * Get CodeExceptionGen object; returns null if this basic block is
	 * not the entry point of an exception handler.
	 * @return the CodeExceptionGen object, or null
	 */
	public CodeExceptionGen getExceptionGen() {
		return exceptionGen;
	}

	/**
	 * Set the CodeExceptionGen object.  Marks this basic block as
	 * the entry point of an exception handler.
	 * @param exceptionGen the CodeExceptionGen object for the block
	 */
	public void setExceptionGen(CodeExceptionGen exceptionGen) {
		this.exceptionGen = exceptionGen;
	}

	/**
	 * Add an outgoing edge to the basic block.
	 * This should only be used by the CFG class.
	 * @param edge the outgoing Edge
	 */
	void addOutgoingEdge(Edge edge) {
		if (VERIFY_INTEGRITY && edge.getSource() != this)
			throw new IllegalArgumentException();

		if (firstOutgoingEdge == null) {
			firstOutgoingEdge = lastOutgoingEdge = edge;
		} else {
			lastOutgoingEdge.setNextOutgoingEdge(edge);
			lastOutgoingEdge = edge;
		}
	}

	/**
	 * Get the first outgoing edge from the basic block.
	 * This should only be used by the CFG class.
	 */
	Edge getFirstOutgoingEdge() {
		return firstOutgoingEdge;
	}

	/**
	 * Add an incoming edge to the basic block.
	 * This should only be used by the CFG class.
	 * @param edge the incoming Edge
	 */
	void addIncomingEdge(Edge edge) {
		if (VERIFY_INTEGRITY && edge.getDest() != this)
			throw new IllegalArgumentException();

		if (firstIncomingEdge == null) {
			firstIncomingEdge = lastIncomingEdge = edge;
		} else {
			lastIncomingEdge.setNextIncomingEdge(edge);
			lastIncomingEdge = edge;
		}
	}

	/**
	 * Get the first incoming edge to the basic block.
	 * This should only be used by the CFG class.
	 */
	Edge getFirstIncomingEdge() {
		return firstIncomingEdge;
	}

	/**
	 * Remove an incoming edge.
	 * @param edge the incoming edge
	 */
	void removeIncomingEdge(Edge edge) {
		Edge prev = null, cur = firstIncomingEdge;
		while (cur != null) {
			Edge next = cur.getNextIncomingEdge();
			if (cur.equals(edge)) {
				if (prev != null)
					prev.setNextIncomingEdge(next);
				else
					firstIncomingEdge = next;
				return;
			}
			prev = cur;
			cur = next;
		}
		throw new IllegalArgumentException("removing nonexistent edge!");
	}

	/**
	 * Remove an outgoing edge.
	 * @param edge the outgoing edge
	 */
	void removeOutgoingEdge(Edge edge) {
		Edge prev = null, cur = firstOutgoingEdge;
		while (cur != null) {
			Edge next = cur.getNextOutgoingEdge();
			if (cur.equals(edge)) {
				if (prev != null)
					prev.setNextOutgoingEdge(next);
				else
					firstOutgoingEdge = next;
				return;
			}
			prev = cur;
			cur = cur.getNextOutgoingEdge();
		}
		throw new IllegalArgumentException("removing nonexistent edge!");
	}

}

// vim:ts=4
