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

package edu.umd.cs.daveho.ba;

import java.util.*;

/**
 * An edge of the control flow graph.
 */
public class Edge implements EdgeTypes, Comparable {

    private BasicBlock source, dest;
    private int type;
    private int id;
    private int dupCount;
    private int sourceBytecode, destBytecode;
    private int sourceLine, destLine;

    /**
     * Constructor.
     * @param source source basic block
     * @param dest destination basic block
     * @param type type of edge
     * @param dupCount the count used to distinguish this Edge from other Edges
     *   with the same source and destination blocks; normally zero
     */
    public Edge(BasicBlock source, BasicBlock dest, int type, int dupCount) {
	if (source == null) throw new IllegalArgumentException();
	if (dest == null) throw new IllegalArgumentException();
	this.source = source;
	this.dest = dest;
	this.type = type;
	this.id = -1;
	this.dupCount = dupCount;
	this.sourceBytecode = -1;
	this.destBytecode = -1;
	this.sourceLine = -1;
	this.destLine = -1;
    }

    /**
     * Constructor.
     * @param source source basic block
     * @param dest destination basic block
     * @param type type of edge
     */
    public Edge(BasicBlock source, BasicBlock dest, int type) {
	this(source, dest, type, 0);
    }

    /**
     * Get the type of edge.
     */
    public int getType() {
	return type;
    }

    /**
     * Is the edge an exception edge?
     */
    public boolean isExceptionEdge() {
	return type == HANDLED_EXCEPTION_EDGE || type == UNHANDLED_EXCEPTION_EDGE;
    }

    /** Set the edge's unique id. */
    public void setId(int id) {
	this.id = id;
    }

    /** Get the edge's unique id. */
    public int getId() {
	return id;
    }

    /** Get the edge's dupCount. */
    public int getDupCount() {
	return dupCount;
    }

    /**
     * Set source and destination information.
     * @param sourceBytecode the bytecode offset of the edge source
     * @param destBytecode the bytecode offset of the edge destination
     * @param sourceLine the source line number of the edge source
     * @param destLine the source line number of the edge destination
     */
    public void setSourceAndDest(int sourceBytecode, int destBytecode, int sourceLine, int destLine) {
	this.sourceBytecode = sourceBytecode;
	this.destBytecode = destBytecode;
	this.sourceLine = sourceLine;
	this.destLine = destLine;
    }

    /** Get bytecode offset of edge source. */
    public int getSourceBytecode() { return sourceBytecode; }

    /** Get bytecode offset of edge destination. */
    public int getDestinationBytecode() { return destBytecode; }

    /** Get source line of edge source. */
    public int getSourceLine() { return sourceLine;}

    /** Get source line of edge destination. */
    public int getDestinationLine() { return destLine; }

    /** Get the source of the edge. */
    public BasicBlock getSource() {
	return source;
    }

    /** Get the destination of the edge. */
    public BasicBlock getDest() {
	return dest;
    }

    /** Compare with other edge. */
    public int compareTo(Object o) {
	// Lexicographically compare (source,dest) pair.
	// Use dupCount as tie-breaker.
	Edge other = (Edge) o;
	int cmp;
	cmp = source.compareTo(other.source);
	if (cmp != 0)
	    return cmp;
	cmp = dest.compareTo(other.dest);
	if (cmp != 0)
	    return cmp;
	return dupCount - other.dupCount;
    }

    /** Return a string representation of the edge. */
    public String toString() {
	return "EDGE(" + id + ") type " + edgeTypeToString(type) +" from block " + source.getId()+ " to block " + dest.getId();
    }

    /** Get string representing given edge type. */
    public static String edgeTypeToString(int edgeType) {
	switch (edgeType) {
	case FALL_THROUGH_EDGE:
	    return "FALL_THROUGH";
	case IFCMP_EDGE:
	    return "IFCMP";
	case SWITCH_EDGE:
	    return "SWITCH";
	case SWITCH_DEFAULT_EDGE:
	    return "SWITCH_DEFAULT";
	case JSR_EDGE:
	    return "JSR";
	case RET_EDGE:
	    return "RET";
	case GOTO_EDGE:
	    return "GOTO";
	case RETURN_EDGE:
	    return "RETURN";
	case UNHANDLED_EXCEPTION_EDGE:
	    return "UNHANDLED_EXCEPTION";
	case HANDLED_EXCEPTION_EDGE:
	    return "HANDLED_EXCEPTION";
	case START_EDGE:
	    return "START";
	case BACKEDGE_TARGET_EDGE:
	    return "BACKEDGE_TARGET_EDGE";
	case BACKEDGE_SOURCE_EDGE:
	    return "BACKEDGE_SOURCE_EDGE";
	}
	throw new IllegalStateException();
    }

    /** Get numeric edge type from string representation. */
    public static int stringToEdgeType(String s) {
	s = s.toUpperCase();

	if (s.equals("FALL_THROUGH"))
	    return FALL_THROUGH_EDGE;
	else if (s.equals("IFCMP"))
	    return IFCMP_EDGE;
	else if (s.equals("SWITCH"))
	    return SWITCH_EDGE;
	else if (s.equals("SWITCH_DEFAULT"))
	    return SWITCH_DEFAULT_EDGE;
	else if (s.equals("JSR"))
	    return JSR_EDGE;
	else if (s.equals("RET"))
	    return RET_EDGE;
	else if (s.equals("GOTO"))
	    return GOTO_EDGE;
	else if (s.equals("RETURN"))
	    return RETURN_EDGE;
	else if (s.equals("UNHANDLED_EXCEPTION"))
	    return UNHANDLED_EXCEPTION_EDGE;
	else if (s.equals("HANDLED_EXCEPTION"))
	    return HANDLED_EXCEPTION_EDGE;
	else if (s.equals("START"))
	    return START_EDGE;
	else if (s.equals("BACKEDGE_TARGET_EDGE"))
	    return BACKEDGE_TARGET_EDGE;
	else if (s.equals("BACKEDGE_SOURCE_EDGE"))
	    return BACKEDGE_SOURCE_EDGE;
	else
	    throw new IllegalArgumentException("Unknown edge type: " + s);
    }
}
