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
		stack.resetForMethodEntry(this);
		super.visit(obj);
	}

	@Override
	public void sawOpcode(int seen) {
		stack.mergeJumps(this);
		switch (seen) {
		case FCMPG:
		case LCMP:
		case DCMPG:
		case DCMPL:
		case FCMPL:
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

			FieldAnnotation field0 = item0.getField();
			FieldAnnotation field1 = item1.getField();
			if (field0 != null && field0.equals(field1))
				bugReporter.reportBug(new BugInstance(this,
						"SA_SELF_COMPARISON", NORMAL_PRIORITY)
						.addClassAndMethod(this).add(field0)
						.addSourceLine(this));

			else {
				int reg0 = item0.getRegisterNumber();
				int reg1 = item1.getRegisterNumber();
				if (reg0 >= 0 && reg0 == reg1)
					bugReporter.reportBug(new BugInstance(this,
							"SA_SELF_COMPARISON", NORMAL_PRIORITY)
							.addClassAndMethod(this).add(
									LocalVariableAnnotation
											.getLocalVariableAnnotation(
													getMethod(), reg0, getPC(),
													getPC() - 1))
							.addSourceLine(this));
			}
		}
		}
		stack.sawOpcode(this, seen);
	}
}
