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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
	private static boolean CHECK_CONTROL_FLOW =
		Boolean.getBoolean("mrc.checkControlFlow");
	private static boolean COUNT_POP_BRANCH_TARGETS =
		Boolean.getBoolean("mrc.countPopBranchTargets");
	
	private static final int SCAN =0;
	private static final int SAW_INVOKE = 1;
	
	private static final BitSet INVOKE_OPCODE_SET = new BitSet();
	static {
		INVOKE_OPCODE_SET.set(Constants.INVOKEINTERFACE);
		INVOKE_OPCODE_SET.set(Constants.INVOKESPECIAL);
		INVOKE_OPCODE_SET.set(Constants.INVOKESTATIC);
		INVOKE_OPCODE_SET.set(Constants.INVOKEVIRTUAL);
	}
	
	private static class QueuedWarning {
		BugInstance warning;
		int popPC;
		QueuedWarning(BugInstance warning, int popPC) {
			this.warning = warning;
			this.popPC = popPC;
		}
	}

	private BugReporter bugReporter;
	private BitSet branchTargetSet;
	private List<QueuedWarning> queuedWarningList;
	private List<Integer> popList;
	private List<Integer> retValPopList;
	private ClassContext classContext;
	private Method method;
	private int state;
	private int callPC;
	private String className, methodName, signature;
	private int popIsBranchTarget;
	private int retValPopIsBranchTarget;
	private boolean lastIsInvoke;
	
	public MethodReturnCheck(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		this.branchTargetSet = new BitSet();
		this.queuedWarningList = new LinkedList<QueuedWarning>();
		if (COUNT_POP_BRANCH_TARGETS) {
			this.popList = new LinkedList<Integer>();
			this.retValPopList = new LinkedList<Integer>();
		}
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
		BitSet bytecodeSet = classContext.getBytecodeSet(method);
		if (!(bytecodeSet.get(Constants.POP) || bytecodeSet.get(Constants.POP2)))
			return;
		if (!(bytecodeSet.get(Constants.INVOKEINTERFACE) ||
				bytecodeSet.get(Constants.INVOKESPECIAL) ||
				bytecodeSet.get(Constants.INVOKESTATIC) ||
				bytecodeSet.get(Constants.INVOKEVIRTUAL)))
			return;

		if (DEBUG) System.out.println("Visiting " + method);
		reset();
		super.visitCode(code);

		if (DEBUG)System.out.println(
				"After visit: " +
				queuedWarningList.size() + " queued warnings");
		for (Iterator<QueuedWarning> i = queuedWarningList.iterator(); i.hasNext();) {
			QueuedWarning qw = i.next();

			if (CHECK_CONTROL_FLOW && branchTargetSet.get(qw.popPC))
				continue;
			
			bugReporter.reportBug(qw.warning);
		}
		if (COUNT_POP_BRANCH_TARGETS) {
			for (Iterator<Integer> i = popList.iterator(); i.hasNext();) {
				int pc = i.next().intValue();
				if (branchTargetSet.get(pc))
					++popIsBranchTarget;
			}
		}
		
	}

	private void reset() {
		if (DEBUG) System.out.println("resetting state");
		branchTargetSet.clear();
		queuedWarningList.clear();
		if (COUNT_POP_BRANCH_TARGETS) {
			popList.clear();
			retValPopList.clear();
			lastIsInvoke = false;
		}
		method = null;
	}
	
	public void sawOpcode(int seen) {
		// Mark branch and goto targets
		if (isBranch(seen)) {
			markBranch(getBranchTarget());
			if (seen != GOTO && seen != GOTO_W) {
				markBranch(getBranchFallThrough());
			}
		} else if (isSwitch(seen)) {
			markBranch(getDefaultSwitchOffset());
			int[] switchOffsetList = getSwitchOffsets();
			for (int i = 0; i < switchOffsetList.length; ++i) {
				markBranch(switchOffsetList[i]);
			}
		}
		
		if (COUNT_POP_BRANCH_TARGETS) {
			if (isPop(seen)) {
				popList.add(new Integer(getPC()));
				if (lastIsInvoke)
					retValPopList.add(new Integer(getPC()));
			}
		}
		
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
					queuedWarningList.add(new QueuedWarning(warning, popPC));
					if (DEBUG) System.out.println(queuedWarningList.size() + " queued warnings so far");
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

		if (COUNT_POP_BRANCH_TARGETS) {
			lastIsInvoke = INVOKE_OPCODE_SET.get(seen);
		}

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

	private void markBranch(int branchTarget) {
		if (branchTarget >= 0) {
			branchTargetSet.set(branchTarget);
		}
	}
	
	//@Override
	public void report() {
		if (COUNT_POP_BRANCH_TARGETS) {
			System.out.println(
					"Observed:\n" +
					"\t" + popIsBranchTarget + " pops which were branch targets\n" +
					"\t" + retValPopIsBranchTarget + " return value pops which were branch targets"
					);
		}
	}
}
