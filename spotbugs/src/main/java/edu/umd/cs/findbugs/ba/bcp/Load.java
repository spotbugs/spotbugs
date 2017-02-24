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
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.vna.ValueNumber;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

/**
 * A PatternElement representing a load from a field. Variables represent the
 * field and the result of the load.
 *
 * @author David Hovemeyer
 * @see PatternElement
 */
public class Load extends FieldAccess {

    /**
     * Constructor.
     *
     * @param fieldVarName
     *            the name of the field variable
     * @param resultVarName
     *            the name of the result variable
     */
    public Load(String fieldVarName, String resultVarName) {
        super(fieldVarName, resultVarName);
    }

    @Override
    public MatchResult match(InstructionHandle handle, ConstantPoolGen cpg, ValueNumberFrame before, ValueNumberFrame after,
            BindingSet bindingSet) throws DataflowAnalysisException {

        Variable field;
        Instruction ins = handle.getInstruction();
        FieldInstruction fieldIns;

        // The instruction must be GETFIELD or GETSTATIC
        if (ins instanceof GETFIELD) {
            fieldIns = (GETFIELD) ins;
            ValueNumber ref = before.getTopValue();
            field = new FieldVariable(ref, fieldIns.getClassName(cpg), fieldIns.getFieldName(cpg), fieldIns.getSignature(cpg));
        } else if (ins instanceof GETSTATIC) {
            fieldIns = (GETSTATIC) ins;
            field = new FieldVariable(fieldIns.getClassName(cpg), fieldIns.getFieldName(cpg), fieldIns.getSignature(cpg));
        } else {
            return null;
        }

        Variable result = snarfFieldValue(fieldIns, cpg, after);

        return checkConsistent(field, result, bindingSet);
    }
}

