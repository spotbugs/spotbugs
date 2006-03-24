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

package edu.umd.cs.findbugs.ba.bcp;

import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.MONITORENTER;

import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

/**
 * A PatternElement for matching a MONITORENTER instruction.
 *
 * @author DavidHovemeyer
 */
public class Monitorenter extends OneVariableInstruction {
	/**
	 * Constructor.
	 *
	 * @param varName name of the variable representing the reference
	 *                to the object being locked
	 */
	public Monitorenter(String varName) {
		super(varName);
	}

	@Override
         public MatchResult match(InstructionHandle handle, ConstantPoolGen cpg,
	                         ValueNumberFrame before, ValueNumberFrame after, BindingSet bindingSet) throws DataflowAnalysisException {

		// Instruction must be MONITORENTER.
		Instruction ins = handle.getInstruction();
		if (!(ins instanceof MONITORENTER))
			return null;

		// Ensure the object being locked matches any previous
		// instructions which bound our variable name to a value.
		Variable lock = new LocalVariable(before.getTopValue());
		return addOrCheckDefinition(lock, bindingSet);
	}
}

// vim:ts=4
