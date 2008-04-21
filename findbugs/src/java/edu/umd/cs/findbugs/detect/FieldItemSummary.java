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

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.NonReportingDetector;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.FieldSummary;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class FieldItemSummary extends OpcodeStackDetector implements NonReportingDetector {
	
	FieldSummary fieldSummary = new FieldSummary();
	public FieldItemSummary(BugReporter bugReporter) {
		AnalysisContext context = AnalysisContext.currentAnalysisContext();
		context.setFieldSummary(fieldSummary);
	}

	Set<XField> touched = new HashSet<XField>();

	
	
		@Override
	public void sawOpcode(int seen) {
		if (seen == PUTFIELD || seen == PUTSTATIC) {
			XField fieldOperand = getXFieldOperand();
			if (fieldOperand == null) return;
			touched.add(fieldOperand);
			if (!fieldOperand.getClassDescriptor().getClassName().equals(getClassName()))
				fieldSummary.addWrittenOutsideOfConstructor(fieldOperand);
			else if (seen == PUTFIELD) {
				OpcodeStack.Item addr = stack.getStackItem(1); {
				if (addr.getRegisterNumber() != 0 || !getMethodName().equals("<init>"))
					fieldSummary.addWrittenOutsideOfConstructor(fieldOperand);
				}
			} else if (seen == PUTSTATIC && !getMethodName().equals("<clinit>"))
				fieldSummary.addWrittenOutsideOfConstructor(fieldOperand);
			OpcodeStack.Item top = stack.getStackItem(0);
			fieldSummary.mergeSummary(fieldOperand, top);
		}

	}
		
		 
		@Override
        public void visit(Code obj) {
			super.visit(obj);
			fieldSummary.setFieldsWritten(getXMethod(), touched);
			touched.clear();
		}
		
		
		@Override
        public void report() {
			fieldSummary.setComplete(true);
		}

}
