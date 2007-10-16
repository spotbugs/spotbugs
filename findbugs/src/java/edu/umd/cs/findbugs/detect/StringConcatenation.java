/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2004 University of Maryland
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


import java.util.HashMap;
import java.util.Map;

import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.visitclass.DismantleBytecode;

/**
 * Find occurrences of using the String "+" or "+=" operators
 * within a loop.  This is much less efficient than creating
 * a dedicated StringBuffer object outside the loop, and
 * then appending to it.
 *
 * @author Dave Brosius
 * @author William Pugh
 */
public class StringConcatenation extends BytecodeScanningDetector implements StatelessDetector {
	private static final boolean DEBUG
			= SystemProperties.getBoolean("sbsc.debug");

	static final int SEEN_NOTHING = 0;
	static final int SEEN_NEW = 1;
	static final int SEEN_APPEND1 = 2;
	static final int SEEN_APPEND2 = 3;
	static final int CONSTRUCTED_STRING_ON_STACK = 4;
	static final int POSSIBLE_CASE = 5;

	private BugReporter bugReporter;
	private boolean reportedThisMethod;

	private int registerOnStack = -1;
	private int stringSource = -1;
	private int createPC = -1;
	private int state = SEEN_NOTHING;

	public StringConcatenation(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}


	// Keep track of which registers where clobbered at which PC, on
	// a per-method basis
    private Map<Integer,Integer> clobberedRegisters = new HashMap<Integer,Integer>();

	@Override
	public void visit(Method obj) {
		if (DEBUG)
			System.out.println("------------------- Analyzing " + obj.getName() + " ----------------");
		reset();
		clobberedRegisters = new HashMap<Integer,Integer>();
		reportedThisMethod = false;
		super.visit(obj);
	}

	private void reset() {
		state = SEEN_NOTHING;
		createPC = -1;
		registerOnStack = -1;
		stringSource = -1;

		// For debugging: print what call to reset() is being invoked.
		// This helps figure out why the detector is failing to
		// recognize a particular idiom.
		if (DEBUG) System.out.println("Reset from: " + new Throwable().getStackTrace()[1]);
	}

	private boolean storeIntoRegister(int seen, int reg) {
		switch (seen) {
		case ASTORE_0:
			return reg == 0;
		case ASTORE_1:
			return reg == 1;
		case ASTORE_2:
			return reg == 2;
		case ASTORE_3:
			return reg == 3;
		case ASTORE:
			return reg == getRegisterOperand();
		default:
			return false;
		}
	}

