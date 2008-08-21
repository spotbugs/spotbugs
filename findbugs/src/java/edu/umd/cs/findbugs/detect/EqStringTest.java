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


import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.TypeAnnotation;

public class EqStringTest extends BytecodeScanningDetector implements  StatelessDetector {
	boolean constantOnTOS = false;
	boolean callToInternSeen = false;
	boolean callToEqualsSeen = false;
	private BugAccumulator bugAccumulator;
	// String stringOnTop;

	public EqStringTest(BugReporter bugReporter) {
		this.bugAccumulator = new BugAccumulator(bugReporter);;
	}



	@Override
		 public void visit(Method obj) {
		super.visit(obj);
		if (callToEqualsSeen)
			bugAccumulator.clearBugs();
		else
			bugAccumulator.reportAccumulatedBugs();
		constantOnTOS = false;
		callToInternSeen = false;
		callToEqualsSeen = false;
	}


	@Override
		 public void sawOpcode(int seen) {

		switch (seen) {
		case LDC:
		case LDC_W:
			constantOnTOS = true;
			// stringOnTop = stringConstant;
			return;
		case INVOKEVIRTUAL:
			if (getRefConstantOperand().equals("java.lang.String.intern : ()Ljava.lang.String;"))
				callToInternSeen = true;
			if (getRefConstantOperand().equals("java.lang.String.equals : (Ljava.lang.Object;)Z")
				    || getRefConstantOperand().equals("java.lang.String.compareTo : (Ljava.lang.String;)I"))
				callToEqualsSeen = true;
			break;
		case IF_ACMPEQ:
		case IF_ACMPNE:
			if (constantOnTOS && !callToInternSeen)
				bugAccumulator.accumulateBug(new BugInstance(this, "ES_COMPARING_STRINGS_WITH_EQ", NORMAL_PRIORITY)
						.addClassAndMethod(this)
						.addType("Ljava/lang/String;").describe(TypeAnnotation.FOUND_ROLE), this);
			break;
		default:
			break;
		}
		constantOnTOS = false;
	}
}
