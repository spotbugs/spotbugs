/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004, Tom Truscott <trt@unx.sas.com>
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

import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.visitclass.Constants2;

public class InfiniteRecursiveLoop extends BytecodeScanningDetector implements Constants2, StatelessDetector {

	private BugReporter bugReporter;
	private boolean staticMethod ;
	private boolean seenTransferOfControl;
	private boolean seenStateChange;

	private boolean DEBUG = false;
	public InfiniteRecursiveLoop(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public void visit(JavaClass obj) {
	}

	int parameters;
	public void visit(Method obj) {
		seenTransferOfControl = false;
		seenStateChange = false;
                parameters = stack.resetForMethodEntry(this);
		staticMethod = (obj.getAccessFlags() & (ACC_STATIC)) != 0;
		if (DEBUG ) {
		System.out.println();
		System.out.println(" --- " + getFullyQualifiedMethodName());
		System.out.println();
		}
	}

	public void sawOffset(int offset) {
		seenTransferOfControl = true;
	}

	OpcodeStack stack = new OpcodeStack();


	/** Signal an infinite loop if either:
	 * we see a call to the same method with the same parameters, or
	 * we see a call to the same (dynamically dispatched method), and there
	 * has been no transfer of control.	
	 */
	public void sawOpcode(int seen) {
		if (seenTransferOfControl && seenStateChange) return;

	
		if (DEBUG ) {
		System.out.println(stack);	
		System.out.println(OPCODE_NAMES[seen]);
		}

	
		if ((seen == INVOKEVIRTUAL || seen == INVOKESPECIAL || seen == INVOKEINTERFACE || seen == INVOKESTATIC) 
			    && getNameConstantOperand().equals(getMethodName())
			    && getSigConstantOperand().equals(getMethodSig())
			    && ((seen == INVOKESTATIC) == getMethod().isStatic())
			    && stack.getStackDepth() >= parameters)  {
		if (DEBUG ) {
		System.out.println("IL: Checking...");
		System.out.println(getClassConstantOperand() + "." + getNameConstantOperand() + " : " + getSigConstantOperand());
		System.out.println("vs. " + getClassName() + "." + getMethodName() + " : " + getMethodSig());
		}
		if ( getClassConstantOperand().equals(getClassName())
				|| seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE) {
			// Invocation of same method
			// Now need to see if parameters are the same
			int firstParameter = 0;
			if (getMethodName().equals("<init>")) firstParameter = 1;
			boolean match1 = !seenStateChange;
			for(int i = firstParameter; match1 && i < parameters; i++) {
				OpcodeStack.Item it = stack.getStackItem(parameters-1-i);
				if (!it.isInitialParameter() || it.getRegisterNumber() != i)
					match1 = false;
				}
			boolean match2 = !seenTransferOfControl;
			if (match2 && seen != INVOKESTATIC
					&& !getNameConstantOperand().equals("<init>")) {
				// Have to check if first parmeter is the same
				// know there must be a this argument
				OpcodeStack.Item p = stack.getStackItem(parameters-1);
				if (!p.isInitialParameter()
					|| p.getRegisterNumber() != 0)
					match2 = false;
				}

			if (match1 || match2)  
				bugReporter.reportBug(new BugInstance(this, "IL_INFINITE_RECURSIVE_LOOP", HIGH_PRIORITY)
				        .addClassAndMethod(this)
				        .addSourceLine(this)
					);
			}
		}

               switch(seen) {
                       case ARETURN:
                       case IRETURN:
                       case LRETURN:
                       case RETURN:
                       case DRETURN:
                       case FRETURN:
                               seenTransferOfControl = true;
				break;
			case PUTSTATIC:
			case PUTFIELD:
			case IASTORE:
			case AASTORE:
			case DASTORE:
			case FASTORE:
			case LASTORE:
			case SASTORE:
			case CASTORE:
			case BASTORE:
			case INVOKEVIRTUAL:
			case INVOKESPECIAL:
			case INVOKEINTERFACE:
			case INVOKESTATIC:
				seenStateChange = true;
				break;
                       }
		stack.sawOpcode(this,seen);
	}

}