	@Override
	public void sawOpcode(int seen) {
		if (reportedThisMethod) return;
		int oldState = state;
		if (DEBUG) {
		    System.out.print("Opcode: ");
		    printOpCode(seen);
		}
		
		// Keep track of registers that are clobbered and at what PC,
		// not including stores due to string concatenations
		int storeTo = -1;
		switch(seen) {
		case ASTORE_0:
	        storeTo = 0;
	        break;
        case ASTORE_1:
            storeTo = 1;
            break;
        case ASTORE_2:
            storeTo = 2;
            break;
        case ASTORE_3:
            storeTo = 3;
            break;
        case ASTORE:
            storeTo = getRegisterOperand();
            break;
		}
		if(storeTo >= 0 && state != CONSTRUCTED_STRING_ON_STACK) {
		    clobberedRegisters.put(storeTo, getPC());
		}
		
		switch (state) {
		case SEEN_NOTHING:
			if ((seen == NEW)
					&&
					getClassConstantOperand().startsWith("java/lang/StringBu")) {
				state = SEEN_NEW;
				createPC = getPC();
			}
			break;

		case SEEN_NEW:
			if (DEBUG && seen == INVOKEVIRTUAL) {
				System.out.println("Invoke virtual");
				System.out.println("   " + getNameConstantOperand());
				System.out.println("   " + getClassConstantOperand());
				System.out.println("   " + getSigConstantOperand());
			}
			if (seen == INVOKEVIRTUAL
					&& "append".equals(getNameConstantOperand())
					&& getClassConstantOperand().startsWith("java/lang/StringBu")) {
				if (DEBUG) System.out.println("Saw string being appended from register " + registerOnStack);
				if (getSigConstantOperand().startsWith("(Ljava/lang/String;)")
						&& registerOnStack >= 0) {
					if (DEBUG)
						System.out.println("Saw string being appended, source = " + registerOnStack);
					state = SEEN_APPEND1;
					stringSource = registerOnStack;
				} else
					reset();
			}
			break;
		case SEEN_APPEND1:
			if (storeIntoRegister(seen, stringSource))
				reset();
			else if (seen == INVOKEVIRTUAL
					&& "append".equals(getNameConstantOperand())
					&& getClassConstantOperand().startsWith("java/lang/StringBu")) {
				state = SEEN_APPEND2;
			}
			break;

		case SEEN_APPEND2:
			if (storeIntoRegister(seen, stringSource))
				reset();
			else if (seen == INVOKEVIRTUAL
					&& "toString".equals(getNameConstantOperand())
					&& getClassConstantOperand().startsWith("java/lang/StringBu")) {
				state = CONSTRUCTED_STRING_ON_STACK;
			}
			break;

		case CONSTRUCTED_STRING_ON_STACK:
			if (storeIntoRegister(seen, stringSource))
				state = POSSIBLE_CASE;
			else
				reset();
			break;

		case POSSIBLE_CASE:
		    // Note: the bottom of a loop is not necessarily a goto; 
		    // one sourceforge bug (Bug1811106) pointed out that for
		    // do/while loops, it may be a if_icmpge.  I generalized
		    // it to any branch.
			if (DismantleBytecode.isBranch(seen)
					&& (getPC() - getBranchTarget()) < 300
					&& getBranchTarget() <= createPC) {
			    
			    // Next check: was the destination register clobbered
			    // elsewhere in this loop?
			    boolean clobberedInLoop = false;
			    for(int reg : clobberedRegisters.keySet()) {
			        if(reg != stringSource) {
			            continue;
			        }
			        int pc = clobberedRegisters.get(reg);
			        if(pc >= getBranchTarget()) {
			            clobberedInLoop = true;
			            break;
			        }
			    }
			    if(clobberedInLoop) {
			        reset();
			        break;
			    }
			    
				bugReporter.reportBug(new BugInstance(this, "SBSC_USE_STRINGBUFFER_CONCATENATION", NORMAL_PRIORITY)
						.addClassAndMethod(this)
						.addSourceLine(this, createPC));
				// System.out.println("SBSC spread: " + (getPC() - getBranchTarget()));
				reset();
				reportedThisMethod = true;
			} else if ((seen == NEW)
					&&
					getClassConstantOperand().startsWith("java/lang/StringBu")) {
				state = SEEN_NEW;
				createPC = getPC();
			} else {
			    if(DismantleBytecode.isBranch(seen) && DEBUG) {
			        System.out.println("Rejecting branch:");
	                System.out.println("  spread: " + (getPC() - getBranchTarget()));
                    System.out.println("  getBranchTarget(): " + getBranchTarget());
                    System.out.println("  createPC: " + createPC);
			    }
			}
			break;
		}
		registerOnStack = -1;
		switch (seen) {
		case ALOAD_0:
			registerOnStack = 0;
			break;
		case ALOAD_1:
			registerOnStack = 1;
			break;
		case ALOAD_2:
			registerOnStack = 2;
			break;
		case ALOAD_3:
			registerOnStack = 3;
			break;
		case ALOAD:
			registerOnStack = getRegisterOperand();
			break;
		}
		if (DEBUG && state != oldState)
			System.out.println("At PC " + getPC()
					+ " changing from state " + oldState
					+ " to state " + state
					+ ", regOnStack = " + registerOnStack);
	}
}

// vim:ts=4
