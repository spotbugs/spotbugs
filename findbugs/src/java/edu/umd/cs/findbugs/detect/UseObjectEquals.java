/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004 Dave Brosius <dbrosius@users.sourceforge.net>
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

import java.util.*;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.FieldAnnotation;
import edu.umd.cs.findbugs.visitclass.Constants2;
import org.apache.bcel.classfile.*;
import org.apache.bcel.generic.*;
import org.apache.bcel.Repository;

public class UseObjectEquals extends BytecodeScanningDetector implements Constants2 {
	private BugReporter bugReporter;
	private OpcodeStack stack;

	public UseObjectEquals(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	public void visit(Method obj) {
		super.visit(obj);
		stack = new OpcodeStack();
	}
	
	public void sawOpcode(int seen) {					
		if ((seen == INVOKEVIRTUAL) 
		&&   getNameConstantOperand().equals("equals")
		&&   getSigConstantOperand().equals("(Ljava/lang/Object;)Z")) {
			
			if (stack.getStackDepth() > 1) {
				OpcodeStack.Item item = stack.getStackItem(1);
				
				if (item.isArray()) {
					bugReporter.reportBug(new BugInstance("UOE_BAD_ARRAY_COMPARE", NORMAL_PRIORITY)
			        		.addClassAndMethod(this)
			        		.addSourceLine(this));
				} else {
/*
					try {
						JavaClass cls = item.getJavaClass();

						if ((cls != null) && cls.isFinal()) {
							if (item.getSignature().equals("Ljava/lang/Class;"))
								return;
							String methodClassName = getClassConstantOperand();
							if (methodClassName.equals("java/lang/Object")) {
								bugReporter.reportBug(new BugInstance("UOE_USE_OBJECT_EQUALS", LOW_PRIORITY)
				    	    		.addClassAndMethod(this)
				    	    		.addSourceLine(this));	
				    	    }
				    	}
					} catch (ClassNotFoundException cnfe) {
						//cnfe.printStackTrace();
						bugReporter.reportMissingClass(cnfe);
					}
*/
				}
			}
		}

		stack.sawOpcode(this, seen);
	}
}

// vim:ts=4
