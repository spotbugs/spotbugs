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

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.XField;

public class FindSelfComparison extends BytecodeScanningDetector {

	BugReporter bugReporter;

	OpcodeStack stack = new OpcodeStack();

	public FindSelfComparison(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	@Override
	public void visit(JavaClass obj) {
	}

	@Override
	public void visit(Method obj) {
	}

	@Override
	public void visit(Code obj) {
		whichRegister = -1;
		registerLoadCount = 0;
		stack.resetForMethodEntry(this);
		super.visit(obj);
	}

	@Override
	public void sawOpcode(int seen) {
		// System.out.println(getPC() + " " + OPCODE_NAMES[seen] + " " + whichRegister + " " + registerLoadCount);
		stack.mergeJumps(this);
		switch (seen) {
		case FCMPG:
		case DCMPG:
		case DCMPL:
		case FCMPL:
			break;
		case LCMP:
		case IF_ACMPEQ:
		case IF_ACMPNE:
		case IF_ICMPNE:
		case IF_ICMPEQ:
		case IF_ICMPGT:
		case IF_ICMPLE:
		case IF_ICMPLT:
		case IF_ICMPGE: {

			OpcodeStack.Item item0 = stack.getStackItem(0);
			OpcodeStack.Item item1 = stack.getStackItem(1);

			if (item0.getSignature().equals("D")
					|| item0.getSignature().equals("F"))
				break;
			if (item1.getSignature().equals("D")
					|| item1.getSignature().equals("F"))
				break;

			XField field0 = item0.getXField();
			XField field1 = item1.getXField();
			int fr0 = item0.getFieldLoadedFromRegister();
			int fr1 = item1.getFieldLoadedFromRegister();
			if (field0 != null && field0.equals(field1) && fr0 != -1 && fr0 == fr1)
				bugReporter.reportBug(new BugInstance(this,
						"SA_FIELD_SELF_COMPARISON", NORMAL_PRIORITY)
						.addClassAndMethod(this).addField(field0)
						.addSourceLine(this));

			else if (registerLoadCount >= 2) {
					bugReporter.reportBug(new BugInstance(this,
							"SA_LOCAL_SELF_COMPARISON", NORMAL_PRIORITY)
							.addClassAndMethod(this).add(
									LocalVariableAnnotation
											.getLocalVariableAnnotation(
													getMethod(), whichRegister, getPC(),
													getPC() - 1))
							.addSourceLine(this));
			}
		}
		}
		stack.sawOpcode(this, seen);
		if (isRegisterLoad() && seen != IINC) {
			if (getRegisterOperand() == whichRegister) registerLoadCount++;
			else {
				whichRegister = getRegisterOperand();
				registerLoadCount = 1;
			}
		} else {
			whichRegister = -1;
			registerLoadCount = 0;
		}
	}
	int whichRegister;
	int registerLoadCount;
}
