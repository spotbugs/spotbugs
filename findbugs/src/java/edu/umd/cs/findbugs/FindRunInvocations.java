/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2003, University of Maryland
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

package edu.umd.cs.findbugs;
import org.apache.bcel.classfile.*;
import edu.umd.cs.pugh.visitclass.Constants2;
import java.util.HashSet;

public class FindRunInvocations extends BytecodeScanningDetector implements   Constants2 {

   private BugReporter bugReporter;

   public FindRunInvocations(BugReporter bugReporter) {
	this.bugReporter = bugReporter;
	}


   private boolean isThread(String clazz) {
	  try {
		return org.apache.bcel.Repository.instanceOf(clazz,"java.lang.Thread");
	  } catch (ClassNotFoundException e) {
		bugReporter.logError("Repository lookup failure: " + e.toString());
		return false;
	  }
	}
   public void sawOpcode(int seen) {
	if ((seen == INVOKEVIRTUAL || seen == INVOKEINTERFACE) 
				&& nameConstant.equals("run")
				&& sigConstant.equals("()V")
				&& isThread(betterClassConstant)
				)
		bugReporter.reportBug(new BugInstance("RU_INVOKE_RUN", NORMAL_PRIORITY)
			.addClassAndMethod(this)
			.addSourceLine(this));
	}
}
