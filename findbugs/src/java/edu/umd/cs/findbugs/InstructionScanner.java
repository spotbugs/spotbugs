package edu.umd.cs.findbugs;

// We require BCEL 5.1 or later.
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

import edu.umd.cs.daveho.ba.*;

/**
 * A scanner for implementing a state machine over a sequence
 * of instructions and control edges.
 */
public interface InstructionScanner {
	/**
	 * Traverse an edge.
	 */
	public void traverseEdge(Edge edge);
	/**
	 * Traverse an instruction.
	 */
	public void scanInstruction(Instruction ins);
	/**
	 * Return true if this scanner has completed, false otherwise.
	 */
	public boolean isDone();
}

// vim:ts=4
