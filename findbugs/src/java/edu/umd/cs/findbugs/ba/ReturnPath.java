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

public class ReturnPath {
	/**
	 * Top value.
	 */
	public static final int TOP = 0;
	/**
	 * Method "returns" by exiting the process.
	 */
	public static final int EXIT = 1;
	/**
	 * Method returns by throwing an unhandled exception.
	 */
	public static final int UE = 2;
	/**
	 * Method returns either by exiting or throwing an unhandled exception.
	 */
	public static final int EXIT_UE = 3;
	/**
	 * Method may return normally.
	 */
	public static final int RETURNS = 4;

	private int kind;

	public ReturnPath(int kind) {
		this.kind = kind;
	}

	public int getKind() {
		return kind;
	}

	public void setKind(int kind) {
		this.kind = kind;
	}

	public void copyFrom(ReturnPath other) {
		this.kind = other.kind;
	}

	public boolean sameAs(ReturnPath other) {
		return this.kind == other.kind;
	}

	private static final int[][] mergeMatrix = {
		// TOP      EXIT      UE       EXIT_UE   RETURNS
		{TOP, }, // TOP
		{EXIT, EXIT, }, // EXIT
		{UE, EXIT_UE, UE, }, // UE
		{EXIT_UE, EXIT_UE, EXIT_UE, EXIT_UE, }, // EXIT_UE
		{RETURNS, RETURNS, RETURNS, RETURNS, RETURNS}, // RETURNS
	};

	public void mergeWith(ReturnPath other) {
		int max = Math.max(this.kind, other.kind);
		int min = Math.min(this.kind, other.kind);
		this.kind = mergeMatrix[max][min];
	}

	@Override
         public String toString() {
		switch (kind) {
		case TOP:
			return "[TOP]";
		case EXIT:
			return "[EXIT]";
		case UE:
			return "[UE]";
		case EXIT_UE:
			return "[EXIT_UE]";
		case RETURNS:
			return "[RETURNS]";
		default:
			throw new IllegalStateException();
		}
	}
}

// vim:ts=4
