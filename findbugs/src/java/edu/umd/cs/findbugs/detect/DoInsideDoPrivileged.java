/*
 * FindBugs - Find Bugs in Java programs
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

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;

/**
 * @author pugh
 */
public class DoInsideDoPrivileged  extends BytecodeScanningDetector {
	BugReporter bugReporter;
	public DoInsideDoPrivileged(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	boolean isDoPrivileged = false;
	@Override
	public void visit(JavaClass obj) {
		try {
			isDoPrivileged =
				Repository.implementationOf(getClassName(),"java/security/PrivilegedAction")
				|| Repository.implementationOf(getClassName(),"java/security/PrivilegedExceptionAction");
		} catch (ClassNotFoundException e) {
			isDoPrivileged = true;
		}
	}

	@Override
	public void visit(Code obj) {
		if (isDoPrivileged && getMethodName().equals("run")) return;
		if (getMethod().isPrivate()) return;
		if (DumbMethods.isTestMethod(getMethod())) return;
		super.visit(obj);
	}
	@Override
	public void sawOpcode(int seen) {
		try {
		if (seen == INVOKEVIRTUAL && getNameConstantOperand().equals("setAccessible")) {
			String className = getDottedClassConstantOperand();
			if (className.equals("java.lang.reflect.Field") || className.equals("java.lang.reflect.Method"))
				bugReporter.reportBug(new BugInstance(this, "DP_DO_INSIDE_DO_PRIVILEGED",
						LOW_PRIORITY)
							.addClassAndMethod(this)
							.addCalledMethod(this)
							.addSourceLine(this)
							);
		}
		if (seen == NEW) {
			String classOfConstructedClass = getClassConstantOperand();
			JavaClass constructedClass = Repository.lookupClass(classOfConstructedClass);
			if (Repository.instanceOf(constructedClass,"java/lang/ClassLoader") 
					&& !(getMethodName().equals("main") && getMethodSig().equals("([Ljava/lang/String;)V") && getMethod().isStatic()) )
				bugReporter.reportBug(new BugInstance(this, "DP_CREATE_CLASSLOADER_INSIDE_DO_PRIVILEGED",
					NORMAL_PRIORITY)
						.addClassAndMethod(this)
						.addClass(constructedClass)
						.addSourceLine(this)
						);
		}
		} catch (ClassNotFoundException e) {
			// ignore this
		}

	}

}
