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
