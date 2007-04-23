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

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.ba.ClassContext;

public class BadAppletConstructor extends BytecodeScanningDetector  {
	private BugReporter bugReporter;
	private final JavaClass appletClass;
	private boolean inConstructor;

	public BadAppletConstructor(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		JavaClass appletClass = null;
		try {
			appletClass = Repository.lookupClass("java.applet.Applet");
		} catch (ClassNotFoundException cnfe) {
			bugReporter.reportMissingClass(cnfe);
		}
		this.appletClass = appletClass;
	}



	@Override
		 public void visitClassContext(ClassContext classContext) {
		if (appletClass == null)
			return;

		JavaClass cls = classContext.getJavaClass();
		try {
			if (cls.instanceOf(appletClass))
				cls.accept(this);
		} catch (ClassNotFoundException cnfe) {
			bugReporter.reportMissingClass(cnfe);
		}
	}

	@Override
		 public void visit(Method obj) {
		inConstructor = obj.getName().equals("<init>");
	}

	@Override
		 public void visit(Code obj) {
		if (inConstructor)
			super.visit(obj);
	}

	@Override
		 public void sawOpcode(int seen) {
		if (seen == INVOKEVIRTUAL) {
			String method = getNameConstantOperand();
			String signature = getSigConstantOperand();
			if (((method.equals("getDocumentBase") || method.equals("getCodeBase")) && signature.equals("()Ljava/net/URL;"))
			||  (method.equals("getAppletContext") &&  signature.equals("()Ljava/applet/AppletContext;"))
			||  (method.equals("getParameter") && signature.equals("(Ljava/lang/String;)Ljava/lang/String;")))
				bugReporter.reportBug(new BugInstance(this, "BAC_BAD_APPLET_CONSTRUCTOR", NORMAL_PRIORITY)
					.addClassAndMethod(this)
					.addSourceLine(this));
		}
	}
}

// vim:ts=4
