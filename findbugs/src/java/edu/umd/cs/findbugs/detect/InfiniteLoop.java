/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2006 University of Maryland
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

public class InfiniteLoop extends BytecodeScanningDetector  {

	private static final boolean active = true;

	BugReporter bugReporter;

	OpcodeStack stack = new OpcodeStack();
	public InfiniteLoop(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	int state = 0;
	int age = 0;
	int lastBranch;

	@Override
         public void visit(JavaClass obj) {
	}

	@Override
         public void visit(Method obj) {
	}

	@Override
         public void visit(Code obj) {
		// System.out.println(getFullyQualifiedMethodName());
		// unless active, don't bother dismantling bytecode
		if (active) {
			for(int i = 0; i < lastUpdate.length; i++)
				lastUpdate[i] = -1;
			lastBranch = age = -1;
			// System.out.println("TestingGround: " + getFullyQualifiedMethodName());
                	stack.resetForMethodEntry(this);
			super.visit(obj);
		}
	}

	int lastUpdate[] = new int[256];

	@Override
	public void sawOffset(int offset) {
		lastBranch  = getPC();
	}
	@Override
	public void sawOpcode(int seen) {
		stack.mergeJumps(this);
		switch(seen) {
		case ARETURN:
		case IRETURN:
		case RETURN:
		case DRETURN:
		case FRETURN:
		case LRETURN:
		lastBranch = getPC();
		}
		// System.out.println(getPC() + "\t" + OPCODE_NAMES[seen] + "\t" + state);
		if (isRegisterStore()) lastUpdate[getRegisterOperand()] = getPC();
		else {
			switch(state) {
			case 0:
				if (isRegisterLoad()) {
					state = 1;
					age = lastUpdate[getRegisterOperand()];
				}
				break;
			case 1: 
				switch(seen) {
				case ICONST_0:
				case ICONST_1:
				case ICONST_2:
				case ICONST_3:
				case ICONST_4:
				case ICONST_5:
				case ICONST_M1:
				case BIPUSH:
				case SIPUSH:
					state = 2;
					break;
				default:
					state = 0;
				}
				break;
			case 2:
				switch(seen) {
				case IF_ICMPNE:
				case IF_ICMPEQ:
				case IF_ICMPGT:
				case IF_ICMPLE:
				case IF_ICMPLT:
				case IF_ICMPGE:
					if (getBranchOffset() < 0 && age <  getBranchTarget() && lastBranch < getBranchTarget()) {
						BugInstance bug = new BugInstance(this, "IL_INFINITE_LOOP", HIGH_PRIORITY)
				        .addClassAndMethod(this).addSourceLine(this,getPC());
			
						bugReporter.reportBug(bug);
						System.out.println("Found it");
						
					}
				}
				state = 0;
					
			}
		}
		
		stack.sawOpcode(this,seen);
	}
}
