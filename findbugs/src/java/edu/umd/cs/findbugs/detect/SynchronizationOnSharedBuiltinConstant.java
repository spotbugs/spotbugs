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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.OpcodeStack;
import edu.umd.cs.findbugs.StringAnnotation;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.FieldSummary;
import edu.umd.cs.findbugs.ba.XField;
import edu.umd.cs.findbugs.ba.XMethod;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class SynchronizationOnSharedBuiltinConstant extends OpcodeStackDetector {

	BugReporter bugReporter;
	Set<String> badSignatures;
	public SynchronizationOnSharedBuiltinConstant(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
		badSignatures = new HashSet<String>();
		badSignatures.addAll(Arrays.asList(new String[] { "Ljava/lang/Boolean;",
				"Ljava/lang/Double;","Ljava/lang/Float;","Ljava/lang/Byte;","Ljava/lang/Character;",
				"Ljava/lang/Short;","Ljava/lang/Integer;", "Ljava/lang/Long;"}));
	}



	private static boolean newlyConstructedObject(OpcodeStack.Item item) {
		XMethod method = item.getReturnValueOf();
		if (method == null) return false;
		return method.getName().equals("<init>");
	}
	
	private static final Pattern identified = Pattern.compile("\\p{Alnum}+");
	
	@Override
	public void sawOpcode(int seen) {
		if (seen == MONITORENTER) {
			OpcodeStack.Item top = stack.getStackItem(0);
			String signature = top.getSignature();
			Object constant = top.getConstant();
			if (signature.equals("Ljava/lang/String;") && constant instanceof String) {
				BugInstance bug = new BugInstance(this, "DL_SYNCHRONIZATION_ON_SHARED_CONSTANT", NORMAL_PRIORITY).addClassAndMethod(this);

				String value = (String) constant;
				if (identified.matcher(value).matches())
					bug.addString(value).describe(StringAnnotation.STRING_CONSTANT_ROLE);
				
				bugReporter.reportBug(bug.addSourceLine(this));
			} else if (badSignatures.contains(signature)) {
				boolean isBoolean = signature.equals("Ljava/lang/Boolean;");
				XField field = top.getXField();
				FieldSummary fieldSummary = AnalysisContext.currentAnalysisContext().getFieldSummary();
				OpcodeStack.Item summary = fieldSummary.getSummary(field);
				int priority = NORMAL_PRIORITY;
				if (isBoolean) priority--;
				if (newlyConstructedObject(summary))
					bugReporter.reportBug(new BugInstance(this, "DL_SYNCHRONIZATION_ON_UNSHARED_BOXED_PRIMITIVE", NORMAL_PRIORITY)
					.addClassAndMethod(this).addType(signature).addOptionalField(field).addOptionalLocalVariable(this, top).addSourceLine(this));
				else if (isBoolean) 
					bugReporter.reportBug(new BugInstance(this, "DL_SYNCHRONIZATION_ON_BOOLEAN", priority)
					.addClassAndMethod(this).addOptionalField(field).addOptionalLocalVariable(this, top).addSourceLine(this));
				else bugReporter.reportBug(new BugInstance(this, "DL_SYNCHRONIZATION_ON_BOXED_PRIMITIVE", priority)
				.addClassAndMethod(this).addType(signature).addOptionalField(field).addOptionalLocalVariable(this, top).addSourceLine(this));
			}
		}
	}
}
