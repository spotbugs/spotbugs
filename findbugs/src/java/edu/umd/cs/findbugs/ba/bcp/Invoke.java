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

package edu.umd.cs.daveho.ba.bcp;

import org.apache.bcel.generic.*;
import edu.umd.cs.daveho.ba.*;

/**
 * A PatternElement to match a method invocation.
 * Currently, we don't allow variables in this element (for arguments
 * and return value).  This would be a good thing to add.
 * We also don't distinguish between invokevirtual, invokeinterface,
 * and invokespecial.
 *
 * @see PatternElement
 * @author David Hovemeyer
 */
public class Invoke extends PatternElement {
	private String className;
	private String methodName;
	private String methodSig;
	private boolean isStatic;

	/**
	 * Constructor.
	 * @param className the class name of the method
	 * @param methodName the name of the method
	 * @param methodSig the signature of the method
	 * @param isStatic true if the method is static, false otherwise
	 */
	public Invoke(String className, String methodName, String methodSig, boolean isStatic) {
		this.className = className;
		this.methodName = methodName;
		this.methodSig = methodSig;
		this.isStatic = isStatic;
	}

	public MatchResult match(InstructionHandle handle, ConstantPoolGen cpg,
		ValueNumberFrame before, ValueNumberFrame after, BindingSet bindingSet) throws DataflowAnalysisException {
		Instruction ins = handle.getInstruction();
		if (!(ins instanceof InvokeInstruction))
			return null;
		InvokeInstruction inv = (InvokeInstruction) ins;

		String className = inv.getClassName(cpg);
		String methodName = inv.getMethodName(cpg);
		String methodSig = inv.getSignature(cpg);
		boolean isStatic = inv instanceof INVOKESTATIC;

		if (this.className.equals(className) &&
			this.methodName.equals(methodName) &&
			this.methodSig.equals(methodSig) &&
			this.isStatic == isStatic)
			return new MatchResult(this, bindingSet);
		else
			return null;	
	}

	public boolean acceptBranch(Edge edge, InstructionHandle source) {
		return true;
	}

	public int minOccur() {
		return 1;
	}

	public int maxOccur() {
		return 1;
	}
}

// vim:ts=4
