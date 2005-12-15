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

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;

/**
 * @author pugh
 */
public class DoInsideDoPriviledged  extends BytecodeScanningDetector {
	BugReporter bugReporter;
	public DoInsideDoPriviledged(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}
	boolean isDoPriviledged = false;
	@Override
	public void visit(JavaClass obj) {
		try {
			isDoPriviledged = Repository.implementationOf(getClassName(),"java/security/PrivilegedAction");
		} catch (ClassNotFoundException e) {
			isDoPriviledged = true;
		}
	}
	@Override
	public void visit(Code obj) {
		if (isDoPriviledged && getMethodName().equals("run")) return;
		super.visit(obj);
	}
	@Override
	public void sawOpcode(int seen) {
		try {
		if (seen == NEW) {
			String classOfConstructedClass = getClassConstantOperand();
			JavaClass constructedClass = Repository.lookupClass(classOfConstructedClass);
			if (Repository.instanceOf(constructedClass,"java/lang/ClassLoader")) 
				bugReporter.reportBug(new BugInstance(this, "DP_DO_INSIDE_DO_PRIVILEDGED",
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
