/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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
package edu.umd.cs.findbugs.detect;

import java.util.BitSet;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.ba.ClassContext;

/**
 * Look for calls to methods where the return value is
 * erroneously ignored.  This detector is meant as a simpler
 * and faster replacement for BCPMethodReturnCheck.
 * 
 * @author David Hovemeyer
 */
public class MethodReturnCheck extends BytecodeScanningDetector {
	private static boolean DEBUG = Boolean.getBoolean("mrc.debug");
	
	private static final int SCAN =0;
	private static final int SAW_INVOKE = 1;
	
	private static final BitSet INVOKE_OPCODE_SET = new BitSet();
	static {
		INVOKE_OPCODE_SET.set(Constants.INVOKEINTERFACE);
		INVOKE_OPCODE_SET.set(Constants.INVOKESPECIAL);
		INVOKE_OPCODE_SET.set(Constants.INVOKESTATIC);
		INVOKE_OPCODE_SET.set(Constants.INVOKEVIRTUAL);
	}

	private BugReporter bugReporter;
	private ClassContext classContext;
	private Method method;
	private int state;
	private int callPC;
	private String className, methodName, signature;
	
	public MethodReturnCheck(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	
	public void visitClassContext(ClassContext classContext) {
		this.classContext = classContext;
		super.visitClassContext(classContext);
		this.classContext = null;
	}
	
	public void visit(Method method) {
		this.method = method;
	}
	
	public void visitCode(Code code) {
		// Prescreen to find methods with POP or POP2 instructions,
		// and at least one method invocation
		if (!prescreen())
			return;
		
		if (DEBUG) System.out.println("Visiting " + method);
		super.visitCode(code);
	}

	private boolean prescreen() {
		BitSet bytecodeSet = classContext.getBytecodeSet(method);
		if (!(bytecodeSet.get(Constants.POP) || bytecodeSet.get(Constants.POP2))) {
			return false;
		} else if (!(bytecodeSet.get(Constants.INVOKEINTERFACE) ||
				bytecodeSet.get(Constants.INVOKESPECIAL) ||
				bytecodeSet.get(Constants.INVOKESTATIC) ||
				bytecodeSet.get(Constants.INVOKEVIRTUAL))) {
			return false;
		}
		return true;
	}
	
	public void sawOpcode(int seen) {
		boolean redo;
		
		do {
			redo = false;
			switch (state) {
			case SCAN:
				if (INVOKE_OPCODE_SET.get(seen)) {
					callPC = getPC();
					className = getDottedClassConstantOperand();
					methodName = getNameConstantOperand();
					signature = getSigConstantOperand();
					if (requiresReturnValueCheck()) {
						if (DEBUG) System.out.println(
								"Saw "+className+"."+methodName+":"+signature+" @"+callPC);
						state = SAW_INVOKE;
					}
				}
				break;
				
			case SAW_INVOKE:
				if (isPop(seen)) {
					int popPC = getPC();
					if (DEBUG) System.out.println("Saw POP @"+popPC);
					BugInstance warning =
						new BugInstance(this, "RV_RETURN_VALUE_IGNORED", NORMAL_PRIORITY)
							.addClassAndMethod(this)
							.addMethod(className, methodName, signature).describe("METHOD_CALLED")
							.addSourceLine(this, callPC);
					bugReporter.reportBug(warning);
				} else {
					// This instruction might be an invocation, too.
					// So redo processing this instruction.
					redo = true;
				}
				state = SCAN;
				break;
				
			default:
			}
		} while (redo);

	}

	private boolean isPop(int seen) {
		return seen == Constants.POP || seen == Constants.POP2;
	}

	private boolean requiresReturnValueCheck() {
		if (DEBUG) {
			System.out.println("Trying: "+className+"."+methodName+":"+signature);
		}
		
		// FIXME: just look for String methods for now
		return className.equals("java.lang.String")
			&& signature.endsWith(")Ljava/lang/String;");
	}
}
