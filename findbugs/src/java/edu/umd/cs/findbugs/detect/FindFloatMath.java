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

public class FindFloatMath extends BytecodeScanningDetector implements StatelessDetector {
	private BugReporter bugReporter;

	public FindFloatMath(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}



	@Override
		 public void sawOpcode(int seen) {
		switch (seen) {
		case FMUL:
		case FDIV:
			if (getFullyQualifiedMethodName().indexOf("float") == -1
					&& getFullyQualifiedMethodName().indexOf("Float") == -1
					&& getFullyQualifiedMethodName().indexOf("FLOAT") == -1)
				bugReporter.reportBug(new BugInstance(this, "FL_MATH_USING_FLOAT_PRECISION",
						LOW_PRIORITY)
						.addClassAndMethod(this)
						.addSourceLine(this));
			break;
		case FCMPG:
		case FCMPL:
			break;
		case FADD:
		case FSUB:
		case FREM:
			if (getFullyQualifiedMethodName().indexOf("float") == -1
					&& getFullyQualifiedMethodName().indexOf("Float") == -1
					&& getFullyQualifiedMethodName().indexOf("FLOAT") == -1)

				bugReporter.reportBug(new BugInstance(this, "FL_MATH_USING_FLOAT_PRECISION",
						NORMAL_PRIORITY)
						.addClassAndMethod(this)
						.addSourceLine(this));
			break;
		default:
			break;
		}
	}
}
