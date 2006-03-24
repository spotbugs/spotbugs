/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2005, University of Maryland
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

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;

public class ConfusionBetweenInheritedAndOuterMethod extends BytecodeScanningDetector {


	BugReporter bugReporter;
	public ConfusionBetweenInheritedAndOuterMethod(BugReporter bugReporter) {
		this.bugReporter =  bugReporter;
	}


	@Override
         public void visitJavaClass(JavaClass obj) {
		// totally skip methods not defined in inner classes
		if (obj.getClassName().indexOf('$') >= 0) super.visitJavaClass(obj);
		
	}

	OpcodeStack stack = new OpcodeStack();
	@Override
         public void visit(Code obj) {
		stack.resetForMethodEntry(this);
		super.visit(obj);
	}

	private static String stripLastDollar(String s) {
		int i = s.lastIndexOf('$');
		if (i == -1) throw new IllegalArgumentException();
		return s.substring(0,i);
	}
	@Override
         public void sawOpcode(int seen) {
		stack.mergeJumps(this);
		try {
         if (seen != INVOKEVIRTUAL) return;
         if (!getClassName().equals(getClassConstantOperand())) return;
         XMethod invokedMethod = XFactory.createXMethod(getClassConstantOperand(), getNameConstantOperand(), getSigConstantOperand(), false);
         if (Methods.getMethods().contains(invokedMethod)) {

        	 return;
         }
         String possibleTargetClass = getClassName();
         while(true) {
        	 int i = possibleTargetClass.lastIndexOf('$');
			if (i == -1) break;
			possibleTargetClass = possibleTargetClass.substring(0,i);
        	 XMethod alternativeMethod = XFactory.createXMethod(possibleTargetClass, getNameConstantOperand(), getSigConstantOperand(), false);
        	 Set<XMethod> definedMethods = Methods.getMethods();
        	 if (definedMethods.contains(alternativeMethod)) 	bugReporter.reportBug(new BugInstance(this, "IA_AMBIGUOUS_INVOCATION_OF_INHERITED_OR_OUTER_METHOD", NORMAL_PRIORITY)
				        .addClassAndMethod(this)
				          .addMethod(invokedMethod)
				        .addMethod(alternativeMethod)
				        .addSourceLine(this, getPC()));
         }
         
         
		} finally {
		stack.sawOpcode(this, seen);
		}
	}

}
