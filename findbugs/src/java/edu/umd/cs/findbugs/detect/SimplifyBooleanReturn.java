/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004,2005 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2004,2005 University of Maryland
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

import java.util.BitSet;

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.visitclass.Constants2;

public class SimplifyBooleanReturn extends BytecodeScanningDetector implements Constants2, StatelessDetector {
	private static final int SAW_NOTHING = 0;
	private static final int SAW_IF = 1;
	private static final int SAW_ICONST_PART1 = 2;
	private static final int SAW_IRETURN_PART1 = 3;
	private static final int SAW_ICONST_PART2 = 4;
	
	private static BitSet ifOpCodes = new BitSet()
	{{
		set(IF_ACMPEQ);
		set(IF_ACMPNE);
		set(IF_ICMPEQ);
		set(IF_ICMPGE);
		set(IF_ICMPGT);
		set(IF_ICMPLE);
		set(IF_ICMPLT);
		set(IF_ICMPNE);
		set(IFEQ);
		set(IFGE);
		set(IFGT);
		set(IFLE);
		set(IFLT);
		set(IFNE);
		set(IFNONNULL);
		set(IFNULL);
	}};
	
	private BugReporter bugReporter;
	private int state;
	private int startPC;
	
	public SimplifyBooleanReturn(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
	
	public void visit(Code obj) {
		if (getMethodSig().endsWith(")Z")) {
			state = SAW_NOTHING;
			super.visit(obj);
		}
	}
	
	public void sawOpcode(int seen) {
		switch (state) {
			case SAW_NOTHING:
				if (ifOpCodes.get(seen)) 
					state = SAW_IF;
			break;
			
			case SAW_IF:
				if ((seen == ICONST_0) || (seen == ICONST_1)) {
					state = SAW_ICONST_PART1;
					startPC = getPC();
				}
				else
					state = SAW_NOTHING;
			break;
			
			case SAW_ICONST_PART1:
				if (seen == IRETURN)
					state = SAW_IRETURN_PART1;
				else
					state = SAW_NOTHING;
			break;
			
			case SAW_IRETURN_PART1:
				if ((seen == ICONST_0) || (seen == ICONST_1))
					state = SAW_ICONST_PART2;
				else
					state = SAW_NOTHING;
			break;
			
			case SAW_ICONST_PART2:
				if (seen == IRETURN)
					bugReporter.reportBug( new BugInstance(this, "SBR_SIMPLIFY_BOOLEAN_RETURN", LOW_PRIORITY )
				        	.addClassAndMethod(this)
				        	.addSourceLineRange(this, startPC, getPC()));
				else
					state = SAW_NOTHING;
			break;
		}
	}
}