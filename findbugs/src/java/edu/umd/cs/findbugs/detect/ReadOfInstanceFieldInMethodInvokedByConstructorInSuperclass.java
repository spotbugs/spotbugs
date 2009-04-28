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

import java.util.Set;

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.FieldSummary;
import edu.umd.cs.findbugs.ba.Hierarchy2;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;
import edu.umd.cs.findbugs.classfile.DescriptorFactory;

public class ReadOfInstanceFieldInMethodInvokedByConstructorInSuperclass extends OpcodeStackDetector {

	BugReporter bugReporter;

	BugAccumulator accumulator;

	public ReadOfInstanceFieldInMethodInvokedByConstructorInSuperclass(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		this.accumulator = new BugAccumulator(bugReporter);
	}

	public void visit(Code obj) {
		if (getMethod().isStatic())
			return;
		super.visit(obj);
		accumulator.reportAccumulatedBugs();
	}

	@Override
	public void sawOpcode(int opcode) {
		if (opcode != GETFIELD)
			return;
		OpcodeStack.Item item = stack.getStackItem(0);
		if (item.getRegisterNumber() != 0)
			return;
		XField f = getXFieldOperand();
		if (!f.getClassDescriptor().equals(getClassDescriptor()))
			return;
		if (f.isSynthetic() || f.getName().startsWith("this$"))
			return;
		FieldSummary fieldSummary = AnalysisContext.currentAnalysisContext().getFieldSummary();

		Set<XMethod> calledFrom = fieldSummary.getCalledFromSuperConstructor(DescriptorFactory
		        .createClassDescriptor(getSuperclassName()), getXMethod());
		if (calledFrom.isEmpty())
			return;

		for (XMethod m : calledFrom) {

			BugInstance bug = new BugInstance(this, "TESTING", NORMAL_PRIORITY).addClassAndMethod(this).addMethod(m).addField(f);

			accumulator.accumulateBug(bug, this);
		}

	}

}
