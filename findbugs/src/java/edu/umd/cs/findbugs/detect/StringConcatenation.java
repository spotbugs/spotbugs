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

import edu.umd.cs.findbugs.*;
import java.util.*;
import org.apache.bcel.classfile.*;
import edu.umd.cs.findbugs.visitclass.DismantleBytecode;
import edu.umd.cs.findbugs.visitclass.Constants2;

/**
 * Find occurrences of using the String "+" or "+=" operators
 * within a loop.  This is much less efficient than creating
 * a dedicated StringBuffer object outside the loop, and
 * then appending to it.
 *
 * @author Dave Brosius
 */
public class StringConcatenation extends BytecodeScanningDetector implements   Constants2 {
	private static final boolean DEBUG = Boolean.getBoolean("sbsc.debug");

	static final int SEEN_NOTHING = 0;
	static final int SEEN_NEW = 1;
	static final int SEEN_DUP = 2;
	static final int SEEN_INVOKESPECIAL = 3;
	static final int POSSIBLE_CASE = 4;
	
	private BugReporter bugReporter;
	
	private int appendPC = -1;
	private int createPC = -1;
	private int state = SEEN_NOTHING;
	
	public StringConcatenation(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void visit(Method obj) {
		if (DEBUG)
			System.out.println("------------------- Analyzing " + obj.getName() + " ----------------");
		reset();
    	super.visit(obj);
	}

	private void reset() {
		state = SEEN_NOTHING;
		appendPC = createPC = -1;

		// For debugging: print what call to reset() is being invoked.
		// This helps figure out why the detector is failing to
		// recognize a particular idiom.
		if (DEBUG) System.out.println("Reset from: " + new Throwable().getStackTrace()[1]);
	}
	
	public void sawOpcode(int seen) {
		switch (state)
		{
		case SEEN_NOTHING:
			if ((seen == NEW) && "java/lang/StringBuffer".equals(getClassConstantOperand())) {
				state = SEEN_NEW;
				createPC = getPC();
			}
			break;
		
		case SEEN_NEW:
			if (seen == DUP)
				state = SEEN_DUP;
			else
				reset();
			break;
		
		case SEEN_DUP:
			if ((seen == INVOKESPECIAL) 
			&&  ("<init>".equals(getNameConstantOperand()))
			&&	("java/lang/StringBuffer".equals(getClassConstantOperand()))) {
				// A StringBuffer is being constructed
				if ("()V".equals(getSigConstantOperand())) {
					state = SEEN_INVOKESPECIAL;
				} else if ("(Ljava/lang/String;)V".equals(getSigConstantOperand())) {
					// The StringBuffer constructor can start with the
					// value of an existing string.
					state = SEEN_INVOKESPECIAL;
					appendPC = getPC();
				} else {
					reset();
				}
			} else
				reset();
			break;
		
		case SEEN_INVOKESPECIAL:
			if ((seen == ASTORE_0)
			||  (seen == ASTORE_1)
			||  (seen == ASTORE_2)
			||  (seen == ASTORE_3)
			||  (seen == ASTORE)) {
				reset();
				break;
			} else
				state = POSSIBLE_CASE;
			// FALL THROUGH
				
		case POSSIBLE_CASE:
			if ((seen == GOTO) && (appendPC >= 0)) {
				if (getBranchTarget() < getPC()
				&&	getBranchTarget() < createPC
				&&	getBranchTarget() < appendPC) {
					bugReporter.reportBug(new BugInstance("SBSC_USE_STRINGBUFFER_CONCATENATION", NORMAL_PRIORITY)
						.addClassAndMethod(this)
						.addSourceLine(this, appendPC));
				}
				reset();
			}
			else if (seen == INVOKEVIRTUAL) {

				if ("append".equals(getNameConstantOperand())
				&&	"(Ljava/lang/String;)Ljava/lang/StringBuffer;".equals(getSigConstantOperand())) {
					appendPC = getPC();
				} else if ("Ljava/lang/StringBuffer;".equals(getClassConstantOperand())
				&&	"toString".equals(getNameConstantOperand())
				&&	"()Ljava/lang/String;".equals(getSigConstantOperand())) {
					// The StringBuffer appears to be consumed in the loop.
					// In the pattern we're looking for, the StringBuffer
					// is consumed AFTER the exit from the loop.
					reset();
				}
			} else if ((seen == NEW) && "java/lang/StringBuffer".equals(getClassConstantOperand())) {
				reset();
				state = SEEN_NEW;
			}
				
			break;
					
		default:
			reset();
			break;
		}
	}
}

// vim:ts=4
