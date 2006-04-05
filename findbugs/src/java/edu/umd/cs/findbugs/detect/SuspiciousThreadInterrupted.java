/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004-2005 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2004-2005 University of Maryland
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
import java.util.BitSet;
import org.apache.bcel.classfile.Method;

/**
 * looks for calls to Thread.interrupted from a non static context, especially when that context is
 * not 'currentThread()'.
 */
public class SuspiciousThreadInterrupted extends BytecodeScanningDetector implements  StatelessDetector {
	public static final int SEEN_NOTHING = 0;
	public static final int SEEN_CURRENTTHREAD = 1;
	public static final int SEEN_POP_AFTER_CURRENTTHREAD = 2;
	public static final int SEEN_UNKNOWNCONTEXT_POP = 3;
	
	private BugReporter bugReporter;
	private BitSet localsWithCurrentThreadValue;
	private int state;
	
	public SuspiciousThreadInterrupted(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	


	@Override
         public void visit(Method obj) {
		localsWithCurrentThreadValue = new BitSet();
		state = SEEN_NOTHING;
		super.visit(obj);
	}

	@Override
         public void sawOpcode(int seen) {
		switch (state) {
			case SEEN_NOTHING:
				if ((seen == INVOKESTATIC)
		        && getClassConstantOperand().equals("java/lang/Thread")
		        && getNameConstantOperand().equals("currentThread")
		        && getSigConstantOperand().equals("()Ljava/lang/Thread;"))
		        	state = SEEN_CURRENTTHREAD;
		        else if (seen == ALOAD) {
		        	if (localsWithCurrentThreadValue.get(getRegisterOperand())) {
		        		state = SEEN_CURRENTTHREAD;
		        	}
		        } else if ((seen >= ALOAD_0) && (seen <= ALOAD_3)) {
		        	if (localsWithCurrentThreadValue.get(seen - ALOAD_0)) {
		        		state = SEEN_CURRENTTHREAD;
		        	}
		        } else if (seen == POP) {
		        	state = SEEN_UNKNOWNCONTEXT_POP;
		        }
		    break;
		    
		    case SEEN_CURRENTTHREAD:
		    	if (seen == POP) {
		    		state = SEEN_POP_AFTER_CURRENTTHREAD;
		    	}
		    	else if (seen == ASTORE) {
		    		localsWithCurrentThreadValue.set(getRegisterOperand());
		    		state = SEEN_NOTHING;
		    	} else if ((seen >= ASTORE_0) && (seen <= ASTORE_3)) {
		    		localsWithCurrentThreadValue.set(seen - ASTORE_0);
		    		state = SEEN_NOTHING;
		    	} else {
		    		state = SEEN_NOTHING;
		    	}
		    break;
		    
		    default:
				if ((seen == INVOKESTATIC)
		        && getClassConstantOperand().equals("java/lang/Thread")
		        && getNameConstantOperand().equals("interrupted")
		        && getSigConstantOperand().equals("()Z")) {
		        	if (state == SEEN_POP_AFTER_CURRENTTHREAD) {
						bugReporter.reportBug(new BugInstance(this, "STI_INTERRUPTED_ON_CURRENTTHREAD", LOW_PRIORITY)
						        .addClassAndMethod(this)
						        .addSourceLine(this));
		        	} else if (state == SEEN_UNKNOWNCONTEXT_POP) {
						bugReporter.reportBug(new BugInstance(this, "STI_INTERRUPTED_ON_UNKNOWNTHREAD", NORMAL_PRIORITY)
						        .addClassAndMethod(this)
						        .addSourceLine(this));
		        	}
		        }
				state = SEEN_NOTHING;		        	
		    break;
		}
	}
}

// vim:ts=4
