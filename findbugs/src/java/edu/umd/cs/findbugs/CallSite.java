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
		return System.identityHashCode(method) + basicBlock.getId() + System.identityHashCode(handle);
	}

	public boolean equals(Object o) {
		if (!(o instanceof CallSite))
			return false;
		CallSite other = (CallSite) o;
		return method == other.method && basicBlock == other.basicBlock && handle == other.handle;
	}
}

// vim:ts=4
