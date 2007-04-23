/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2005 University of Maryland
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
import java.util.*;
import org.apache.bcel.classfile.*;

/**
 * Find occurrences of Math using constants, where the result of the
 * calculation can be determined statically. Replacing the math formula
 * with the constant performs better, and sometimes is more accurate.
 *
 * @author Dave Brosius
 */
public class UnnecessaryMath extends BytecodeScanningDetector implements StatelessDetector {
	static final int SEEN_NOTHING = 0;
	static final int SEEN_DCONST = 1;

	private BugReporter bugReporter;
	private int state = SEEN_NOTHING;
	private double constValue;

	private static final Set<String> zeroMethods = new HashSet<String>() 
											{{ add("acos");
											   add("asin");
											   add("atan");
											   add("atan2");
											   add("cbrt");
											   add("cos");
											   add("cosh");
											   add("exp");
											   add("expm1");
											   add("log");
											   add("log10");
											   add("pow");
											   add("sin");
											   add("sinh");
											   add("sqrt");
											   add("tan");
											   add("tanh");
											   add("toDegrees");
											   add("toRadians");
											}};
	private static final Set<String> oneMethods = new HashSet<String>()
											{{ add("acos");
											   add("asin");
											   add("atan");
											   add("cbrt");
											   add("exp");
											   add("log");
											   add("log10");
											   add("pow");
											   add("sqrt");
											   add("toDegrees");
											}};
	private static final Set<String> anyMethods = new HashSet<String>()
											{{ add("abs");
											   add("ceil");
											   add("floor");
											   add("rint");
											   add("round");
											}};

	public UnnecessaryMath(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}



	@Override
		 public void visit(Code obj) {
		// Don't complain about unnecessary math calls in class initializers,
		// since they may be there to improve readability.
		if (getMethod().getName().equals("<clinit>"))
			return;

		state = SEEN_NOTHING;
		super.visit(obj);
	}

	@Override
		 public void sawOpcode(int seen) {
		if (state == SEEN_NOTHING) {
			if ((seen == DCONST_0) || (seen == DCONST_1)) {
				constValue = (double) (seen - DCONST_0);
				state = SEEN_DCONST;
			}			
			else if ((seen == LDC2_W) || (seen == LDC_W)) {
				state = SEEN_DCONST;
				Constant c = this.getConstantRefOperand();
				if (c instanceof ConstantDouble)
					constValue = ((ConstantDouble)c).getBytes();
				else if (c instanceof ConstantFloat)
					constValue = ((ConstantFloat)c).getBytes();
				else if (c instanceof ConstantLong)
					constValue = ((ConstantLong)c).getBytes();
				else
					state = SEEN_NOTHING;
			}
		} else if (state == SEEN_DCONST) {
			if (seen == INVOKESTATIC) {
				state = SEEN_NOTHING;
				if (getDottedClassConstantOperand().equals("java.lang.Math")) {
					String methodName = getNameConstantOperand();

					if (((constValue == 0.0) && zeroMethods.contains(methodName))
					||  ((constValue == 1.0) && oneMethods.contains(methodName))
					||   (anyMethods.contains(methodName))) {
						bugReporter.reportBug(new BugInstance("UM_UNNECESSARY_MATH", LOW_PRIORITY)
												.addClassAndMethod(this)
												.addSourceLine(this));				
					}
				}
			}
			state = SEEN_NOTHING;
		}
	}
}

// vim:ts=4
