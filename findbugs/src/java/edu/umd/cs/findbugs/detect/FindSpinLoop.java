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
import org.apache.bcel.classfile.Method;
import edu.umd.cs.findbugs.visitclass.Constants2;

public class FindSpinLoop extends BytecodeScanningDetector implements   Constants2 {
    private static final boolean DEBUG = Boolean.getBoolean("findspinloop.debug");

    int stage = 0;
    int stage1 = 0;
    int stage2 = 0;
    int distance = 0;
    int distance2 = 0;
    int start;
    private BugReporter bugReporter;

    public FindSpinLoop(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
	}

    public void visit(Method obj) {
	if (DEBUG) System.out.println("Saw " + betterMethodName);
	stage = 0;
	stage1 = 0;
	stage2 = 0;
	distance = 1000000;
	distance2 = 1000000;
	}

    public void sawOpcode(int seen) {
	/* prototype for short-circuit bug */
	/*
	distance++;
	switch (seen) {
		case ICONST_1: 
			stage1 = 1;
			break;
		case GOTO:
			if (stage1 == 1) stage1 = 2;
			else stage1 = 0;
			break;
		case ICONST_0:
			if (stage1 == 2) 
				distance = 0;
			stage1 = 0;
		default:
			stage1 = 0;
		}
	switch (seen) {
		case IAND: 
		case IOR: 
			if (distance < 4)  {
				distance2 = distance;
				stage2 = 1;
				}
			else stage2 = 0;
			break;
		case IFEQ: 
		case IFNE: 
			if (stage2 == 1)  
				System.out.println("non-short-circuit logic at distance " + distance2 + " in " + betterMethodName);
			stage2 = 0;
			break;
		default:
			stage2 = 0;
			break;
		};

	*/

	// System.out.println("PC: " + PC + ", stage: " + stage1);
	switch (seen) {
		case ALOAD_0: 
			if (DEBUG) System.out.println("   ALOAD_0 at PC " + PC);
			start = PC;
			stage  = 1;
			break;
		case GETFIELD:
			if (DEBUG) System.out.println("   getfield in stage " + stage);
			if (stage == 1) {
				stage = 2;
				}
			else stage = 0;
			break;
		case GOTO:
		case IFNE:
		case IFEQ:
		case IFNULL:
		case IFNONNULL:
			if (DEBUG) System.out.println("   conditional branch in stage " + stage + " to " + branchTarget );
			if (stage == 2 && branchTarget == start) {
				bugReporter.reportBug(new BugInstance("SP_SPIN_ON_FIELD", NORMAL_PRIORITY)
					.addClassAndMethod(this)
					.addReferencedField(this));
				stage = 0;
				}
			else if (branchTarget < PC)
				stage = 0;
			break;
		default:
			stage = 0;
			break;
		}

		}
}
