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


import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import org.apache.bcel.classfile.*;

public class UseObjectEquals extends BytecodeScanningDetector implements StatelessDetector {
	private BugReporter bugReporter;
	private OpcodeStack stack = new OpcodeStack();

	public UseObjectEquals(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}



	@Override
         public void visit(Method obj) {
		super.visit(obj);
        stack.resetForMethodEntry(this);
	}
		
	@Override
         public void sawOpcode(int seen) {		
		stack.mergeJumps(this);
		if ((seen == INVOKEVIRTUAL) 
		&&   getNameConstantOperand().equals("equals")
		&&   getSigConstantOperand().equals("(Ljava/lang/Object;)Z")) {
			
			if (stack.getStackDepth() > 1) {
				OpcodeStack.Item item1 = stack.getStackItem(1);
				
					try {
						JavaClass cls = item1.getJavaClass();

						if ((cls != null) && cls.isFinal()) {
							if (item1.getSignature().equals("Ljava/lang/Class;"))
								return;							
							String methodClassName = getClassConstantOperand();
							if (methodClassName.equals("java/lang/Object")) {
								if (!AnalysisContext.currentAnalysisContext().isApplicationClass(cls))
									return;

								bugReporter.reportBug(new BugInstance("UOE_USE_OBJECT_EQUALS", LOW_PRIORITY)
				    	    		.addClassAndMethod(this)
				    	    		.addSourceLine(this));	
				    	    }
				    	}
					} catch (ClassNotFoundException cnfe) {
						//cnfe.printStackTrace();
						bugReporter.reportMissingClass(cnfe);
					}
				}
		}

		stack.sawOpcode(this, seen);
	}
}

// vim:ts=4
