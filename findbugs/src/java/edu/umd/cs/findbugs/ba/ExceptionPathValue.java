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

/**
 * A value representing, for a location in a method, what
 * kind of control flow lead to that location.
 * This value can represent whether the location is
 * <ul>
 * <li> on an exception path,
 * <li> on a path in which exception control flow has merged back
 *      with non-exception control flow, or
 * <li> on a completely non-exception path.
 * </ul>
 *
 * @see ExceptionPathValueAnalysis
 * @author David Hovemeyer
 */
public class ExceptionPathValue {
	public static final int TOP = 0;
	public static final int NON_EXCEPTION = 1;
	public static final int HANDLED_EXCEPTION = 2;
	public static final int UNHANDLED_EXCEPTION = 3;
	public static final int CONDITIONAL_UNHANDLED_EXCEPTION = 4;
	public static final int EXCEPTION = 5;

	private static final int NE = NON_EXCEPTION;
	private static final int HE = HANDLED_EXCEPTION;
	private static final int UE = UNHANDLED_EXCEPTION;
	private static final int CUE = CONDITIONAL_UNHANDLED_EXCEPTION;
	private static final int E = EXCEPTION;

	private static int[][] mergeMatrix = {
		// TOP   NE    HE    UE   CUE    E
		{  TOP,                              }, // TOP
		{  NE,   NE,                         }, // NE
		{  HE,   HE,   HE,                   }, // HE
		{  UE,   CUE,  E,    UE              }, // UE
		{  CUE,  CUE,  E,    CUE, CUE,       }, // CUE
		{  E,    E,    E,    E,   E,     E   }, // E
	};

	private int kind;

	public ExceptionPathValue(int kind) {
		this.kind = kind;
	}

	public int getKind() { return kind; }

	public void setKind(int kind) { this.kind = kind; }

	public boolean equals(Object o) {
		if (this.getClass() != o.getClass())
			return false;
		ExceptionPathValue other = (ExceptionPathValue) o;
		return this.kind == other.kind;
	}

	public int hashCode() { throw new UnsupportedOperationException(); }

	public void copyFrom(ExceptionPathValue other) {
		this.kind = other.kind;
	}

	public void mergeWith(ExceptionPathValue other) {
		int min = Math.min(this.kind, other.kind);
		int max = Math.max(this.kind, other.kind);
		this.kind = mergeMatrix[max][min];
	}

	public String toString() {
		switch (kind) {
		case TOP:
			return "[TOP]";
		case NON_EXCEPTION:
			return "[Non-exception]";
		case HANDLED_EXCEPTION:
			return "[Handled exception]";
		case UNHANDLED_EXCEPTION:
			return "[Unhandled exception]";
		case CONDITIONAL_UNHANDLED_EXCEPTION:
			return "[Conditional unhandled exception]";
		case EXCEPTION:
			return "[Handled or Unhandled Exception]";
		default:
			throw new IllegalStateException("Unknown kind of ExceptionPathValue: " + kind);
		}
	}
}

// vim:ts=4
