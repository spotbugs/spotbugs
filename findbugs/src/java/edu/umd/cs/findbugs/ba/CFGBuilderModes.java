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

/**
 * Modes which can be set in CFGBuilder to affect how the CFG is constructed.
 *
 * @see CFGBuilder
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
