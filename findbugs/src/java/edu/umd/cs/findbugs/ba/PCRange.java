package edu.umd.cs.daveho.ba;

import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * Represents a range of instructions within a method.
 */
public class PCRange {
	/** The first instruction in the range (inclusive). */
	public final InstructionHandle first;

	/** The last instruction in the range (inclusive). */
	public final InstructionHandle last;

	/**
	 * Constructor.
	 * @param first first instruction in the range (inclusive)
	 * @param last last instruction in the range (inclusive)
	 */
	public PCRange(InstructionHandle first, InstructionHandle last) {
		this.first = first;
		this.last = last;
	}
}

// vim:ts=4
