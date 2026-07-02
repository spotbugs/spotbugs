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

import org.apache.bcel.generic.ACONST_NULL;
import org.apache.bcel.generic.ALOAD;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.GETFIELD;
import org.apache.bcel.generic.GETSTATIC;
import org.apache.bcel.generic.IFNONNULL;
import org.apache.bcel.generic.IFNULL;
import org.apache.bcel.generic.IF_ACMPEQ;
import org.apache.bcel.generic.IF_ACMPNE;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.ba.DataflowAnalysisException;
import edu.umd.cs.findbugs.ba.Edge;
import edu.umd.cs.findbugs.ba.EdgeTypes;
import edu.umd.cs.findbugs.ba.vna.ValueNumberFrame;

public class IfNull extends OneVariableInstruction implements EdgeTypes {

    public IfNull(String varName) {
        super(varName);
    }

    @Override
    public MatchResult match(InstructionHandle handle, ConstantPoolGen cpg, ValueNumberFrame before, ValueNumberFrame after,
            BindingSet bindingSet) throws DataflowAnalysisException {

        // Instruction must be IFNULL/IFNONNULL, or a reference comparison
        // (IF_ACMPEQ/IF_ACMPNE) against the null constant. The latter is how
        // javac compiles a Yoda-style null check such as "null == field".
        Instruction ins = handle.getInstruction();
        if (!(ins instanceof IFNULL || ins instanceof IFNONNULL || isReferenceComparisonWithNull(handle))) {
            return null;
        }

        // Ensure reference used is consistent with previous uses of
        // same variable.
        LocalVariable ref = new LocalVariable(before.getTopValue());
        return addOrCheckDefinition(ref, bindingSet);
    }

    private static boolean isReferenceComparisonWithNull(InstructionHandle handle) {
        Instruction ins = handle.getInstruction();
        if (!(ins instanceof IF_ACMPEQ || ins instanceof IF_ACMPNE)) {
            return false;
        }
        // Yoda "null == ref": skip back over the reference load to the ACONST_NULL.
        for (InstructionHandle prev = handle.getPrev(); prev != null; prev = prev.getPrev()) {
            Instruction prevIns = prev.getInstruction();
            if (prevIns instanceof ACONST_NULL) {
                return true;
            }
            if (!isReferenceLoad(prevIns)) {
                return false;
            }
        }
        return false;
    }

    private static boolean isReferenceLoad(Instruction ins) {
        return ins instanceof ALOAD || ins instanceof GETFIELD || ins instanceof GETSTATIC;
    }

    @Override
    public boolean acceptBranch(Edge edge, InstructionHandle source) {
        Instruction ins = source.getInstruction();
        boolean branchOnNull = (ins instanceof IFNULL) || (ins instanceof IF_ACMPEQ);
        return edge.getType() == (branchOnNull ? IFCMP_EDGE : FALL_THROUGH_EDGE);
    }
}
