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

package edu.umd.cs.findbugs;

import java.util.*;

// We require BCEL 5.1 or later.
import org.apache.bcel.*;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;

import edu.umd.cs.daveho.ba.*;

public class CallSite {
	private Method method;
	private BasicBlock basicBlock;
	private InstructionHandle handle;

	public CallSite(Method method, BasicBlock basicBlock, InstructionHandle handle) {
		this.method = method;
		this.basicBlock = basicBlock;
		this.handle = handle;
	}

	public Method getMethod() { return method; }

	public BasicBlock getBasicBlock() { return basicBlock; }

	public InstructionHandle getHandle() { return handle; }

	public int hashCode() {
		return System.identityHashCode(method) ^ basicBlock.getId() ^ System.identityHashCode(handle);
	}

	public boolean equals(Object o) {
		if (!(o instanceof CallSite))
			return false;
		CallSite other = (CallSite) o;
		return method == other.method && basicBlock == other.basicBlock && handle == other.handle;
	}
}

// vim:ts=4
