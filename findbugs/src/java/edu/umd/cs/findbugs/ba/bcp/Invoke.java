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

import java.util.regex.*;
import org.apache.bcel.Repository;
import org.apache.bcel.Constants;
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
	private boolean acceptSubclass;
	private String className;
	private Pattern methodNameRE;
	private Pattern methodSigRE;
	private boolean isStatic;

	/**
	 * Constructor.
	 * @param className the class name of the method; if it begins with a "+"
	 *  character, then any subclass of the named class is accepted
	 * @param methodName the name of the method; if it begins with a "/" character,
	 *  then the rest of the string is treated as a regular expression
	 * @param methodSig the signature of the method; if it begins with a "/" character,
	 *  then the rest of the string is treated as a regular expression
	 * @param isStatic true if the method is static, false otherwise
	 */
	public Invoke(String className, String methodName, String methodSig, boolean isStatic) {
		if (className.startsWith("+")) {
			this.acceptSubclass = true;
			className = className.substring(1);
		} else
			this.acceptSubclass = false;
		this.className = className;

		this.methodNameRE = createRE(methodName);
		this.methodSigRE = createRE(methodSig);

		this.isStatic = isStatic;
	}

	private Pattern createRE(String name) {
		String pattern;
		if (name.startsWith("/")) {
			// Regular expression match
			pattern = name.substring(1);
		} else {
			// Quote metacharacters, so we match the literal value of the string
			pattern = "\\Q" + name + "\\E";
		}
		return Pattern.compile(pattern);
	}

	public MatchResult match(InstructionHandle handle, ConstantPoolGen cpg,
		ValueNumberFrame before, ValueNumberFrame after, BindingSet bindingSet) throws DataflowAnalysisException {

		// See if the instruction is an InvokeInstruction
		Instruction ins = handle.getInstruction();
		if (!(ins instanceof InvokeInstruction))
			return null;
		InvokeInstruction inv = (InvokeInstruction) ins;

		// Check that it's static or non-static, as appropriate
		boolean isStatic = inv.getOpcode() == Constants.INVOKESTATIC;
		if (this.isStatic != isStatic)
			return null;

		// Check class name, method name, and method signature...
		String className = inv.getClassName(cpg);
		String methodName = inv.getMethodName(cpg);
		String methodSig = inv.getSignature(cpg);

		// Check class name
		if (acceptSubclass) {
			// See if the current invoked class is a subclass of the one we want
			try {
				if (!Repository.instanceOf(className, this.className))
					return null;
			} catch (ClassNotFoundException e) {
				return null;
			}
		} else {
			// Force exact class match
			if (!this.className.equals(className))
				return null;
		}

		// Check method name and signature
		if (!methodNameRE.matcher(methodName).matches())
			return null;
		if (!methodSigRE.matcher(methodSig).matches())
			return null;

		// It's a match!
		return new MatchResult(this, bindingSet);

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
