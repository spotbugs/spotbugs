/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003-2005 University of Maryland
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

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.BytecodeScanningDetector;
import edu.umd.cs.findbugs.ba.AnalysisContext;
import edu.umd.cs.findbugs.ba.ClassContext;
import edu.umd.cs.findbugs.ba.XField;

public class VolatileUsage extends BytecodeScanningDetector {
	private BugReporter bugReporter;

	public VolatileUsage(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	@Override
	public void visitClassContext(ClassContext classContext) {
		classContext.getJavaClass().accept(this);
	}

	Set<XField> initializationWrites = new HashSet<XField>();

	Set<XField> otherWrites = new HashSet<XField>();

	@Override
	public void sawOpcode(int seen) {
		switch (seen) {
		case PUTSTATIC: {
			XField f = getXFieldOperand();
			if (!interesting(f))
				return;
			if (getMethodName().equals("<clinit>"))
				initializationWrites.add(f);
			else
				otherWrites.add(f);
			break;
		}
		case PUTFIELD: {
			XField f = getXFieldOperand();
			if (!interesting(f))
				return;

			if (getMethodName().equals("<init>"))
				initializationWrites.add(f);
			else
				otherWrites.add(f);
			break;
		}
		}
	}

	@Override
	public void report() {

		for (XField f : AnalysisContext.currentXFactory().allFields())
			if (interesting(f)) {
				int priority = LOW_PRIORITY;
				if (initializationWrites.contains(f) && !otherWrites.contains(f))
					priority = NORMAL_PRIORITY;
				bugReporter.reportBug(new BugInstance(this, "VO_VOLATILE_REFERENCE_TO_ARRAY", priority).addClass(
				        f.getClassDescriptor()).addField(f));
			}
	}

	/**
	 * @param f
	 * @return
	 */
	private boolean interesting(XField f) {
		return f != null && f.isVolatile() && f.getSignature().charAt(0) == '[';
	}
}
