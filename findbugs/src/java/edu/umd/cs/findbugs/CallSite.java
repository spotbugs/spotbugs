/*
 * FindBugs - Find bugs in Java programs
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

package edu.umd.cs.findbugs;

import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.ba.BasicBlock;
import edu.umd.cs.findbugs.ba.Location;

/**
 * The site of a method call.
 */
public class CallSite {
	private final Method method;
	private final Location location;

	/**
	 * Constructor.
	 *
	 * @param method     the method containing the call site
	 * @param basicBlock the basic block where the call site is located
	 * @param handle     the instruction which performs the call
	 */
	public CallSite(Method method, BasicBlock basicBlock, InstructionHandle handle) {
		this.method = method;
		this.location = new Location(handle, basicBlock);
	}

	/**
	 * Get the method containing the call site.
	 */
	public Method getMethod() {
		return method;
	}

	/**
	 * Get the Location (basic block and instruction) where the call site is located.
	 */
	public Location getLocation() {
		return location;
	}

	/**
	 * Get the basic block where the call site is located.
	 */
	public BasicBlock getBasicBlock() {
		return location.getBasicBlock();
	}

	/**
	 * Get the instruction which performs the call.
	 */
	public InstructionHandle getHandle() {
		return location.getHandle();
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(method)
				^ getBasicBlock().getId()
				^ System.identityHashCode(location.getHandle());
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof CallSite))
			return false;
		CallSite other = (CallSite) o;
		return method == other.method
				&& getBasicBlock() == other.getBasicBlock()
				&& getHandle() == other.getHandle();
	}
}

// vim:ts=4
