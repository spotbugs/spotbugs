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


import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.StatelessDetector;

/**
 * Find comparisons involving values computed with bitwise
 * operations whose outcomes are fixed at compile time.
 *
 * @author Tom Truscott <trt@unx.sas.com>
 */
public class IncompatMask extends BytecodeScanningDetector implements StatelessDetector {
	int state;
	long arg0, arg1;
	int bitop;

	private BugReporter bugReporter;

	public IncompatMask(BugReporter bugReporter) {
		this.state = 0;
		this.bugReporter = bugReporter;
	}



	@Override
		 public void visit(Method obj) {
		super.visit(obj);
		this.state = 0;
	}

	private void checkState(int expectedState) {
		if (state == expectedState)
			state++;
		else
			state = 0;
	}

	private void noteVal(long val) {
		if (state == 0)
			arg0 = val;
		else if (state == 2)
			arg1 = val;
		else
			state = -1;
		state++;
	}

	@Override
		 public void sawInt(int val) {
		noteVal(val);
	}

	@Override
		 public void sawLong(long val) {
		noteVal(val);
	}

	@Override
		 public void sawOpcode(int seen) {
		// System.out.println("BIT: " + state + ": " + OPCODE_NAMES[seen]);

		switch (seen) {
		case ICONST_M1:
			noteVal(-1);
			return;
		case ICONST_0:
			noteVal(0);
			return;
		case ICONST_1:
			noteVal(1);
			return;
		case ICONST_2:
			noteVal(2);
			return;
		case ICONST_3:
			noteVal(3);
			return;
		case ICONST_4:
			noteVal(4);
			return;
		case ICONST_5:
			noteVal(5);
			return;
		case LCONST_0:
			noteVal(0);
			return;
		case LCONST_1:
			noteVal(1);
			return;

		case BIPUSH:
			return;  /* will pick up value via sawInt */
		case LDC2_W:
			return;  /* will pick up value via sawLong */

		case SIPUSH:
			return;  /* will pick up value via sawInt */
		case LDC:
			return;  /* will pick up value via sawInt */

		case IAND:
		case LAND:
			bitop = IAND;
			checkState(1);
			return;
		case IOR:
		case LOR:
			bitop = IOR;
			checkState(1);
			return;

		case LCMP:
			return; /* Ignore. An 'if' opcode will follow */

		case IFEQ:
		case IFNE:
			/* special case: if arg1 is 0 it will not be pushed */
			if (state == 2) {
				arg1 = 0;
				state = 3;
			}

			/* fallthrough */

		case IF_ICMPEQ:
		case IF_ICMPNE:
			checkState(3);
			if (state != 4)
				return;
			break; /* the only break in this switch!  gross */

		case GOTO:
			state = -1;
			return;

		default:
			state = 0;
			return;
		}


		/* We have matched the instruction pattern, so check the args */
		long dif;
		String t;

		if (bitop == IOR) {
			dif = arg0 & ~arg1;
			t = "BIT_IOR";
		} else if (arg0 != 0 || arg1 != 0) {
			dif = arg1 & ~arg0;
			t = "BIT_AND";
		} else {
			dif = 1;
			t = "BIT_AND_ZZ";
		}

		if (dif != 0) {
			// System.out.println("Match at offset " + getPC());
			bugReporter.reportBug(new BugInstance(this, t, NORMAL_PRIORITY)
					.addClassAndMethod(this)
					.addSourceLine(this));
		}
		state = 0;
	}
}

// vim:ts=4
