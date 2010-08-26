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

import java.util.Iterator;

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.SignatureParser;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class TestingGround extends OpcodeStackDetector {

	final BugReporter bugReporter;
	final BugAccumulator accumulator;

	public TestingGround(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		this.accumulator = new BugAccumulator(bugReporter);
	}

	@Override
	public void visit(Code code) {
		boolean interesting = true;
		if (interesting)  {
			// initialize any variables we want to initialize for the method
			super.visit(code); // make callbacks to sawOpcode for all opcodes
		}
		accumulator.reportAccumulatedBugs();
	}

	@Override
	public void sawOpcode(int seen) {
		if (seen == INVOKESTATIC 
				&& getClassConstantOperand().equals("com/google/common/base/Preconditions")
				&& getNameConstantOperand().startsWith("check")) {
			SignatureParser parser = new SignatureParser(getSigConstantOperand());
			int count = 0;
			for(Iterator<String> i = parser.parameterSignatureIterator(); i.hasNext(); count++) {
				String parameter = i.next();
				if (parameter.equals("Ljava/lang/Object;")) {
					OpcodeStack.Item item = stack.getStackItem(parser.getNumParameters() - 1 - count);
					XMethod m =  item.getReturnValueOf();
					if (m == null) 
						continue;
					if (!m.getName().equals("toString"))
						continue;
					if (!m.getClassName().startsWith("java.lang.StringB"))
						continue;
					accumulator.accumulateBug(new BugInstance(this,
							"TESTING", NORMAL_PRIORITY)
					.addClassAndMethod(this)
					.addCalledMethod(this), this);
				}
					
			}		
			
		}
				
	}


	

}
