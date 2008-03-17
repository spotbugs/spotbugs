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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class FieldItemSummary extends OpcodeStackDetector implements NonReportingDetector {

	public FieldItemSummary(BugReporter bugReporter) {
		AnalysisContext context = AnalysisContext.currentAnalysisContext();
		context.setFieldItemSummary(this);
	}

	Map<XField, OpcodeStack.Item> summary = new HashMap<XField, OpcodeStack.Item>();

	Set<XField> writtenOutsideOfConstructor = new HashSet<XField>();

	OpcodeStack.Item getSummary(XField field) {
		OpcodeStack.Item result = summary.get(field);
		if (result == null)
			return new OpcodeStack.Item();
		return result;

	}

	boolean isWrittenOutsideOfConstructor(XField field) {
		if (field.isFinal())
			return false;
		return writtenOutsideOfConstructor.contains(field);
	}

	@Override
	public void sawOpcode(int seen) {
		if (seen == PUTFIELD || seen == PUTSTATIC) {
			XField fieldOperand = getXFieldOperand();
			if (fieldOperand == null) return;
			if (!fieldOperand.getClassDescriptor().getClassName().equals(getClassName()))
				writtenOutsideOfConstructor.add(fieldOperand);
			else if (seen == PUTFIELD) {
				OpcodeStack.Item addr = stack.getStackItem(1);
				if (addr.getRegisterNumber() != 0 || !getMethodName().equals("<init>"))
					writtenOutsideOfConstructor.add(fieldOperand);
			} else if (seen == PUTSTATIC && !getMethodName().equals("<clinit>"))
				writtenOutsideOfConstructor.add(fieldOperand);
			OpcodeStack.Item top = stack.getStackItem(0);
			OpcodeStack.Item oldSummary = summary.get(fieldOperand);
			if (oldSummary != null) {
				summary.put(fieldOperand, OpcodeStack.Item.merge(top, oldSummary));
			} else
				summary.put(fieldOperand, top);
		}

	}

}
