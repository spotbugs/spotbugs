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

import java.util.HashMap;
import java.util.Map;

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class CrossSiteScripting extends OpcodeStackDetector {

	final BugAccumulator accumulator;

	public CrossSiteScripting(BugReporter bugReporter) {
		accumulator = new BugAccumulator(bugReporter);
	}

	Map<String, OpcodeStack.Item> map = new HashMap<String, OpcodeStack.Item>();

	OpcodeStack.Item top = null;

	@Override
	public void visit(Code code) {
		super.visit(code);
		map.clear();
		accumulator.reportAccumulatedBugs();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.umd.cs.findbugs.bcel.OpcodeStackDetector#sawOpcode(int)
	 */
	@Override
	public void sawOpcode(int seen) {
		if (seen == INVOKEINTERFACE) {
			String calledClassName = getClassConstantOperand();
			String calledMethodName = getNameConstantOperand();
			if (calledClassName.equals("javax/servlet/http/HttpSession") && calledMethodName.equals("setAttribute")) {
				OpcodeStack.Item value = stack.getStackItem(0);
				OpcodeStack.Item name = stack.getStackItem(1);
				Object nameConstant = name.getConstant();
				if (nameConstant instanceof String)
					map.put((String) nameConstant, value);
			} else if (calledClassName.equals("javax/servlet/http/HttpSession") && calledMethodName.equals("getAttribute")) {
				OpcodeStack.Item name = stack.getStackItem(0);
				Object nameConstant = name.getConstant();
				if (nameConstant instanceof String) {
					top = map.get((String) nameConstant);
					if (isTainted(top)) 
						stack.replaceTop(top);
				}
			} else 
				top = null;
		} else if (seen == INVOKEVIRTUAL) {
			String calledClassName = getClassConstantOperand();
			String calledMethodName = getNameConstantOperand();
			String calledMethodSig = getSigConstantOperand();
			// System.out.println(calledClassName + "." + calledMethodName);
			if (calledMethodName.startsWith("print") && calledClassName.equals("javax/servlet/jsp/JspWriter")
			        && (calledMethodSig.equals("(Ljava/lang/Object;)V") || calledMethodSig.equals("(Ljava/lang/String;)V"))) {
				OpcodeStack.Item writing = stack.getStackItem(0);
				if (isTainted(writing)) 
					accumulator.accumulateBug(new BugInstance(this, "XSS_REQUEST_PARAMETER_TO_JSP_WRITER",
					        Priorities.HIGH_PRIORITY).addClassAndMethod(this), this);
					
				else if (isTainted(top))
					accumulator.accumulateBug(new BugInstance(this, "XSS_REQUEST_PARAMETER_TO_JSP_WRITER",
					        Priorities.NORMAL_PRIORITY).addClassAndMethod(this), this);
			} else if (calledMethodName.startsWith("print") && calledClassName.equals("java/io/PrintWriter")
			        && (calledMethodSig.equals("(Ljava/lang/Object;)V") || calledMethodSig.equals("(Ljava/lang/String;)V"))) {
				OpcodeStack.Item writing = stack.getStackItem(0);
				OpcodeStack.Item writingTo = stack.getStackItem(1);
				if (isTainted(writing) && isServletWriter(writingTo)) 
					accumulator.accumulateBug(new BugInstance(this, "XSS_REQUEST_PARAMETER_TO_SERVLET_WRITER",
					        Priorities.HIGH_PRIORITY).addClassAndMethod(this), this);
				else if (isTainted(top) && isServletWriter(writingTo)) 
					accumulator.accumulateBug(new BugInstance(this, "XSS_REQUEST_PARAMETER_TO_SERVLET_WRITER",
					        Priorities.NORMAL_PRIORITY).addClassAndMethod(this), this);
	
			}
			top = null;
		} else
			top = null;
	}

	private boolean isTainted(OpcodeStack.Item writing) {
		if (writing == null) return false;
		return writing.isServletParameterTainted();
	}

	private boolean isServletWriter(OpcodeStack.Item writingTo) {
		XMethod writingToSource = writingTo.getReturnValueOf();

		return writingToSource != null && writingToSource.getClassName().equals("javax.servlet.http.HttpServletResponse")
		        && writingToSource.getName().equals("getWriter");
	}

}
