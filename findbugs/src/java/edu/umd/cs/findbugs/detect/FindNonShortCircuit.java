/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
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
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

import org.apache.bcel.classfile.Method;

public class FindNonShortCircuit extends OpcodeStackDetector implements
		StatelessDetector {

	int stage1 = 0;
	int stage2 = 0;
	int distance = 0;
	int operator;

	boolean sawDanger;
	boolean sawNullTestOld;
	boolean sawNullTestVeryOld;
	boolean sawNullTest;
	boolean sawDangerOld;
	boolean sawNumericTest, sawNumericTestOld, sawNumericTestVeryOld;
	boolean sawArrayDanger, sawArrayDangerOld;
	boolean sawMethodCall, sawMethodCallOld;

	private BugReporter bugReporter;

	public FindNonShortCircuit(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	@Override
	public void visit(Method obj) {
		clearAll();
		prevOpcode = NOP;
	}

	private void clearAll() {
		stage1 = 0;
		stage2 = 0;
		distance = 1000000;
		sawArrayDanger = sawArrayDangerOld = false;
		sawDanger = sawDangerOld = false;
		sawMethodCall = sawMethodCallOld = false;
		sawNullTest = sawNullTestOld = sawNullTestVeryOld = false;
		sawNumericTest = sawNumericTestOld = sawNumericTestVeryOld = false;
	}
	int prevOpcode;
	@Override
	public void sawOpcode(int seen) {
		// System.out.println(getPC() + " " + OPCODE_NAMES[seen] + " " + stage1 + " " + stage2);
		// System.out.println(stack);
		// System.out.println(getPC() + " " + OPCODE_NAMES[seen] + " " + sawMethodCall + " " + sawMethodCallOld + " " + stage1 + " " + stage2);
		distance++;
		scanForBooleanValue(seen);
		scanForDanger(seen);
		scanForShortCircuit(seen);
		prevOpcode = seen;
	}

	private void scanForDanger(int seen) {
		switch (seen) {
		case AALOAD:
		case BALOAD:
		case SALOAD:
		case CALOAD:
		case IALOAD:
		case LALOAD:
		case FALOAD:
		case DALOAD:
			sawArrayDanger = true;
			sawDanger = true;
			break;

		case INVOKEVIRTUAL:
			if (getNameConstantOperand().equals("length") && getClassConstantOperand().equals("java/lang/String")) break;
			sawDanger = true;
			sawMethodCall = true;
			break;
		case INVOKEINTERFACE:
		case INVOKESPECIAL:
		case INVOKESTATIC:
			   sawDanger = true;
				sawMethodCall = true;
				break;
		case IDIV:
		case IREM:
		case LDIV:
		case LREM:
			sawDanger = true;
			break;

		case ARRAYLENGTH:
		case GETFIELD:
			// null pointer detector will handle these
			break;
		default:
			break;
		}

	}

	private void scanForShortCircuit(int seen) {
		switch (seen) {
		case IAND:
		case IOR:

			// System.out.println("Saw IOR or IAND at distance " + distance);
			OpcodeStack.Item item0 = stack.getStackItem(0);
			OpcodeStack.Item item1 = stack.getStackItem(1);
			if (item0.getConstant() == null && item1.getConstant() == null && distance < 4) {
				if (item0.getRegisterNumber() >= 0 && item1.getRegisterNumber() >= 0)
					if (false) 
						clearAll();
				operator = seen;
				stage2 = 1;
			} else
				stage2 = 0;
			break;
		case IFEQ:
		case IFNE:
			if (stage2 == 1) {
				// System.out.println("Found nsc");
				reportBug();
			}
			stage2 = 0;
			break;
		case PUTFIELD:
		case PUTSTATIC:
		case IRETURN:
			if (operator == IAND && stage2 == 1) {
				reportBug();
			}
			stage2 = 0;
			break;
		default:
			stage2 = 0;
			break;
		}
	}

	private void reportBug() {
		int priority = LOW_PRIORITY;
		String pattern = "NS_NON_SHORT_CIRCUIT";

		if (sawDangerOld) {
			if (sawNullTestVeryOld) priority = HIGH_PRIORITY;
			if (sawMethodCallOld || sawNumericTestVeryOld && sawArrayDangerOld)  {
				priority = HIGH_PRIORITY;
				pattern = "NS_DANGEROUS_NON_SHORT_CIRCUIT";
			}
			else priority = NORMAL_PRIORITY;
		}

		bugReporter.reportBug(new BugInstance(this, pattern,
				priority)
		.addClassAndMethod(this).addSourceLine(this, getPC()));
	}


	private void scanForBooleanValue(int seen) {
		switch (seen) {

		case IAND:
		case IOR:
			switch(prevOpcode) {
			case ILOAD:
			case ILOAD_0:
			case ILOAD_1:
			case ILOAD_2:
			case ILOAD_3:
				clearAll();
			}
			break;
		case ICONST_1:
			stage1 = 1;
			switch(prevOpcode) {
			case IFNONNULL:
			case IFNULL:
				sawNullTest = true;
				break;
			case IF_ICMPGT:
			case IF_ICMPGE:
			case IF_ICMPLT:
			case IF_ICMPLE:
				sawNumericTest = true;
			break;
			}

			break;
		case GOTO:
			if (stage1 == 1)
				stage1 = 2;
			else {
				stage1 = 0;
				clearAll();
			}
			break;
		case ICONST_0:
			if (stage1 == 2) 
				sawBooleanValue();
			stage1 = 0;
			break;
		case INVOKEINTERFACE:
		case INVOKEVIRTUAL:
		case INVOKESPECIAL:
		case INVOKESTATIC:
			String sig = getSigConstantOperand();
			if (sig.endsWith(")Z"))
				sawBooleanValue();
			stage1 = 0;
			break;
		default:
			stage1 = 0;
		}
	}

	private void sawBooleanValue() {
		sawMethodCallOld = sawMethodCall;
		sawDangerOld = sawDanger;
		sawArrayDangerOld = sawArrayDanger;
		sawNullTestVeryOld = sawNullTestOld;
		sawNullTestOld = sawNullTest;
		sawNumericTestVeryOld = sawNumericTestOld;
		sawNumericTestOld = sawNumericTest;
		sawNumericTest = false;
		sawDanger = false;
		sawArrayDanger = false;
		sawMethodCall = false;
		distance = 0;
		stage1 = 0;

	}
}
