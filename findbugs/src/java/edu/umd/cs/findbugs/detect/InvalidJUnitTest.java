/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2004,2005 Dave Brosius <dbrosius@users.sourceforge.net>
 * Copyright (C) 2005 William Pugh
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
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XClass;
import edu.umd.cs.findbugs.classfile.CheckedAnalysisException;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.Global;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.*;

public class InvalidJUnitTest extends BytecodeScanningDetector {

	private static final int SEEN_NOTHING = 0;

	private static final int SEEN_ALOAD_0 = 1;

	private BugReporter bugReporter;

	private int state;

	public InvalidJUnitTest(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}



	boolean directChildOfTestCase;

	@Override
		 public void visitClassContext(ClassContext classContext) {
		if (!enabled())
			return;

		JavaClass jClass = classContext.getJavaClass();

		try {
			if (!isJunit3TestCase(getXClass())) return;
			if ((jClass.getAccessFlags() & ACC_ABSTRACT) == 0) {
				if (!hasTestMethods(jClass)) {
					bugReporter.reportBug( new BugInstance( this, "IJU_NO_TESTS", LOW_PRIORITY)
							.addClass(jClass));
				}
			}
			directChildOfTestCase = "junit.framework.TestCase".equals(jClass.getSuperclassName());
			jClass.accept(this);
		} catch (ClassNotFoundException cnfe) {
			bugReporter.reportMissingClass(cnfe);
		}

	}

	private boolean isJunit3TestCase(XClass jClass) throws ClassNotFoundException {
		ClassDescriptor  sDesc = jClass.getSuperclassDescriptor();
		if (sDesc == null) return false;
		String sName = sDesc.getClassName();
		if (sName.equals("junit/framework/TestCase")) return true;
		if (sName.equals("java/lang/Object")) return false;


		XClass sClass;
        try {
	        sClass = Global.getAnalysisCache().getClassAnalysis(XClass.class, sDesc);
        } catch (CheckedAnalysisException e) {
	      return false;
        }
		return isJunit3TestCase(sClass);

	}
	private boolean hasTestMethods(JavaClass jClass) {
		boolean foundTest = false;
		Method[] methods = jClass.getMethods();
		for (Method m : methods) {
			if (m.isPublic() && m.getName().startsWith("test") && m.getSignature().equals("()V")) 
				return true;
			if (m.getName().startsWith("runTest") && m.getSignature().endsWith("()V")) 
				return true;
		}
		if (hasSuite(methods)) return true;

		try {
			JavaClass sClass = jClass.getSuperClass();
			if (sClass != null) return hasTestMethods(sClass);
		} catch (ClassNotFoundException e) {
			AnalysisContext.reportMissingClass(e);
		}

		return false;
	}
	/** is there a JUnit3TestSuite */
	private boolean hasSuite(Method[] methods) {
		for (Method m : methods) {
			if (m.getName().equals("suite")
				&& m.isPublic()
				&& m.isStatic()
			  //&& m.getReturnType().equals(junit.framework.Test.class)
			  //&& m.getArgumentTypes().length == 0
				&& m.getSignature().equals("()Ljunit/framework/Test;"))
			  return true;
		}
		return false;
	}




	/**
	 * Check whether or not this detector should be enabled.
	 * The detector is disabled if the TestCase class cannot be found
	 * (meaning we don't have junit.jar on the aux classpath).
	 * 
	 * @return true if it should be enabled, false if not
	 */
	private boolean enabled() {
		return true;
		}

	@Override
	public void visit(Method obj) {
		if (getMethodName().equals("suite") && !obj.isStatic())
			bugReporter.reportBug(new BugInstance(this, "IJU_SUITE_NOT_STATIC",
					NORMAL_PRIORITY).addClassAndMethod(this));

		if (getMethodName().equals("suite") && obj.getSignature().startsWith("()") && obj.isStatic())  {
			if ((!obj.getSignature().equals("()Ljunit/framework/Test;") 
					&& !obj.getSignature().equals("()Ljunit/framework/TestSuite;"))
					|| !obj.isPublic())
				bugReporter.reportBug( new BugInstance( this, "IJU_BAD_SUITE_METHOD", NORMAL_PRIORITY)
				.addClassAndMethod(this));

		}



	}

	private boolean sawSuperCall;

	@Override
		 public void visit(Code obj) {
		if (!directChildOfTestCase
				&& (getMethodName().equals("setUp") || getMethodName().equals(
						"tearDown"))
				&& !getMethod().isPrivate()) {
			sawSuperCall = false;
			super.visit(obj);
			if (sawSuperCall)
				return;
			JavaClass we = Lookup.findSuperImplementor(getThisClass(),
					getMethodName(), "()V", bugReporter);
			if (we != null && !we.getClassName().equals("junit.framework.TestCase")) {
				// OK, got a bug
				bugReporter.reportBug(new BugInstance(this, getMethodName()
						.equals("setUp") ? "IJU_SETUP_NO_SUPER"
						: "IJU_TEARDOWN_NO_SUPER", NORMAL_PRIORITY)
						.addClassAndMethod(this));
			}
		}
	}

	@Override
		 public void sawOpcode(int seen) {
		switch (state) {
		case SEEN_NOTHING:
			if (seen == ALOAD_0)
				state = SEEN_ALOAD_0;
			break;

		case SEEN_ALOAD_0:
			if ((seen == INVOKESPECIAL)
					&& (getNameConstantOperand().equals(getMethodName()))
					&& (getMethodSig().equals("()V")))
				sawSuperCall = true;
			state = SEEN_NOTHING;
			break;
		default:
			state = SEEN_NOTHING;
		}
	}
}

// vim:ts=4
