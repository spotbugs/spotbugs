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
 * A PatternElement representing a load from a field.
 * Variables represent the field and the result of the load.
 *
 * @see PatternElement
 * @author David Hovemeyer
 */
public class Load extends SingleInstruction {
	private String fieldVarName;
	private String resultVarName;

	/**
	 * Constructor.
	 * @param fieldVarName the name of the field variable
	 * @param resultVarName the name of the result variable
	 */
	public Load(String fieldVarName, String resultVarName) {
		this.fieldVarName = fieldVarName;
		this.resultVarName = resultVarName;
	}

	public BindingSet match(InstructionHandle handle, ConstantPoolGen cpg,
		ValueNumberFrame before, ValueNumberFrame after, BindingSet bindingSet) throws DataflowAnalysisException {

		Variable field;
		Instruction ins = handle.getInstruction();

		// The instruction must be GETFIELD or GETSTATIC
		if (ins instanceof GETFIELD) {
			GETFIELD getfield = (GETFIELD) ins;
			ValueNumber ref = before.getTopValue();
			field = new FieldVariable(ref, getfield.getClassName(cpg), getfield.getFieldName(cpg));
		} else if (ins instanceof GETSTATIC) {
			GETSTATIC getstatic = (GETSTATIC) ins;
			field = new FieldVariable(getstatic.getClassName(cpg), getstatic.getFieldName(cpg));
		} else
			return null;

		Variable result = new LocalVariable(after.getTopValue());

		// Ensure that the field and result variables are consistent with
		// previous definitions (if any)
		bindingSet = addOrCheckDefinition(fieldVarName, field, bindingSet);
		if (bindingSet == null)
			return null;
		return addOrCheckDefinition(resultVarName, result, bindingSet);
	}
}

// vim:ts=4
