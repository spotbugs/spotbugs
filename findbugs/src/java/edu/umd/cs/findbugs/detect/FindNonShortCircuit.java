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

import org.apache.bcel.classfile.Method;

public class FindNonShortCircuit extends BytecodeScanningDetector implements
		StatelessDetector {

	int stage1 = 0;
	int stage2 = 0;
	int distance = 0;
	int operator;

	boolean sawDanger = false;
	boolean sawDangerOld = false;

	private BugReporter bugReporter;

	public FindNonShortCircuit(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	OpcodeStack stack = new OpcodeStack();

	@Override
	public void visit(Method obj) {
		stack.resetForMethodEntry(this);
		stage1 = 0;
		stage2 = 0;
		distance = 1000000;
		sawDanger = sawDangerOld = true;
	}

	@Override
	public void sawOpcode(int seen) {
		stack.mergeJumps(this);
		// System.out.println(getPC() + " " + OPCODE_NAMES[seen] + " " + stage1 + " " + stage2);
		// System.out.println(stack);
		distance++;
		scanForBooleanValue(seen);
		scanForDanger(seen);
		scanForShortCircuit(seen);
		stack.sawOpcode(this, seen);
	}

	private void scanForDanger(int seen) {
		switch (seen) {
		case INVOKEINTERFACE:
		case INVOKESPECIAL:
		case INVOKEVIRTUAL:
		case INVOKESTATIC:
		case ARRAYLENGTH:
		case AALOAD:
		case BALOAD:
		case SALOAD:
		case CALOAD:
		case IALOAD:
		case LALOAD:
		case FALOAD:
		case DALOAD:
		case IDIV:
		case IREM:
		case GETFIELD:
			sawDanger = true;
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
		bugReporter.reportBug(new BugInstance(this, "NS_NON_SHORT_CIRCUIT",
				sawDangerOld ? NORMAL_PRIORITY : LOW_PRIORITY)
				.addClassAndMethod(this).addSourceLine(this, getPC()));
	}

	private void scanForBooleanValue(int seen) {
		switch (seen) {

		case ICONST_1:
			stage1 = 1;
			break;
		case GOTO:
			if (stage1 == 1)
				stage1 = 2;
			else
				stage1 = 0;
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
		sawDangerOld = sawDanger;
		sawDanger = false;
		distance = 0;
		stage1 = 0;

	}
}
