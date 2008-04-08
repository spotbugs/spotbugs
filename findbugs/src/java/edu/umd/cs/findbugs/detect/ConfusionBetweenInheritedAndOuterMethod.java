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

import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.ba.XFactory;
import edu.umd.cs.findbugs.ba.XMethod;

public class ConfusionBetweenInheritedAndOuterMethod extends BytecodeScanningDetector {


	BugReporter bugReporter;
	public ConfusionBetweenInheritedAndOuterMethod(BugReporter bugReporter) {
		this.bugReporter =  bugReporter;
	}


	@Override
		 public void visitJavaClass(JavaClass obj) {
		hasThisDollarZero = false;
		// totally skip methods not defined in inner classes
		if (obj.getClassName().indexOf('$') >= 0) super.visitJavaClass(obj);

	}

	boolean hasThisDollarZero;

	@Override
	public void visit(Field f) {
		if (f.getName().equals("this$0")) hasThisDollarZero = true;
	}
	@Override
		 public void visit(Code obj) {
		if (hasThisDollarZero) {
		super.visit(obj);
		}
	}

	private static String stripLastDollar(String s) {
		int i = s.lastIndexOf('$');
		if (i == -1) throw new IllegalArgumentException();
		return s.substring(0,i);
	}
	@Override
		 public void sawOpcode(int seen) {
		 if (seen != INVOKEVIRTUAL) return;
		 if (!getClassName().equals(getClassConstantOperand())) return;
		 XMethod invokedMethod = XFactory.createXMethod(getDottedClassConstantOperand(), getNameConstantOperand(), getSigConstantOperand(), false);
		 if (invokedMethod.isResolved() && invokedMethod.getClassName().equals(getDottedClassConstantOperand())) {
			 // method is not inherited
			 return;
		 }
		 // method is inherited
		 String possibleTargetClass = getDottedClassName();
		 String superClassName = getDottedSuperclassName();
		 while(true) {
			 int i = possibleTargetClass.lastIndexOf('$');
			if (i <= 0) break;
			possibleTargetClass = possibleTargetClass.substring(0,i);
			if (possibleTargetClass.equals(superClassName)) break;
			 XMethod alternativeMethod = XFactory.createXMethod(possibleTargetClass, getNameConstantOperand(), getSigConstantOperand(), false);
			 if (alternativeMethod.isResolved() && alternativeMethod.getClassName().equals(possibleTargetClass)) 	{
				 String targetPackage = invokedMethod.getPackageName();
				 String alternativePackage = alternativeMethod.getPackageName();
				 int priority = HIGH_PRIORITY;
				 if (targetPackage.equals(alternativePackage)) priority++;
				 if (targetPackage.startsWith("javax.swing") || targetPackage.startsWith("java.awt"))
					 priority+=2;
				 if (invokedMethod.getName().equals(getMethodName())) priority++;

				 bugReporter.reportBug(new BugInstance(this, "IA_AMBIGUOUS_INVOCATION_OF_INHERITED_OR_OUTER_METHOD", priority)
						.addClassAndMethod(this)
						  .addMethod(invokedMethod).describe("METHOD_INHERITED")
						.addMethod(alternativeMethod).describe("METHOD_ALTERNATIVE_TARGET")
						.addSourceLine(this, getPC()));
				 break;
			 }
		 }


		
	}

}
