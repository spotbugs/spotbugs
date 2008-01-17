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

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.SourceLineAnnotation;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.UseAnnotationDatabase;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.CheckReturnAnnotationDatabase;
import edu.umd.cs.findbugs.ba.CheckReturnValueAnnotation;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

/**
 * Look for calls to methods where the return value is erroneously ignored. This
 * detector is meant as a simpler and faster replacement for
 * BCPMethodReturnCheck.
 * 
 * @author David Hovemeyer
 */
public class MethodReturnCheck extends OpcodeStackDetector implements UseAnnotationDatabase {
	private static final boolean DEBUG = SystemProperties.getBoolean("mrc.debug");

	private static final int SCAN = 0;

	private static final int SAW_INVOKE = 1;

	private static final BitSet INVOKE_OPCODE_SET = new BitSet();
	static {
		INVOKE_OPCODE_SET.set(Constants.INVOKEINTERFACE);
		INVOKE_OPCODE_SET.set(Constants.INVOKESPECIAL);
		INVOKE_OPCODE_SET.set(Constants.INVOKESTATIC);
		INVOKE_OPCODE_SET.set(Constants.INVOKEVIRTUAL);
	}

	boolean previousOpcodeWasNEW;

	private final BugReporter bugReporter;
	private final BugAccumulator bugAccumulator;

	private CheckReturnAnnotationDatabase checkReturnAnnotationDatabase;

	private Method method;

	private XMethod callSeen;

	private int state;

	private int callPC;

	public MethodReturnCheck(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		this.bugAccumulator = new BugAccumulator(bugReporter);
	}

	@Override
	public void visitClassContext(ClassContext classContext) {
		checkReturnAnnotationDatabase = AnalysisContext
				.currentAnalysisContext().getCheckReturnAnnotationDatabase();
		super.visitClassContext(classContext);
	}

	@Override
	public void visit(Method method) {
		this.method = method;
	}

	@Override
	public void visitCode(Code code) {
		// Prescreen to find methods with POP or POP2 instructions,
		// and at least one method invocation

		if (DEBUG)
			System.out.println("Visiting " + method);
		super.visitCode(code);
		bugAccumulator.reportAccumulatedBugs();
	}

	@Override
	public void sawOpcode(int seen) {

		if (DEBUG) 
			System.out.println(state + " " + OPCODE_NAMES[seen]);
		
		if (seen == INVOKESPECIAL && getNameConstantOperand().equals("<init>")) {
			int arguments = PreorderVisitor.getNumberArguments(getSigConstantOperand());
			
			if (arguments + 1 == stack.getStackDepth()) {
				OpcodeStack.Item invokedOn = stack.getStackItem(arguments);
				if (!getMethodName().equals("<init>") || invokedOn.getRegisterNumber() != 0) {
					callSeen = XFactory.createReferencedXMethod(this);
					callPC = getPC();
					sawMethodCallWithIgnoredReturnValue();
				}
			}
		}
		if (state == SAW_INVOKE && isPop(seen))
	        sawMethodCallWithIgnoredReturnValue();
        else if (INVOKE_OPCODE_SET.get(seen)) {
			callPC = getPC();
			callSeen = XFactory.createReferencedXMethod(this);
			state = SAW_INVOKE;
			if (DEBUG) System.out.println("  invoking " + callSeen);
		} else
			state = SCAN;

	
		if (seen == NEW) {
			previousOpcodeWasNEW = true;
		} else {
			if (seen == INVOKESPECIAL && previousOpcodeWasNEW) {
				CheckReturnValueAnnotation annotation = checkReturnAnnotationDatabase
						.getResolvedAnnotation(callSeen, false);
				if (annotation != null
						&& annotation != CheckReturnValueAnnotation.CHECK_RETURN_VALUE_IGNORE) {
					int priority = annotation.getPriority();
					if (!checkReturnAnnotationDatabase
							.annotationIsDirect(callSeen)
							&& !callSeen.getSignature().endsWith(
									callSeen.getClassName().replace('.', '/')
											+ ";"))
						priority++;
					bugAccumulator.accumulateBug(new BugInstance(this,
							annotation.getPattern(), priority)
							.addClassAndMethod(this).addCalledMethod(this), this);
				}

			}
			previousOpcodeWasNEW = false;
		}

	}

	/**
     * 
     */
    private void sawMethodCallWithIgnoredReturnValue() {
	    {
			CheckReturnValueAnnotation annotation = checkReturnAnnotationDatabase
					.getResolvedAnnotation(callSeen, false);
			if (annotation != null
					&& annotation != CheckReturnValueAnnotation.CHECK_RETURN_VALUE_IGNORE) {
				int popPC = getPC();
				if (DEBUG)
					System.out.println("Saw POP @" + popPC);
				int catchSize = getSizeOfSurroundingTryBlock(popPC);

				int priority = annotation.getPriority();
				if (catchSize <= 1)
					priority += 2;
				else if (catchSize <= 2)
					priority += 1;
				if (!checkReturnAnnotationDatabase.annotationIsDirect(callSeen)
						&& !callSeen.getSignature()
								.endsWith(
										callSeen.getClassName().replace('.',
												'/')
												+ ";"))
					priority++;
				
				String pattern = annotation.getPattern();
				if (callSeen.getName().equals("<init>") 
						&& callSeen.getClassName().endsWith("Exception"))
					pattern = "RV_EXCEPTION_NOT_THROWN";
				BugInstance warning = new BugInstance(this,
						pattern, priority)
						.addClassAndMethod(this)
						.addMethod(callSeen).describe("METHOD_CALLED");
				bugAccumulator.accumulateBug(warning, SourceLineAnnotation.fromVisitedInstruction(this, callPC));
			}
			state = SCAN;
		}
    }

	private boolean isPop(int seen) {
		return seen == Constants.POP || seen == Constants.POP2;
	}

}
