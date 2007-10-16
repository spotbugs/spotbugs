/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
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

import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.classfile.CodeException;
import org.apache.bcel.classfile.ExceptionTable;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Detector;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.visitclass.PreorderVisitor;

public class DontCatchIllegalMonitorStateException
		extends PreorderVisitor implements Detector {

	private static final boolean DEBUG = SystemProperties.getBoolean("dcimse.debug");

	BugReporter bugReporter;
	Set<String> msgs = null;
	ClassContext classContext;

	public DontCatchIllegalMonitorStateException(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		if (DEBUG)
			msgs = new HashSet<String>();
	}



	@Override
		 public void visit(ExceptionTable obj) {
		if (DEBUG) {
			String names[] = obj.getExceptionNames();
			for (String name : names)
				if (name.equals("java.lang.Exception")
						|| name.equals("java.lang.Throwable"))
					System.out.println(name + " thrown by " + getFullyQualifiedMethodName());
		}
	}

	@Override
		 public void visit(CodeException obj) {
		int type = obj.getCatchType();
		if (type == 0) return;
		String name = getConstantPool().constantToString(getConstantPool().getConstant(type));
		if (DEBUG) {
			String msg = "Catching " + name + " in " + getFullyQualifiedMethodName();
			if (msgs.add(msg))
				System.out.println(msg);
		}
		if (name.equals("java.lang.IllegalMonitorStateException"))
			bugReporter.reportBug(new BugInstance(this, "IMSE_DONT_CATCH_IMSE", HIGH_PRIORITY)
					.addClassAndMethod(this)
					.addSourceLine(this.classContext, this, obj.getHandlerPC()));

	}

	public void visitClassContext(ClassContext classContext) {
		this.classContext = classContext;
		classContext.getJavaClass().accept(this);
	}

	public void report() {
	}
}
