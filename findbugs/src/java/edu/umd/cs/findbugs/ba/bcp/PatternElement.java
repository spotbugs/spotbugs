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

package edu.umd.cs.daveho.ba.bcp;

import org.apache.bcel.generic.InstructionHandle;

/**
 * A PatternElement is an element of a ByteCodePattern.
 * It potentially matches some number of bytecode instructions.
 */
public interface PatternElement {
	/**
	 * Return whether or not this element matches the given
	 * instruction with the given Bindings in effect.
	 * @param handle the instruction
	 * @param bindingSet the set of Bindings
	 * @return if the match is successful, returns an updated BindingSet;
	 *   if the match is not successful, returns null
	 */
	public BindingSet match(InstructionHandle handle, BindingSet bindingSet);

	/**
	 * Return the minimum number of instructions this PatternElement
	 * must match in the ByteCodePattern.
	 */
	public int minOccur();

	/**
	 * Return the maximum number of instructions this PatternElement
	 * must match in the ByteCodePattern.
	 */
	public int maxOccur();
}

// vim:ts=4
