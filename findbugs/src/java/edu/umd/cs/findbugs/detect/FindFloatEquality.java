/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005 Jay Dunning
 * Copyright (C) 2005 University of Maryland
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
import org.apache.bcel.classfile.Code;

public class FindFloatEquality extends BytecodeScanningDetector implements StatelessDetector 
{
	private static final int SAW_NOTHING = 0;
	private static final int SAW_COMP = 1;
	private int priority;
	private BugReporter bugReporter;
	private OpcodeStack opStack = new OpcodeStack();
	private int state;

	public FindFloatEquality(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	

	Collection<SourceLineAnnotation> found = new LinkedList<SourceLineAnnotation>();
		
	@Override
         public void visit(Code obj) {
		found.clear();
		priority = LOW_PRIORITY;

		
        opStack.resetForMethodEntry(this);
		state = SAW_NOTHING;
	
		super.visit(obj);
		if (!found.isEmpty()) {
				BugInstance bug = new BugInstance(this, "FE_FLOATING_POINT_EQUALITY", priority)
				        .addClassAndMethod(this);

				for(SourceLineAnnotation s : found)
					bug.add(s);
				bugReporter.reportBug(bug);
				
				found.clear();
		}
	}
	
	public boolean okValueToCompareAgainst(Number n) {
		if (n == null) return false;
		double v = n.doubleValue();
		if (Double.isInfinite(v) || Double.isNaN(v)) return true;
		v = v - Math.floor(v);
		return v == 0.0;
	}
	@Override
         public void sawOpcode(int seen) {
		if (false) System.out.println(OPCODE_NAMES[seen] + " " +  state);
		opStack.mergeJumps(this);
		try {
			switch ( seen ) {
				case FCMPG:
				case FCMPL:
				case DCMPG:
				case DCMPL:
					if (opStack.getStackDepth() >= 2) {
						OpcodeStack.Item first = opStack.getStackItem(0);
						OpcodeStack.Item second = opStack.getStackItem(1);

						Number n1 = (Number)first.getConstant();
						Number n2 = (Number)second.getConstant();
						if (n1 != null && Double.isNaN(n1.doubleValue())
								|| n2 != null && Double.isNaN(n2.doubleValue()) ) {
							BugInstance bug = new BugInstance(this, "FE_TEST_IF_EQUAL_TO_NOT_A_NUMBER", HIGH_PRIORITY)
					        .addClassAndMethod(this).addSourceLine(this);
							bugReporter.reportBug(bug);
							state = SAW_NOTHING;
							break;
						}
						if (first.getSpecialKind() == OpcodeStack.Item.FLOAT_MATH && !okValueToCompareAgainst(n2)
								|| second.getSpecialKind() == OpcodeStack.Item.FLOAT_MATH && !okValueToCompareAgainst(n1)) {
							if (priority != HIGH_PRIORITY) found.clear();
							priority = HIGH_PRIORITY;
							state = SAW_COMP;
							break;
						}
						if (priority == HIGH_PRIORITY) break;
						if (first.isInitialParameter() && n2 != null) break;
						if (second.isInitialParameter() && n1 != null) break;
						if (first.getRegisterNumber() == second.getRegisterNumber()) break;
						if (first.isInitialParameter() && second.isInitialParameter()) break;
						if (n1 != null && n2 != null) break;
						
						if (okValueToCompareAgainst(n1) || okValueToCompareAgainst(n2)) break;
						if (n1 != null || n2 != null) {
							if (priority == LOW_PRIORITY) found.clear();
							priority = NORMAL_PRIORITY;
							
						}
						else if (priority == NORMAL_PRIORITY) break;
						state = SAW_COMP;
					}
				break;
				
				case IFEQ:
				case IFNE:
					if (state == SAW_COMP) {
						SourceLineAnnotation sourceLineAnnotation =
							SourceLineAnnotation.fromVisitedInstruction(getClassContext(), this, getPC());
						if (sourceLineAnnotation != null) {
							found.add(sourceLineAnnotation);
						}
					}
					state = SAW_NOTHING;
				break;
				
				default:
					state = SAW_NOTHING;
				break;
			}
		}
		finally {
			opStack.sawOpcode(this, seen);
		}
	}
}
