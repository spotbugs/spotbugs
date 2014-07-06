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

package edu.umd.cs.findbugs.ba;

import org.apache.bcel.generic.InstructionHandle;

/**
 * The target of a branch instruction.
 */
public class Target {
    private final InstructionHandle targetInstruction;

    private @Edge.Type
    final int edgeType;

    /**
     * Constructor.
     *
     * @param targetInstruction
     *            the handle of the target instruction
     * @param edgeType
     *            type of CFG edge; see EdgeTypes interface
     */
    public Target(InstructionHandle targetInstruction, @Edge.Type int edgeType) {
        this.targetInstruction = targetInstruction;
        this.edgeType = edgeType;
    }

    /**
     * Get the handle of the target instruction.
     */
    public InstructionHandle getTargetInstruction() {
        return targetInstruction;
    }

    /**
     * Get the control flow edge type.
     */
    public @Edge.Type int getEdgeType() {
        return edgeType;
    }
}
