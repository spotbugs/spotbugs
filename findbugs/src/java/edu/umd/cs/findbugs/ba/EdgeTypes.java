package edu.umd.cs.daveho.ba;

/**
 * Types of control-flow edges
 * @see Edge
 */
public interface EdgeTypes {
    /** Edge type for fall-through to next instruction. */
    public static final int FALL_THROUGH_EDGE = 0;
    /** Edge type for IFCMP instructions when condition is true. */
    public static final int IFCMP_EDGE = 1;
    /** Edge type for switch instructions (explicit case). */
    public static final int SWITCH_EDGE = 2;
    /** Edge type for switch instructions (default case). */
    public static final int SWITCH_DEFAULT_EDGE = 3;
    /** Edge type for JSR instructions. */
    public static final int JSR_EDGE = 4;
    /** Edge type for RET instructions. */
    public static final int RET_EDGE = 5;
    /** Edge type for GOTO instructions. */
    public static final int GOTO_EDGE = 6;
    /** Edge type for RETURN instructions.  (These must go to the exit node of the CFG). */
    public static final int RETURN_EDGE = 7;
    /** Edge representing the possibility that an exception might propagate
        out of the current method.  Such edges always go to the exit node 
        in the CFG. */
    public static final int UNHANDLED_EXCEPTION_EDGE = 8;
    /** Edge representing control flow from an exception-raising basic block
        to an explicit handler for the exception. */
    public static final int HANDLED_EXCEPTION_EDGE = 9;
    /** Edge from entry node to real start node. */
    public static final int START_EDGE = 10;
    /** Special (synthetic) edge for path profiling; CFG entry to backedge target. */
    public static final int BACKEDGE_TARGET_EDGE = 11;
    /** Special (synthetic) edge for path profiling; backedge source to CFG exit. */
    public static final int BACKEDGE_SOURCE_EDGE = 12;
}
