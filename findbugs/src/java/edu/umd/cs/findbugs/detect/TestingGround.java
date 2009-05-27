/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004-2006 University of Maryland
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

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.generic.InstructionHandle;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.FieldSummary;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.Global;

public class TestingGround extends OpcodeStackDetector {

	BugReporter bugReporter;

	public TestingGround(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	@Override
	public void visit(Code code) {
		boolean interesting = !getMethod().isSynchronized() && getMethod().isStatic() && !getMethodName().equals("<clinit>");
		if (interesting) {
			resetStateMachine();
			// System.out.println();
			// System.out.println(getFullyQualifiedMethodName());
			super.visit(code); // make callbacks to sawOpcode for all opcodes
		}
	}

	boolean interestingQuick(XField xField) {
		if (xField.isFinal() || xField.isVolatile() || xField.isSynthetic() || !xField.isStatic())
			return false;
		if (xField.getName().indexOf('$') >= 0)
			return false;
		String sig = xField.getSignature();
		char c = sig.charAt(0);
		if (c != 'L' && c != '[')
			return false;
		if (sig.startsWith("Ljava/lang/"))
			return false;

		return true;
	}

	boolean interestingDeep(XField xField) {
		String sig = xField.getSignature();
		if (sig.charAt(0) == 'L') {
			ClassDescriptor fieldType = DescriptorFactory.createClassDescriptorFromFieldSignature(sig);

			while (fieldType != null) {
				XClass fieldClass;
				try {
					fieldClass = Global.getAnalysisCache().getClassAnalysis(XClass.class, fieldType);
				} catch (CheckedAnalysisException e) {
					break;
				}

				String name = fieldClass.getClassDescriptor().getClassName();
				if (name.startsWith("java/awt") || name.startsWith("javax/swing"))
					return false;
				if (name.equals("java/lang/Object"))
					break;
				fieldType = fieldClass.getSuperclassDescriptor();
			}
		}

		return true;
	}

	@Override
	public void sawBranchTo(int pc) {
		if (state == 999)
			state = 2;
		else if (state != 10)
			resetStateMachine();
	}

	int state;

	int target;

	int startPC;

	boolean sawNew;

	XField f;

	@Override
	public void sawOpcode(int seen) {
		// System.out.printf("%5d %9s: %d\n", getPC(), OPCODE_NAMES[seen],
		// state);
		if (isReturn(seen) && state != 11 && target != -42) {
			resetStateMachine();
			return;
		}

		if (state > 1 && (getPC() >= target && target >= 0 || isReturn(seen) && target == -42)) {
			if ((state == 4 || state == 3 && !f.isVolatile()) && interestingDeep(f)) {
				// found it
				int priority = LOW_PRIORITY;
				boolean isDefaultAccess = (getMethod().getAccessFlags() & (Constants.ACC_PUBLIC | Constants.ACC_PRIVATE | Constants.ACC_PROTECTED)) == 0;
				if (getMethod().isPublic())
					priority = NORMAL_PRIORITY;
				else if (getMethod().isProtected() || isDefaultAccess)
					priority = NORMAL_PRIORITY;
				String signature = f.getSignature();
				if (signature.startsWith("[") || signature.startsWith("Ljava/util/"))
					priority--;
				if (!sawNew)
					priority++;
				if (state == 3 && priority < LOW_PRIORITY)
					priority = LOW_PRIORITY;
				if (getXClass().usesConcurrency())
					priority--;
				// Report the bug.
				bugReporter.reportBug(new BugInstance(this, state == 4 ? "LI_LAZY_INIT_UPDATE_STATIC" : "LI_LAZY_INIT_STATIC",
				        priority).addClassAndMethod(this).addField(f).describe("FIELD_ON").addSourceLineRange(getClassContext(),
				        this, startPC, getPC()));

			}
			resetStateMachine();
		}
		switch (state) {
		case 0:
			if (seen == GETSTATIC) {
				XField xField = getXFieldOperand();
				if (xField == null) {
					
					return;
				}
				if (interestingQuick(xField)) {
					state = 1;
					f = getXFieldOperand();
					sawNew = false;
					startPC = getPC();
				}
			}
			break;
		case 1:
			if (seen == IFNONNULL) {
				state = 999;
				target = getBranchTarget();
			} else if (seen == IFNULL) {
				state = 10;
				target = getBranchTarget();
			} else
				resetStateMachine();
			break;
		case 2:
			if (seen == PUTSTATIC) {
				if (f.equals(getXFieldOperand()))
					state = 3;
				else
					resetStateMachine();
			} else
				switch (seen) {
				case NEW:
				case NEWARRAY:
				case MULTIANEWARRAY:
				case ANEWARRAY:
					sawNew = true;
					break;
				case INVOKESTATIC:
					if (getNameConstantOperand().startsWith("new"))
						sawNew = true;
					break;
				}

			break;
		case 3:

			if (seen == GETSTATIC && f.equals(getXFieldOperand()))
				state = 4;
			break;
		case 10:
			if (seen == GETSTATIC && f.equals(getXFieldOperand()))
				state = 11;
			break;
		case 11:
			if (isReturn(seen) && target == getPC() + 1) {
				state = 2;
				target = -42;
			} else
				resetStateMachine();
			break;
		}

	}

	/**
     * 
     */
	private void resetStateMachine() {
		state = 0;
		target = -1;
	}

}
