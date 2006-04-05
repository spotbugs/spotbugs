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


import edu.umd.cs.findbugs.*;
import edu.umd.cs.findbugs.ba.Hierarchy;
import org.apache.bcel.classfile.Code;

public class FindRunInvocations extends BytecodeScanningDetector implements StatelessDetector {

	private BugReporter bugReporter;
	private boolean alreadySawStart;

	public FindRunInvocations(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}


	private boolean isThread(String clazz) {
		try {
			return Hierarchy.isSubtype(clazz, "java.lang.Thread");
		} catch (ClassNotFoundException e) {
			bugReporter.reportMissingClass(e);
			return false;
		}
	}

	@Override
         public void visit(Code obj) {
		alreadySawStart = false;
		super.visit(obj);
	}

	@Override
         public void sawOpcode(int seen) {
		if (alreadySawStart) return;
		if ((seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE)
		        && getSigConstantOperand().equals("()V")
		        && isThread(getDottedClassConstantOperand())
		) {
			if (getNameConstantOperand().equals("start"))
				alreadySawStart = true;
			else if (getNameConstantOperand().equals("run"))
				bugReporter
				        .reportBug(new BugInstance(this, "RU_INVOKE_RUN", NORMAL_PRIORITY)
				        .addClassAndMethod(this)
				        .addSourceLine(this));
		}
	}
}
