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

import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.ba.AnalysisContext;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;

public class TestingGround extends BytecodeScanningDetector {

	private static final boolean active = SystemProperties.getBoolean("findbugs.tg.active");

	BugReporter bugReporter;

	OpcodeStack stack = new OpcodeStack();

	public TestingGround(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	@Override
	public void visit(JavaClass obj) {
	}

	@Override
	public void visit(Method obj) {
		if (Character.isUpperCase(obj.getName().charAt(0))) {
				BugInstance bug = new BugInstance(this, "TESTING", NORMAL_PRIORITY)
				.addClass(this).addMethod(this).addString("method should start with lower case character");
				bugReporter.reportBug(bug);
			}

	}
	@Override
	public void visit(Field obj) {
		if (obj.isFinal() && obj.isStatic() 
            && !obj.getName().equals(obj.getName().toUpperCase()) 
            && !obj.getName().equals("serialVersionUID")) {
			BugInstance bug = new BugInstance(this, "TESTING", 
					obj.getSignature().equals("I") ? HIGH_PRIORITY : NORMAL_PRIORITY)
			
			.addClass(this).addField(this).addString("Should be upper case");
			bugReporter.reportBug(bug);
		}
	}

	@Override
	public void visit(Code obj) {
		// unless active, don't bother dismantling bytecode
		if (active) {
			stack.resetForMethodEntry(this);
			super.visit(obj);
		}
	}

	int prevOpcode = 0;
	@Override
	public void sawOpcode(int seen) {
		stack.mergeJumps(this);
		
		if (prevOpcode == I2D && seen == INVOKESTATIC
				&& getNameConstantOperand().equals("ceil")
				&& getClassConstantOperand().equals("java.lang.Math"))
			bugReporter.reportBug(new BugInstance(this, "TESTING", HIGH_PRIORITY)
			.addClassAndMethod(this).addCalledMethod(this).addSourceLine(this));
	
		
		prevOpcode = seen;
		
		stack.sawOpcode(this, seen);
	}
}
