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

import java.util.*;
import org.apache.bcel.generic.InstructionHandle;

public class PatternElementMatch {
	private PatternElement patternElement;
	private List<InstructionHandle> matchedInstructionList;

	public PatternElementMatch(PatternElement patternElement) {
		this.patternElement = patternElement;
		this.matchedInstructionList = new LinkedList<InstructionHandle>();
	}

	public void addMatchedInstruction(InstructionHandle handle) {
		matchedInstructionList.add(handle);
	}

	public int getNumMatchedInstructions() {
		return matchedInstructionList.size();
	}

	// TODO: add a duplicate() method
}

// vim:ts=4
