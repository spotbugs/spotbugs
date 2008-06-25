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

import javax.annotation.OverridingMethodsMustInvokeSuper;

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.Lookup;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.ClassDescriptor;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;
import edu.umd.cs.findbugs.classfile.analysis.AnnotationValue;

public class CbeckMustOverrideSuperAnnotation extends OpcodeStackDetector {

	BugReporter bugReporter;

	ClassDescriptor mustOverrideAnnotation = DescriptorFactory.createClassDescriptor(OverridingMethodsMustInvokeSuper.class);

	public CbeckMustOverrideSuperAnnotation(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	private boolean sawCallToSuper;

	@Override
	public void visit(Code code) {
		if (getMethod().isStatic() || getMethod().isPrivate())
			return;
		XMethod overrides = Lookup.findSuperImplementorAsXMethod(getThisClass(), getMethodName(), getMethodSig(), bugReporter);

		if (overrides == null)
			return;
		AnnotationValue annotation = overrides.getAnnotation(mustOverrideAnnotation);
		if (annotation == null)
			return;
		sawCallToSuper = false;
		super.visit(code);
		if (!sawCallToSuper)
			bugReporter.reportBug(new BugInstance(this, "TESTING", NORMAL_PRIORITY).addClassAndMethod(this));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.umd.cs.findbugs.bcel.OpcodeStackDetector#sawOpcode(int)
	 */
	@Override
	public void sawOpcode(int seen) {
		if (seen != INVOKESPECIAL)
			return;

		String calledClassName = getClassConstantOperand();
		String calledMethodName = getNameConstantOperand();
		String calledMethodSig = getSigConstantOperand();
		if (calledClassName.equals(getSuperclassName()) && calledMethodName.equals(getMethodName())
		        && calledMethodSig.equals(getMethodSig())) {
			sawCallToSuper = true;
		}

	}

}
