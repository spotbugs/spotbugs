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
import java.util.Set;

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugAccumulator;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.FieldSummary;
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
	
	Set<XField> initializedFields;

	public void visit(Code obj) {
		if (getMethod().isStatic())
			return;
		initializedFields = new HashSet<XField>();
		super.visit(obj);
		accumulator.reportAccumulatedBugs();
	}

	@Override
	public void sawOpcode(int opcode) {
		if (opcode == PUTFIELD) {
			XField f = getXFieldOperand();
			OpcodeStack.Item item = stack.getStackItem(1);
			if (item.getRegisterNumber() != 0)
				return;
			initializedFields.add(f);
			return;
		}
		if (opcode != GETFIELD)
			return;
		OpcodeStack.Item item = stack.getStackItem(0);
		if (item.getRegisterNumber() != 0)
			return;
		XField f = getXFieldOperand();
		
		if (f == null || !f.getClassDescriptor().equals(getClassDescriptor()))
			return;
		if (f.isSynthetic() || f.getName().startsWith("this$"))
			return;
		if (initializedFields.contains(f))
			return;
		FieldSummary fieldSummary = AnalysisContext.currentAnalysisContext().getFieldSummary();

		Set<XMethod> calledFrom = fieldSummary.getCalledFromSuperConstructor(DescriptorFactory
		        .createClassDescriptor(getSuperclassName()), getXMethod());
		if (calledFrom.isEmpty())
			return;
		UnreadFields unreadFields = AnalysisContext.currentAnalysisContext().getUnreadFields();
		
		int priority;
		if (!unreadFields.isWrittenInConstructor(f))
			return;
		
		if (f.isFinal() || !unreadFields.isWrittenOutsideOfConstructor(f))
			priority = HIGH_PRIORITY;
		else {
			priority = NORMAL_PRIORITY;
		}
		int nextOpcode = getNextOpcode();
		if (nextOpcode == IFNULL || nextOpcode == IFNONNULL)
			priority++;
		BugInstance bug = new BugInstance(this, "UR_UNINIT_READ_CALLED_FROM_SUPER_CONSTRUCTOR", priority).addClassAndMethod(this).addField(f);
		
		for (XMethod m : calledFrom) 
			bug.addMethod(m).describe(MethodAnnotation.METHOD_CALLED_FROM);

		accumulator.accumulateBug(bug, this);
		

	}

}
