/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004,2005, Tom Truscott <trt@unx.sas.com>
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

import java.text.NumberFormat;

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.visitclass.Constants2;

public class TestingGround extends BytecodeScanningDetector implements Constants2, StatelessDetector {

	private static final boolean active = Boolean.getBoolean("findbugs.tg.active");;
	private NumberFormat formatter = null;

	public TestingGround(BugReporter bugReporter) {
		if (active) {
			formatter = NumberFormat.getIntegerInstance();
			formatter.setMinimumIntegerDigits(4);
			formatter.setGroupingUsed(false);
		}
	}

	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public void visit(JavaClass obj) {
	}

	public void visit(Method obj) {
	}

	public void visit(Code obj) {
		// unless active, don't bother dismantling bytecode
		if (active) {
			System.out.println("TestingGround: " + getFullyQualifiedMethodName());
			super.visit(obj);
		}
	}


	public void sawOpcode(int seen) {

		printOpCode(seen);

	}

	private void printOpCode(int seen) {
		System.out.print("  TestingGround: [" + formatter.format(getPC()) + "]  " + OPCODE_NAMES[seen]);
		if ((seen == INVOKEVIRTUAL) || (seen == INVOKESPECIAL) || (seen == INVOKEINTERFACE) || (seen == INVOKESTATIC))
			System.out.print("   " + getClassConstantOperand() + "." + getNameConstantOperand() + " " + getSigConstantOperand());
		else if (seen == LDC || seen == LDC_W || seen == LDC2_W) {
			Constant c = getConstantRefOperand();
			if (c instanceof ConstantString)
				System.out.print("   \"" + getStringConstantOperand() + "\"");
			else if (c instanceof ConstantClass)
				System.out.print("   " + getClassConstantOperand());
			else
				System.out.print("   " + c);
		} else if ((seen == ALOAD) || (seen == ASTORE))
			System.out.print("   " + getRegisterOperand());
		else if ((seen == GOTO) || (seen == GOTO_W)
		||       (seen == IF_ACMPEQ) || (seen == IF_ACMPNE)
		||       (seen == IF_ICMPEQ) || (seen == IF_ICMPGE)
		||       (seen == IF_ICMPGT) || (seen == IF_ICMPLE)
		||       (seen == IF_ICMPLT) || (seen == IF_ICMPNE)
		||       (seen == IFEQ) 	|| (seen == IFGE)
		||       (seen == IFGT) 	|| (seen == IFLE)
		||       (seen == IFLT) 	|| (seen == IFNE)
		||       (seen == IFNONNULL) || (seen == IFNULL))
			System.out.print("   " + getBranchTarget());
		else if ((seen == NEW) || (seen == INSTANCEOF))
			System.out.print("   " + getClassConstantOperand());
		else if ((seen == TABLESWITCH) || (seen == LOOKUPSWITCH)) {
			System.out.print("    [");
			int switchPC = getPC();
			int[] offsets = getSwitchOffsets();
			for (int i = 0; i < offsets.length; i++) {
				System.out.print((switchPC + offsets[i]) + ",");
			}
			System.out.print((switchPC + getDefaultSwitchOffset()) + "]");
		}

		System.out.println();
	}
}
