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

import java.math.BigDecimal;

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.OpcodeStack.Item;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.FieldSummary;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class TestingGround extends OpcodeStackDetector {

	BugReporter bugReporter;

	public TestingGround(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	@Override
	public void visit(Code code) {
		boolean interesting = true;
		if (interesting)  {
			// initialize any variables we want to initialize for the method
			super.visit(code); // make callbacks to sawOpcode for all opcodes
		}
	}

	@Override
	public void sawOpcode(int seen) {
		if (seen == INVOKESPECIAL 
				&& getClassConstantOperand().equals("java/math/BigDecimal")
				&& getNameConstantOperand().equals("<init>")
				&& getSigConstantOperand().equals("(D)V")) {
			OpcodeStack.Item top = stack.getStackItem(0);
			Object value = top.getConstant();
			if (value instanceof Double) {
				double arg = ((Double) value).doubleValue();
				String dblString = Double.toString(arg);
				String bigDecimalString = new BigDecimal(arg).toString();
				boolean ok = dblString.equals(bigDecimalString) || dblString.equals(bigDecimalString + ".0");
				
				if (!ok) {
					boolean scary = dblString.length() <= 8 && dblString.toUpperCase().indexOf("E") == -1;
					bugReporter.reportBug(new BugInstance(this, "TESTING", scary ? NORMAL_PRIORITY : LOW_PRIORITY )
							.addClassAndMethod(this).addString(dblString).addSourceLine(this));
				}
			}
			
		}
				
	}


	

}
