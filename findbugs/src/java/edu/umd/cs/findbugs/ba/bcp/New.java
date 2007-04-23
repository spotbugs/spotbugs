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
import org.apache.bcel.generic.NEW;

import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

/**
 * A PatternElement which matches NEW instructions and binds the
 * result to a variable.
 *
 * @author David Hovemeyer
 * @see PatternElement
 */
public class New extends OneVariableInstruction {
	/**
	 * Constructor.
	 *
	 * @param resultVarName name of the result of the NEW instruction
	 */
	public New(String resultVarName) {
		super(resultVarName);
	}

	@Override
		 public MatchResult match(InstructionHandle handle, ConstantPoolGen cpg,
							 ValueNumberFrame before, ValueNumberFrame after, BindingSet bindingSet) throws DataflowAnalysisException {

		Instruction ins = handle.getInstruction();
		if (!(ins instanceof NEW))
			return null;

		LocalVariable result = new LocalVariable(after.getTopValue());
		return addOrCheckDefinition(result, bindingSet);
	}
}

// vim:ts=4
