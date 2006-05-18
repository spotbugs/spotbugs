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

import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.ba.*;
import java.util.BitSet;
import org.apache.bcel.Constants;
import org.apache.bcel.classfile.*;

/**
 * Look for calls to methods where the return value is erroneously ignored. This
 * detector is meant as a simpler and faster replacement for
 * BCPMethodReturnCheck.
 * 
 * @author David Hovemeyer
 */
public class MethodReturnCheck extends BytecodeScanningDetector {
	private static final boolean DEBUG = Boolean.getBoolean("mrc.debug");

	private static final boolean CHECK_ALL = Boolean.getBoolean("mrc.checkall");

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

	private BugReporter bugReporter;

	private ClassContext classContext;

	private CheckReturnAnnotationDatabase checkReturnAnnotationDatabase;

	private Method method;

	private XMethod callSeen;

	private int state;

	private int callPC;

	private String className, methodName, signature;

	public MethodReturnCheck(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	@Override
	public void visitClassContext(ClassContext classContext) {
		this.classContext = classContext;
		checkReturnAnnotationDatabase = AnalysisContext
				.currentAnalysisContext().getCheckReturnAnnotationDatabase();
		super.visitClassContext(classContext);
		this.classContext = null;
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
	}

	@Override
	public void sawOpcode(int seen) {

		if (state == SAW_INVOKE && isPop(seen)) {
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
				BugInstance warning = new BugInstance(this,
						"RV_RETURN_VALUE_IGNORED", priority).addClassAndMethod(
						this).addMethod(className, methodName, signature,
						seen == Constants.INVOKESTATIC).describe(
						"METHOD_CALLED").addSourceLine(this, callPC);
				bugReporter.reportBug(warning);
			}
			state = SCAN;
		} else if (INVOKE_OPCODE_SET.get(seen)) {
			callPC = getPC();
			className = getDottedClassConstantOperand();
			methodName = getNameConstantOperand();
			signature = getSigConstantOperand();
			callSeen = XFactory.createXMethod(className, methodName, signature,
					seen == INVOKESTATIC);
			state = SAW_INVOKE;
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
					bugReporter.reportBug(new BugInstance(this,
							"RV_RETURN_VALUE_IGNORED", priority)
							.addClassAndMethod(this).addCalledMethod(this).describe(
									"METHOD_CALLED").addSourceLine(this));
				}
				
			}
			previousOpcodeWasNEW = false;
		}

	}

	private boolean isPop(int seen) {
		return seen == Constants.POP || seen == Constants.POP2;
	}

}
