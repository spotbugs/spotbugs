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

import java.util.BitSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.bcel.Constants;
import org.apache.bcel.generic.CodeExceptionGen;
import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.graph.AbstractVertex;

/**
 * Simple basic block abstraction for BCEL.
 *
 * @author David Hovemeyer
 * @see CFG
 */
public class BasicBlock extends AbstractVertex<Edge, BasicBlock> implements Debug {

	/* ----------------------------------------------------------------------
	 * Static data
	 * ---------------------------------------------------------------------- */

	/**
	 * Set of instruction opcodes that have an implicit null check.
	 */
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
		nullCheckInstructionSet.set(Constants.ARRAYLENGTH);
		// nullCheckInstructionSet.set(Constants.MONITOREXIT);
		//nullCheckInstructionSet.set(Constants.ATHROW);
		// Any others?
	}

	/* ----------------------------------------------------------------------
	 * Fields
	 * ---------------------------------------------------------------------- */

	private InstructionHandle firstInstruction;
	private InstructionHandle lastInstruction;
	private InstructionHandle exceptionThrower; // instruction for which this block is the ETB
	private CodeExceptionGen exceptionGen; // set if this block is the entry point of an exception handler
	private boolean inJSRSubroutine;
	
	/* ----------------------------------------------------------------------
	 * Public methods
	 * ---------------------------------------------------------------------- */

	/**
	 * Constructor.
	 */
	public BasicBlock() {
		this.firstInstruction = null;
		this.lastInstruction = null;
		this.exceptionThrower = null;
		this.exceptionGen = null;
		this.inJSRSubroutine = false;
	}
	
	public boolean isInJSRSubroutine() {
		return inJSRSubroutine;
	}
	
	void setInJSRSubroutine(boolean inJSRSubroutine) {
		this.inJSRSubroutine = inJSRSubroutine;
	}

	public int getId() {
		return getLabel();
	}
	
	@Override
         public String toString() {
		return "block " + String.valueOf(getLabel());
	}

	/**
	 * Set the instruction for which this block is the ETB.
	 *
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
	 *
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

	/**
	 * Get the first instruction in the basic block.
	 */
	public InstructionHandle getFirstInstruction() {
		return firstInstruction;
	}

	/**
	 * Get the last instruction in the basic block.
	 */
	public InstructionHandle getLastInstruction() {
		return lastInstruction;
	}

	/**
	 * Get the successor of given instruction within the basic block.
	 * @param handle the instruction
	 * @return the instruction's successor, or null if the instruction
	 *         is the last in the basic block
	 */
	public InstructionHandle getSuccessorOf(InstructionHandle handle) {
		if (VERIFY_INTEGRITY) {
			if (!containsInstruction(handle))
				throw new IllegalStateException();
		}
		return handle == lastInstruction
			? null
			: handle.getNext();
	}

	/**
	 * Get the predecessor of given instruction within the basic block.
	 * @param handle the instruction
	 * @return the instruction's predecessor, or null if the instruction
	 *         is the first in the basic block
	 */
	public InstructionHandle getPredecessorOf(InstructionHandle handle) {
		if (VERIFY_INTEGRITY) {
			if (!containsInstruction(handle))
				throw new IllegalStateException();
		}
		return handle == firstInstruction
			? null
			: handle.getPrev();
	}

	/**
	 * Add an InstructionHandle to the basic block.
	 *
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

		@Override
                 public boolean equals(Object o) {
			if (!(o instanceof InstructionIterator))
				return false;
			InstructionIterator other = (InstructionIterator) o;
			return this.next == other.next && this.last == other.last;
		}

		@Override
                 public int hashCode() {
			int code = getBasicBlock().hashCode() * 227;
			if (next != null)
				code += next.getPosition() + 1;
			return code;
		}

		private BasicBlock getBasicBlock() {
			return BasicBlock.this;
		}

		@Override
                 public String toString() {
			StringBuffer buf = new StringBuffer();
			buf.append("[basicBlock=");
			buf.append(getBasicBlock().getId());
			buf.append(", index=");
			buf.append(next == null ? "end" : String.valueOf(next.getPosition()));
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
	 * Is this block an exception handler?
	 */
	public boolean isExceptionHandler() {
		return exceptionGen != null;
	}

	/**
	 * Get CodeExceptionGen object; returns null if this basic block is
	 * not the entry point of an exception handler.
	 *
	 * @return the CodeExceptionGen object, or null
	 */
	public CodeExceptionGen getExceptionGen() {
		return exceptionGen;
	}

	/**
	 * Set the CodeExceptionGen object.  Marks this basic block as
	 * the entry point of an exception handler.
	 *
	 * @param exceptionGen the CodeExceptionGen object for the block
	 */
	public void setExceptionGen(CodeExceptionGen exceptionGen) {
		this.exceptionGen = exceptionGen;
	}

	/**
	 * Return whether or not the basic block contains the given instruction.
	 *
	 * @param handle the instruction
	 * @return true if the block contains the instruction, false otherwise
	 */
	public boolean containsInstruction(InstructionHandle handle) {
		Iterator<InstructionHandle> i = instructionIterator();
		while (i.hasNext()) {
			if (i.next() == handle)
				return true;
		}
		return false;
	}

	/**
	 * Return whether or not the basic block contains the instruction
	 * with the given bytecode offset.
	 *
	 * @param offset the bytecode offset
	 * @return true if the block contains an instruction with the given offset,
	 *         false if it does not
	 */
	public boolean containsInstructionWithOffset(int offset) {
		Iterator<InstructionHandle> i = instructionIterator();
		while (i.hasNext()) {
			if (i.next().getPosition() == offset)
				return true;
		}
		return false;
	}
}

// vim:ts=4
