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

public class IsNullValue {
	private static final int DEFINITELY_NULL = 2;
	private static final int DEFINITELY_NULL_ON_SOME_PATH = 1;
	private static final int NOT_DEFINITELY_NULL = 0;

	private int kind;

	private IsNullValue(int kind) {
		this.kind = kind;
	}

	private static final IsNullValue[] instanceList = {
		new IsNullValue(DEFINITELY_NULL),
		new IsNullValue(DEFINITELY_NULL_ON_SOME_PATH),
		new IsNullValue(NOT_DEFINITELY_NULL)
	};

	public static IsNullValue definitelyNull() {
		return instanceList[DEFINITELY_NULL];
	}

	public static IsNullValue definitelyNullOnSomePath() {
		return instanceList[DEFINITELY_NULL_ON_SOME_PATH];
	}

	public static IsNullValue notDefinitelyNull() {
		return instanceList[NOT_DEFINITELY_NULL];
	}

	public static IsNullValue merge(IsNullValue a, IsNullValue b) {
		return instanceList[Math.min(a.kind, b.kind)];
	}
}

// vim:ts=4
