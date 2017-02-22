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

/**
 * Constants defining the type of control flow edges, as well as flags defining
 * additional information about the edges.
 *
 * @see Edge
 */
public interface EdgeTypes {
    /*
     * ----------------------------------------------------------------------
     * Edge types
     * ----------------------------------------------------------------------
     */

    /**
     * Unknown edge type.
     */
    @Edge.Type
    public static final int UNKNOWN_EDGE = -1;

    /**
     * Edge type for fall-through to next instruction.
     */
    @Edge.Type
    public static final int FALL_THROUGH_EDGE = 0;

    /**
     * Edge type for IFCMP instructions when condition is true.
     */
    @Edge.Type
    public static final int IFCMP_EDGE = 1;

    /**
     * Edge type for switch instructions (explicit case).
     */
    @Edge.Type
    public static final int SWITCH_EDGE = 2;

    /**
     * Edge type for switch instructions (default case).
     */
    @Edge.Type
    public static final int SWITCH_DEFAULT_EDGE = 3;

    /**
     * Edge type for JSR instructions.
     */
    @Edge.Type
    public static final int JSR_EDGE = 4;

    /**
     * Edge type for RET instructions.
     */
    @Edge.Type
    public static final int RET_EDGE = 5;

    /**
     * Edge type for GOTO instructions.
     */
    @Edge.Type
    public static final int GOTO_EDGE = 6;

    /**
     * Edge type for RETURN instructions. (These must go to the exit node of the
     * CFG).
     */
    @Edge.Type
    public static final int RETURN_EDGE = 7;

    /**
     * Edge representing the possibility that an exception might propagate out
     * of the current method. Such edges always go to the exit node in the CFG.
     */
    @Edge.Type
    public static final int UNHANDLED_EXCEPTION_EDGE = 8;

    /**
     * Edge representing control flow from an exception-raising basic block to
     * an explicit handler for the exception.
     */
    @Edge.Type
    public static final int HANDLED_EXCEPTION_EDGE = 9;

    /**
     * Edge from entry node to real start node.
     */
    @Edge.Type
    public static final int START_EDGE = 10;

    /**
     * Special (synthetic) edge for path profiling; CFG entry to backedge
     * target.
     */
    @Edge.Type
    public static final int BACKEDGE_TARGET_EDGE = 11;

    /**
     * Special (synthetic) edge for path profiling; backedge source to CFG exit.
     */
    @Edge.Type
    public static final int BACKEDGE_SOURCE_EDGE = 12;

    /**
     * System.exit() edge.
     */
    @Edge.Type
    public static final int EXIT_EDGE = 13;

    /*
     * ----------------------------------------------------------------------
     * Edge flags
     * ----------------------------------------------------------------------
     */

    /**
     * Checked exceptions can be thrown on edge.
     */
    public static final int CHECKED_EXCEPTIONS_FLAG = 1;

    /**
     * Explicit exceptions can be thrown on the edge.
     */
    public static final int EXPLICIT_EXCEPTIONS_FLAG = 2;
}

