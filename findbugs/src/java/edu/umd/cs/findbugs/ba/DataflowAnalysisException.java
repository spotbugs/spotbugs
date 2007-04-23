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

import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MethodGen;

import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;

/**
 * Exception type to indicate a dataflow analysis failure.
 *
 * @see Dataflow
 * @see DataflowAnalysis
 */
public class DataflowAnalysisException extends CheckedAnalysisException {
	private static final long serialVersionUID = 3690480212240446258L;

	/**
	 * Constructor.
	 */
	public DataflowAnalysisException() {
	}

	/**
	 * Constructor.
	 *
	 * @param msg message describing the reason for the exception
	 */
	public DataflowAnalysisException(String msg) {
		super(msg);
	}

	/**
	 * Constructor from message and another Throwable object.
	 *
	 * @param msg   message describing the reason for the exception
	 * @param cause a Throwable which is the cause of the exception
	 */
	public DataflowAnalysisException(String msg, Throwable cause) {
		super(msg, cause);
	}

	/**
	 * Constructor from method and instruction.
	 *
	 * @param message   reason for the error
	 * @param methodGen the method
	 * @param handle    the instruction
	 */
	public DataflowAnalysisException(String message, MethodGen methodGen, InstructionHandle handle) {
		super(message + " in " + SignatureConverter.convertMethodSignature(methodGen) + " at " + handle);
	}

	/**
	 * Constructor from message, method and instruction, and Throwable object (cause).
	 * 
	 * @param message   reason for the error
	 * @param methodGen the method
	 * @param handle    the instruction
	 * @param cause     a Throwable which is the cause of the exception
	 */
	public DataflowAnalysisException(
			String message, MethodGen methodGen, InstructionHandle handle, Throwable cause) {
		this(message, methodGen, handle);
		this.initCause(cause);
	}
}

// vim:ts=4
