package edu.umd.cs.daveho.ba;

// We require BCEL 5.0 or later.
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

/**
 * The target of a branch instruction.
 */
public class Target {
    private InstructionHandle targetInstruction;
    private int edgeType;

    /**
     * Constructor.
     * @param targetInstruction the handle of the target instruction
     * @param edgeType type of CFG edge; see EdgeTypes interface
     */
    public Target(InstructionHandle targetInstruction, int edgeType) {
	this.targetInstruction = targetInstruction;
	this.edgeType = edgeType;
    }

    /** Get the handle of the target instruction. */
    public InstructionHandle getTargetInstruction() {
	return targetInstruction;
    }

    /** Get the control flow edge type. */
    public int getEdgeType() {
	return edgeType;
    }
}
