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

public class ResourceValueFrame extends Frame<ResourceValue> {
	/** The resource doesn't exist. */
	public static final int NONEXISTENT = 0;

	/** The resource is open (or locked, etc). */
	public static final int OPEN = 1;

	/** The resource is closed (or unlocked, etc). */
	public static final int CLOSED = 2;


	private int status;

	public ResourceValueFrame(int numSlots) {
		super(numSlots);
	}

	public ResourceValue mergeValues(int slot, ResourceValue a, ResourceValue b) throws DataflowAnalysisException {
		return ResourceValue.merge(a, b);
	}

	public ResourceValue getDefaultValue() {
		return ResourceValue.notInstance();
	}

}

// vim:ts=4
