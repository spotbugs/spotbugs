/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003,2004 University of Maryland
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

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.visitclass.Constants2;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.LineNumberTable;

public class SwitchFallthrough extends BytecodeScanningDetector implements Constants2 {
	private static final boolean DEBUG = Boolean.getBoolean("switchFallthrough.debug");

	int nextIndex = -1;
	boolean reachable = false;
	boolean inSwitch = false;
	int switchPC;
	private BugReporter bugReporter;
	LineNumberTable lineNumbers;
	private int[] swOffsets = null;
	private int[] swLabels = null;
	private int defSwOffset = 0;
	private int lastSeen = 0;
	private int lastPC = 0;

	public SwitchFallthrough(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void visitClassContext(ClassContext classContext) {
		classContext.getJavaClass().accept(this);
	}

	public void visit(Code obj) {
		inSwitch = false;
		reachable = true;
		swOffsets = null;
		swLabels = null;
		defSwOffset = 0;
		lastSeen = 0;
		lastPC = 0;
/*		lineNumbers = obj.getLineNumberTable();
		if (lineNumbers != null)
*/			super.visit(obj);
	}

	public void sawOpcode(int seen) {
		
		switch (seen) {
		case TABLESWITCH:
		case LOOKUPSWITCH:
			switchPC = getPC();
			inSwitch = true;
			swOffsets = getSwitchOffsets();
			swLabels = getSwitchLabels();
			defSwOffset = getDefaultSwitchOffset();
			reachable = false;
			nextIndex = 0;
			break;		
		default:
		}
		
		if (inSwitch) {
			if (nextIndex >= swOffsets.length)
				inSwitch = false;

			if (inSwitch) {
				if ((getPC() == (switchPC + swOffsets[nextIndex]))
				&&  (swOffsets[nextIndex] != defSwOffset)) {
					if (nextIndex > 0 && reachable) {
						if ((lastSeen != GOTO) && (lastSeen != GOTO_W)) {
							bugReporter.reportBug(new BugInstance("SF_SWITCH_FALLTHROUGH", LOW_PRIORITY)
			        			.addClassAndMethod(this)
			        			.addSourceLineRange(this, lastPC, getPC()));
			        	}

/*	Not sure why this is here, isn't lack of goto enough? 				
						int endOfPreviousCase = lineNumbers.getSourceLine(getPC() - 1);
						int startOfNextCase = lineNumbers.getSourceLine(getPC());
						int previousLabel = swLabels[nextIndex - 1];
						int nextLabel = swLabels[nextIndex];
						if (!(previousLabel == 10 && nextLabel == 13)
						        && !(previousLabel == 13 && nextLabel == 10)
						        && startOfNextCase - endOfPreviousCase <= 2) {
							System.out.println("Reached the switch for " + swLabels[nextIndex]
							        + " at line number " + startOfNextCase
							        + " in " + getFullyQualifiedMethodName());
						}
*/
					}
					do {
						nextIndex++;
						if (nextIndex >= swOffsets.length) {
							inSwitch = false;
							break;
						}
					} while (getPC() == switchPC + swOffsets[nextIndex]);
				}
			}
	
			switch (seen) {
			case TABLESWITCH:
			case LOOKUPSWITCH:
			case ATHROW:
			case RETURN:
			case ARETURN:
			case IRETURN:
			case LRETURN:
			case DRETURN:
			case FRETURN:
			case GOTO_W:
			case GOTO:
				reachable = false;
				break;
			default:
				reachable = true;
			}
		}
		
		lastSeen = seen;
		lastPC = getPC();
	}
}
