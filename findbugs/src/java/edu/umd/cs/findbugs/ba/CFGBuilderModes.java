package edu.umd.cs.daveho.ba;

/**
 * Modes which can be set in BasicCFGBuilder to affect how the CFG is constructed.
 *
 * @see BasicCFGBuilder
 * @author David Hovemeyer
 */
public interface CFGBuilderModes {
	/**
	 * Normal CFG Building mode: basic blocks end at "ordinary" control flow constructs.
	 */
	public static final int NORMAL_MODE = 0;

	/**
	 * Special CFG Building mode: each instruction is a separate basic block.
	 * This is useful for running dataflow analysis on each instruction individually;
	 * for example, determine types for the operand stack and local variables.
	 */
	public static final int INSTRUCTION_MODE = 1;

	/**
	 * Special CFG building mode: instructions which can throw exceptions end
	 * basic blocks.  This mode is needed for performing dataflow analyses
	 * where we want accurate information about exception handlers.
	 */
	public static final int EXCEPTION_SENSITIVE_MODE = 2;
}

// vim:ts=4
