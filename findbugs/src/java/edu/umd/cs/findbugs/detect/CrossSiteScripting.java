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

import java.util.HashSet;

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class CrossSiteScripting extends OpcodeStackDetector {

	BugReporter bugReporter;
	BugAccumulator accumulator;
	
	public CrossSiteScripting(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		accumulator = new BugAccumulator(bugReporter);
	}

	@Override
	public void visit(Code code) {
		super.visit(code);
		accumulator.reportAccumulatedBugs();
		}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.umd.cs.findbugs.bcel.OpcodeStackDetector#sawOpcode(int)
	 */
	@Override
	public void sawOpcode(int seen) {
		if (seen != INVOKEVIRTUAL) {
			return;
		}
		String calledClassName = getClassConstantOperand();
		String calledMethodName = getNameConstantOperand();
		String calledMethodSig = getSigConstantOperand();
		// System.out.println(calledClassName + "." + calledMethodName);
		if (calledMethodName.startsWith("print") && calledClassName.equals("javax/servlet/jsp/JspWriter")
		        && (calledMethodSig.equals("(Ljava/lang/Object;)V") || calledMethodSig.equals("(Ljava/lang/String;)V"))) {
			OpcodeStack.Item writing = stack.getStackItem(0);
			XMethod method = writing.getReturnValueOf();
			if (method != null) {
				if (method.getName().equals("getParameter")
				        && method.getClassName().equals("javax.servlet.http.HttpServletRequest"))
					accumulator.accumulateBug(new BugInstance(this, "XSS_REQUEST_PARAMETER_TO_JSP_WRITER", Priorities.HIGH_PRIORITY)
			        .addClassAndMethod(this), this);
			}
		} else if (calledMethodName.startsWith("print") && calledClassName.equals("java/io/PrintWriter")
		        && (calledMethodSig.equals("(Ljava/lang/Object;)V") || calledMethodSig.equals("(Ljava/lang/String;)V"))) {
			OpcodeStack.Item writing = stack.getStackItem(0);
			OpcodeStack.Item writingTo = stack.getStackItem(1);
			XMethod writingSource = writing.getReturnValueOf();
			XMethod writingToSource = writingTo.getReturnValueOf();
			if (writingSource != null && writingToSource != null)
				if (writingSource.getName().equals("getParameter")
				        && writingSource.getClassName().equals("javax.servlet.http.HttpServletRequest"))
					if (writingToSource.getClassName().equals("javax.servlet.http.HttpServletResponse")
					        && writingToSource.getName().equals("getWriter")) {
						accumulator.accumulateBug(new BugInstance(this, "XSS_REQUEST_PARAMETER_TO_SERVLET_WRITER", Priorities.HIGH_PRIORITY)
				        .addClassAndMethod(this), this);
					}
		}
	}

	

}
