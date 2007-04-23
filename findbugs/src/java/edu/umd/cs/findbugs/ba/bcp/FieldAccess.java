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
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.Type;

import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

/**
 * Base class for Load and Store PatternElements.
 * Handles some of the grunt work of representing fields and
 * extracting field values from the stack frame.
 *
 * @author David Hovemeyer
 * @see Load
 * @see Store
 */
public abstract class FieldAccess extends SingleInstruction implements org.apache.bcel.Constants {
	private String fieldVarName;
	private String valueVarName;

	/**
	 * Constructor.
	 *
	 * @param fieldVarName name of the variable to bind to the field
	 * @param valueVarName name of the variable to bind to the value store in or loaded from the field
	 */
	public FieldAccess(String fieldVarName, String valueVarName) {
		this.fieldVarName = fieldVarName;
		this.valueVarName = valueVarName;
	}

	/**
	 * Check that the Variables determined for the field and the value loaded/stored
	 * are consistent with previous variable definitions.
	 *
	 * @param field      Variable representing the field
	 * @param value      Variable representing the value loaded/stored
	 * @param bindingSet previous definitions
	 * @return a MatchResult containing an updated BindingSet if successful,
	 *         or null if unsucessful
	 */
	protected MatchResult checkConsistent(Variable field, Variable value, BindingSet bindingSet) {
		// Ensure that the field and value variables are consistent with
		// previous definitions (if any)
		bindingSet = addOrCheckDefinition(fieldVarName, field, bindingSet);
		if (bindingSet == null)
			return null;
		bindingSet = addOrCheckDefinition(valueVarName, value, bindingSet);
		if (bindingSet == null)
			return null;
		return new MatchResult(this, bindingSet);
	}

	/**
	 * Return whether the given FieldInstruction accesses a long or double field.
	 *
	 * @param fieldIns the FieldInstruction
	 * @param cpg      the ConstantPoolGen for the method
	 */
	protected static boolean isLongOrDouble(FieldInstruction fieldIns, ConstantPoolGen cpg) {
		Type type = fieldIns.getFieldType(cpg);
		int code = type.getType();
		return code == T_LONG || code == T_DOUBLE;
	}

	/**
	 * Get a Variable representing the stack value which will either be stored
	 * into or loaded from a field.
	 *
	 * @param fieldIns the FieldInstruction accessing the field
	 * @param cpg      the ConstantPoolGen for the method
	 * @param frame    the ValueNumberFrame containing the value to be stored
	 *                 or the value loaded
	 */
	protected static Variable snarfFieldValue(FieldInstruction fieldIns, ConstantPoolGen cpg, ValueNumberFrame frame)
			throws DataflowAnalysisException {

		if (isLongOrDouble(fieldIns, cpg)) {
			int numSlots = frame.getNumSlots();
			ValueNumber topValue = frame.getValue(numSlots - 1);
			ValueNumber nextValue = frame.getValue(numSlots - 2);
			return new LongOrDoubleLocalVariable(topValue, nextValue);
		} else {
			return new LocalVariable(frame.getTopValue());
		}

	}
}

// vim:ts=4
