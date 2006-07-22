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

public class FindNonShortCircuit extends BytecodeScanningDetector implements  StatelessDetector {

	int stage1 = 0;
	int stage2 = 0;
	int distance = 0;
	//int distance2 = 0;
	int operator;
	boolean sawDereference = false;
	boolean sawInvocation = false;
	private BugReporter bugReporter;

	public FindNonShortCircuit(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}



	@Override
         public void visit(Method obj) {
		stage1 = 0;
		stage2 = 0;
		distance = 1000000;
		//distance2 = 1000000;
	}

	@Override
         public void sawOpcode(int seen) {
		/* prototype for short-circuit bug */
		distance++;
		switch (seen) {
		case IFNONNULL:
		case IFNULL:
			stage1 = 3;
			break;
		case ICONST_1:
			if (stage1 != 3)
				stage1 = 1;
			else stage1 = 0;
			break;
		case GOTO:
			if (stage1 == 1)
				stage1 = 2;
			else
				stage1 = 0;
			break;
		case ICONST_0:
			if (stage1 == 2) {
				distance = 0;
				// System.out.println("saw 1; goto X; 0");
			}
			stage1 = 0;
			break;
		case INVOKEINTERFACE:
		case INVOKEVIRTUAL:
		case INVOKESPECIAL:
		case INVOKESTATIC:
			String sig = getSigConstantOperand();
			if (sig.endsWith(")Z"))
					distance = 0;
			stage1 = 0;
			break;
		default:
			stage1 = 0;
		}
		switch (seen) {
		case IAND:
		case IOR:
			// System.out.println("Saw IOR or IAND at distance " + distance);

			if (distance < 4) {
				operator = seen;
				//distance2 = distance;
				stage2 = 1;
			} else
				stage2 = 0;
			break;
		case IFEQ:
		case IFNE:
			if (stage2 == 1) {
				// System.out.println("Found nsc");
				bugReporter.reportBug(new BugInstance(this, "NS_NON_SHORT_CIRCUIT",
				        NORMAL_PRIORITY)
				        .addClassAndMethod(this)
				        .addSourceLine(this, getPC()));
			}
			stage2 = 0;
			break;
		case PUTFIELD:
		case PUTSTATIC:
		case IRETURN:
			if (operator == IAND && stage2 == 1) {
				// System.out.println("Found nsc");
				bugReporter.reportBug(new BugInstance(this, "NS_NON_SHORT_CIRCUIT",
				        NORMAL_PRIORITY)
				        .addClassAndMethod(this)
				        .addSourceLine(this, getPC()));
			}
			stage2 = 0;
			break;
		default:
			stage2 = 0;
			break;
		}


	}
}
