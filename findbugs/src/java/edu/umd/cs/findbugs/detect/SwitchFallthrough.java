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
import edu.umd.cs.findbugs.*;
import org.apache.bcel.classfile.*;
import edu.umd.cs.findbugs.visitclass.Constants2;

public class SwitchFallthrough extends BytecodeScanningDetector implements   Constants2 {
    private static final boolean DEBUG = Boolean.getBoolean("switchFallthrough.debug");

    int nextIndex = -1;
    boolean reachable = false;
    boolean inSwitch = false;
    int start;
    int switchPC;
    private BugReporter bugReporter;
        LineNumberTable  lineNumbers;

    public SwitchFallthrough(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
	}


   public void visit(Code obj) {
		inSwitch = false;
		reachable = true;
	        lineNumbers = obj.getLineNumberTable();
                if (lineNumbers != null) 
			super.visit(obj);
                }

    public void sawOpcode(int seen) {

	switch (seen) {
		case TABLESWITCH:
		case LOOKUPSWITCH:
		switchPC = PC;
		inSwitch = true;
		reachable = false;
		nextIndex = 0;
		break;
		default:
		}
	if (inSwitch && nextIndex >= switchOffsets.length) 
		inSwitch = false;
	if (inSwitch) {
	  if (PC == switchPC + switchOffsets[nextIndex]
		&& switchOffsets[nextIndex] != defaultSwitchOffset
		) {
		if ( nextIndex>0 && reachable) {
		    int endOfPreviousCase = lineNumbers.getSourceLine(PC-1);
		    int startOfNextCase = lineNumbers.getSourceLine(PC);
		    int previousLabel = switchLabels[nextIndex-1];
		    int nextLabel = switchLabels[nextIndex];
		  if (!(previousLabel == 10 && nextLabel == 13)
		       && !(previousLabel == 13 && nextLabel == 10)
		       && startOfNextCase - endOfPreviousCase <= 2) {
		  System.out.println("Reached the switch for " + switchLabels[nextIndex]
				+ " at line number " +  startOfNextCase 
				+ " in " + betterMethodName);
		    }
		 /*
		  System.out.println("switchPC: " + switchPC);
		  System.out.println("nextIndex: " + nextIndex);
		  System.out.println("switchOffset[nextIndex]: " + switchOffsets[nextIndex]);
		  for(int i = 0; i < switchOffsets.length; i++) 
			System.out.println("	" + switchLabels[i] + "	" + 
				(switchPC + switchOffsets[i]));
		  */
		  }
		do {
			nextIndex++;
			if (nextIndex >= switchOffsets.length) {
				inSwitch = false;
				break;
				}
		} while (PC == switchPC + switchOffsets[nextIndex]);
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

}
