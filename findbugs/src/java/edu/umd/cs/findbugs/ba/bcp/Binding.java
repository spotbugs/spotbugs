/*
 * FindBugs - Find bugs in Java programs
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

package edu.umd.cs.findbugs.bcp;

/**
 * A Binding represents a variable and its value.
 *
 * @see Value
 */
public class Binding {
	private final String varName;
	private final Value value;

	/**
	 * Constructor.
	 * @param varName the name of the variable
	 * @param value the value of the variable
	 */
	public Binding(String varName, Value value) {
		this.varName = varName;
		this.value = value;
	}

	/** Get the variable name. */
	public String getVarName() { return varName; }

	/** Get the value of the variable. */
	public Value getValue() { return value; }
}

// vim:ts=4
