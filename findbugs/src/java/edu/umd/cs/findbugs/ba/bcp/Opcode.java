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

import org.apache.bcel.generic.*;
import edu.umd.cs.daveho.ba.*;

/**
 * PatternElement to match instructions with a particular opcode.
 * @see PatternElement
 * @author David Hovemeyer
 */
public class Opcode extends PatternElement {
	private int opcode;

	/**
	 * Constructor.
	 * @param opcode the opcode to match
	 */
	public Opcode(int opcode) { this.opcode = opcode; }

	public MatchResult match(InstructionHandle handle, ConstantPoolGen cpg,
		ValueNumberFrame before, ValueNumberFrame after, BindingSet bindingSet) throws DataflowAnalysisException {

		if (handle.getInstruction().getOpcode() == opcode)
			return new MatchResult(this, bindingSet);
		else
			return null;

	}

	public boolean acceptBranch(Edge edge, InstructionHandle source) { return true; }
	public int minOccur() { return 1; }
	public int maxOccur() { return 1; }
}

// vim:ts=4
