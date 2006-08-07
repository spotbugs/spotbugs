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

import org.apache.bcel.classfile.*;

public class UseOfNonHashableClassInHashDataStructure extends BytecodeScanningDetector {

	BugReporter bugReporter;

	OpcodeStack stack = new OpcodeStack();

	public UseOfNonHashableClassInHashDataStructure(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	@Override
	public void visit(JavaClass obj) {
	}

	@Override
	public void visit(Method obj) {
	}

	@Override
	public void visit(Code obj) {
			stack.resetForMethodEntry(this);
			super.visit(obj);
	}

	@Override
	public void sawOpcode(int seen) {
		stack.mergeJumps(this);
		if (seen == INVOKEVIRTUAL) {
			String className = getClassConstantOperand();
			if (className.equals("java/util/Map") || className.equals("java/util/HashMap") 
					|| className.equals("java/util/LinkedHashMap")
					|| className.equals("java/util/concurrent/ConcurrentHashMap") ) {
				if (getNameConstantOperand().equals("put")
						&& getSigConstantOperand()
						.equals("(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
						&& stack.getStackDepth() >= 3) check(1);
				else if ((getNameConstantOperand().equals("get") || getNameConstantOperand().equals("remove"))
						&& getSigConstantOperand()
						.equals("(Ljava/lang/Object;)Ljava/lang/Object;")
						&& stack.getStackDepth() >= 2) check(0);
			}	else if (className.equals("java/util/Set") || className.equals("java/util/HashSet")  ) {
				if (getNameConstantOperand().equals("add") || getNameConstantOperand().equals("contains") || getNameConstantOperand().equals("remove")
						&& getSigConstantOperand()
						.equals("(Ljava/lang/Object;)Z")
						&& stack.getStackDepth() >= 2) check(0);
			}
		}
		stack.sawOpcode(this, seen);
	}
	private void check(int pos) {
		OpcodeStack.Item item = stack.getStackItem(pos);
		JavaClass type = null;
		try {
			type = item.getJavaClass();
			if (type == null) return;

			if (!FindHEmismatch.isHashableClassName(type.getClassName())) {
				bugReporter.reportBug(new BugInstance("HE_USE_OF_UNHASHABLE_CLASS", getClassConstantOperand().indexOf("Hash") >= 0 ? HIGH_PRIORITY : NORMAL_PRIORITY)
				.addClassAndMethod(this)
				.addClass(type)
				.addClass(getClassConstantOperand())
				.addSourceLine(this));
			}
		} catch (ClassNotFoundException e) {
			return;
		}
		
		

	}
}
