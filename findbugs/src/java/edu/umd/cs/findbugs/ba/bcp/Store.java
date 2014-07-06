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
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.PUTSTATIC;

import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

/**
 * A PatternElement representing a store to a field. Variables represent the
 * field and the value stored.
 *
 * @author David Hovemeyer
 * @see PatternElement
 */
public class Store extends FieldAccess {
    /**
     * Constructor.
     *
     * @param fieldVarName
     *            the name of the field variable
     * @param valueVarName
     *            the name of the variable representing the value stored
     */
    public Store(String fieldVarName, String valueVarName) {
        super(fieldVarName, valueVarName);
    }

    @Override
    public MatchResult match(InstructionHandle handle, ConstantPoolGen cpg, ValueNumberFrame before, ValueNumberFrame after,
            BindingSet bindingSet) throws DataflowAnalysisException {

        Instruction ins = handle.getInstruction();
        FieldInstruction fieldIns;
        Variable field;

        // The instruction must be PUTFIELD or PUTSTATIC
        if (ins instanceof PUTFIELD) {
            fieldIns = (PUTFIELD) ins;
            int numSlots = before.getNumSlots();
            ValueNumber ref = before.getValue(isLongOrDouble(fieldIns, cpg) ? numSlots - 3 : numSlots - 2);
            field = new FieldVariable(ref, fieldIns.getClassName(cpg), fieldIns.getFieldName(cpg), fieldIns.getSignature(cpg));
        } else if (ins instanceof PUTSTATIC) {
            fieldIns = (PUTSTATIC) ins;
            field = new FieldVariable(fieldIns.getClassName(cpg), fieldIns.getFieldName(cpg), fieldIns.getSignature(cpg));
        } else {
            return null;
        }

        Variable value = snarfFieldValue(fieldIns, cpg, before);

        return checkConsistent(field, value, bindingSet);
    }
}

