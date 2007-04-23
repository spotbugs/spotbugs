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
 * Dataflow fact to represent the depth of the Java operand stack.
 *
 * @see StackDepthAnalysis
 */
public class StackDepth {
	private int depth;

	/**
	 * Constructor.
	 *
	 * @param depth the stack depth
	 */
	public StackDepth(int depth) {
		this.depth = depth;
	}

	/**
	 * Get the stack depth.
	 */
	public int getDepth() {
		return depth;
	}

	/**
	 * Set the stack depth.
	 */
	public void setDepth(int depth) {
		this.depth = depth;
	}

	@Override
		 public String toString() {
		if (getDepth() == StackDepthAnalysis.TOP)
			return "[TOP]";
		else if (getDepth() == StackDepthAnalysis.BOTTOM)
			return "[BOTTOM]";
		else
			return "[" + String.valueOf(depth) + "]";
	}
}

// vim:ts=4
