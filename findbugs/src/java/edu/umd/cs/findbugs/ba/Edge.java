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

package edu.umd.cs.findbugs.ba;

import edu.umd.cs.findbugs.graph.AbstractEdge;
import java.util.*;
import org.apache.bcel.generic.InstructionHandle;

/**
 * An edge of a control flow graph.
 * @see BasicBlock
 * @see CFG
 * @author David Hovemeyer
 */
public class Edge extends AbstractEdge<Edge, BasicBlock> implements EdgeTypes, Debug {

	/* ----------------------------------------------------------------------
	 * Fields
	 * ---------------------------------------------------------------------- */

	private int type;

	/* ----------------------------------------------------------------------
	 * Public methods
	 * ---------------------------------------------------------------------- */

	/**
	 * Constructor.
	 * @param source source basic block
	 * @param dest destination basic block
	 */
	public Edge(BasicBlock source, BasicBlock dest) {
		super(source, dest);
	}

	public int getId() {
		return getLabel();
	}

	/**
	 * Get the type of edge.
	 */
	public int getType() {
		return type;
	}

	/**
	 * Set the type of edge.
	 */
	public void setType(int type) {
		this.type = type;
	}

	/**
	 * Is the edge an exception edge?
	 */
	public boolean isExceptionEdge() {
		return type == HANDLED_EXCEPTION_EDGE || type == UNHANDLED_EXCEPTION_EDGE;
	}

	public boolean equals(Object o) {
		if (this.getClass() != o.getClass())
			return false;
		Edge other = (Edge) o;
		return this.getSource() == other.getSource() && this.getTarget() == other.getTarget()
			&& this.getType() == other.getType();
	}

	public int hashCode() {
		return 2003 * getSource().getLabel() + getTarget().getLabel();
	}

	/** Compare with other edge. */
	public int compareTo(Edge other) {
		int cmp = super.compareTo(other);
		if (cmp != 0)
			return cmp;
		return type - other.type;
	}

	/** Return a string representation of the edge. */
	public String toString() {
		BasicBlock source = getSource();
		BasicBlock target = getTarget();

		StringBuffer buf = new StringBuffer();
		buf.append("EDGE(");
		buf.append(getLabel());
		buf.append(") type ");
		buf.append(edgeTypeToString(type));
		buf.append(" from block ");
		buf.append(source.getId());
		buf.append(" to block ");
		buf.append(target.getId());
		InstructionHandle sourceInstruction = source.getLastInstruction();
		InstructionHandle targetInstruction = target.getFirstInstruction();
		String exInfo = " -> ";
		if (targetInstruction == null && target.isExceptionThrower()) {
			targetInstruction = target.getExceptionThrower();
			exInfo = " => ";
		}
		if (sourceInstruction != null && targetInstruction != null) {
			buf.append(" [bytecode ");
			buf.append(sourceInstruction.getPosition());
			buf.append(exInfo);
			buf.append(targetInstruction.getPosition());
			buf.append(']');
		} else if (source.isExceptionThrower()) {
			if (type == FALL_THROUGH_EDGE)
				buf.append(" [successful check]");
			else {
				buf.append(" [failed check for ");
				buf.append(source.getExceptionThrower().getPosition());
				if (targetInstruction != null) {
					buf.append(" to ");
					buf.append(targetInstruction.getPosition());
				}
				buf.append(']');
			}
		}
		return buf.toString();
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

// vim:ts=4
