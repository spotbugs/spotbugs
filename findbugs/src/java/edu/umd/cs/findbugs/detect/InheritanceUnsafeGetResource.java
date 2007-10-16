/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004-2006 University of Maryland
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


import java.util.Set;

import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.StatelessDetector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ch.Subtypes;

public class InheritanceUnsafeGetResource extends BytecodeScanningDetector implements StatelessDetector {

	private BugReporter bugReporter;
	private boolean classIsFinal;
//	private boolean methodIsVisibleToOtherPackages;
	private boolean classIsVisibleToOtherPackages;
//	private boolean methodIsFinal;
	private boolean methodIsStatic;
	int state = 0;
	int sawGetClass;
	boolean reportedForThisClass;
	String stringConstant;
	int prevOpcode;

	public InheritanceUnsafeGetResource(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}



	@Override
		 public void visit(JavaClass obj) {
		classIsFinal = obj.isFinal();
		reportedForThisClass = false;
		classIsVisibleToOtherPackages = obj.isPublic() || obj.isProtected();
	}

	@Override
		 public void visit(Method obj) {
		methodIsStatic = obj.isStatic();
		state = 0;
		sawGetClass = -100;
	}

	@Override
		 public void sawOpcode(int seen) {
		if (reportedForThisClass) return;


		switch (seen) {
		case LDC:
			Constant constantValue = getConstantRefOperand();
			if (constantValue instanceof ConstantClass) 
				sawGetClass = -100;
			else if (constantValue instanceof ConstantString) {
				stringConstant = ((ConstantString)constantValue).getBytes(getConstantPool());
			}
			break;

		case ALOAD_0:
			state = 1;
			break;
		case INVOKEVIRTUAL:
			if (getClassConstantOperand().equals("java/lang/Class")
					&& (getNameConstantOperand().equals("getResource")
					|| getNameConstantOperand().equals("getResourceAsStream"))
					&& sawGetClass + 10 >= getPC()) {
				 Subtypes subtypes = AnalysisContext.currentAnalysisContext()
					.getSubtypes();
				 int priority = NORMAL_PRIORITY;
				 if (prevOpcode == LDC && stringConstant != null && stringConstant.charAt(0)=='/')
					 priority = LOW_PRIORITY;
				 else {
				 String myPackagename = getThisClass().getPackageName();
				Set<JavaClass> mySubtypes = subtypes.getTransitiveSubtypes(getThisClass());
				if (mySubtypes.isEmpty()) priority++;
				else for(JavaClass c : mySubtypes) {
						if (!c.getPackageName().equals(myPackagename)) {
							priority--;
							break;
						}	
					}
				 }
				bugReporter.reportBug(new BugInstance(this, "UI_INHERITANCE_UNSAFE_GETRESOURCE", 
						priority)
						.addClassAndMethod(this)
						.addSourceLine(this));
				reportedForThisClass = true;

			} else if (state == 1
					&& !methodIsStatic
					&& !classIsFinal
					&& classIsVisibleToOtherPackages
					&& getNameConstantOperand().equals("getClass")
					&& getSigConstantOperand().equals("()Ljava/lang/Class;")) {
				sawGetClass = getPC();
			}
			state = 0;
			break;
		default:
			state = 0;
			break;
		}
		if (seen != LDC) stringConstant = null;
		prevOpcode = seen;

	}

}
