/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004 Dave Brosius <dbrosius@users.sourceforge.net>
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

public class StringConcatenation extends BytecodeScanningDetector implements   Constants2 {
	static final int SEEN_NOTHING = 0;
	static final int SEEN_NEW = 1;
	static final int SEEN_DUP = 2;
	static final int SEEN_INVOKESPECIAL = 3;
	static final int POSSIBLE_CASE = 4;
	
	private BugReporter bugReporter;
	
	private int appendPC = -1;
	private int state = SEEN_NOTHING;
	
	public StringConcatenation(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void visit(Method obj) {
    	appendPC = -1;
    	state = SEEN_NOTHING;
    	super.visit(obj);
	}
	
	public void sawOpcode(int seen) {
		switch (state)
		{
		case SEEN_NOTHING:
			if ((seen == NEW) && "java/lang/StringBuffer".equals(getClassConstantOperand()))
				state = SEEN_NEW;
			break;
		
		case SEEN_NEW:
			if (seen == DUP)
				state = SEEN_DUP;
			else
				state = SEEN_NOTHING;
			break;
		
		case SEEN_DUP:
			if ((seen == INVOKESPECIAL) 
			&&  ("<init>".equals(getNameConstantOperand()))
			&&  ("()V".equals(getSigConstantOperand())))
				state = SEEN_INVOKESPECIAL;
			else
				state = SEEN_NOTHING;
			break;
		
		case SEEN_INVOKESPECIAL:
			if ((seen == ASTORE_0)
			||  (seen == ASTORE_1)
			||  (seen == ASTORE_2)
			||  (seen == ASTORE_3)
			||  (seen == ASTORE))
				state = SEEN_NOTHING;
			else
				state = POSSIBLE_CASE;
			break;
				
		case POSSIBLE_CASE:
			if ((seen == GOTO) && (appendPC >= 0)) {
				if (getBranchTarget() < appendPC) {
					bugReporter.reportBug(new BugInstance("SBSC_USE_STRINGBUFFER_CONCATENATION", NORMAL_PRIORITY)
						.addClassAndMethod(this)
						.addSourceLine(this, appendPC));
				}
				state = SEEN_NOTHING;				
			}
			else if ((seen == INVOKEVIRTUAL) 
			&&       "append".equals(getNameConstantOperand())
			&&       "(Ljava/lang/String;)Ljava/lang/StringBuffer;".equals(getSigConstantOperand()))
				appendPC = getPC();
			else if ((seen == NEW) && "java/lang/StringBuffer".equals(getClassConstantOperand()))
				state = SEEN_NEW;
				
			break;
					
		default:
			appendPC = -1;
			state = SEEN_NOTHING;
			break;
		}
	}
}

// vim:ts=4
