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
		
        opStack.resetForMethodEntry(this);
		state = SAW_NOTHING;
	
		super.visit(obj);
		if (!found.isEmpty()) {
				BugInstance bug = new BugInstance(this, "FE_FLOATING_POINT_EQUALITY", LOW_PRIORITY)
				        .addClassAndMethod(this);
				for(SourceLineAnnotation s : found)
					bug.add(s);
				bugReporter.reportBug(bug);
				found.clear();
		}
	}
	@Override
         public void sawOpcode(int seen) {
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
						
						state = SAW_NOTHING;
						Number n1 = (Number)first.getConstant();
						Number n2 = (Number)second.getConstant();
						if ((n1 != null) && (n1.doubleValue() == 0.0f))
							return;
						if ((n2 != null) && (n2.doubleValue() == 0.0f))
							return;
					}
					state = SAW_COMP;
				break;
				
				case IFEQ:
				case IFNE:
					if (state == SAW_COMP) {
						SourceLineAnnotation sourceLineAnnotation =
							SourceLineAnnotation.fromVisitedInstruction(getClassContext(), this, getPC());
						if (sourceLineAnnotation != null)
							found.add(sourceLineAnnotation);
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
