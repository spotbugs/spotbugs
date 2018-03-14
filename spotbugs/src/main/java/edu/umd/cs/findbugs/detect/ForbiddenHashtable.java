/*
 * FindBugs - Find bugs in Java programs
 * Copyright (C) 2017-2018 Public
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

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class ForbiddenHashtable extends OpcodeStackDetector {
	BugReporter bugReporter;

    public ForbiddenHashtable(BugReporter bugReporter) {
		this.bugReporter = bugReporter;
	}

	/*
	 * Just called once one class file.
	 */
	@Override
	public void visit(Code obj) {
		super.visit(obj);
    }

	/**
	 * @param seen
	 *            One op code of jvm
	 */
	@Override
	public void sawOpcode(int seen) {
        if (seen == Const.NEW) {
            String className = getClassConstantOperand();
            if ((null != className) && className.equals("java/util/Hashtable")) {
                BugInstance bug = new BugInstance(this, "SPEC_NO_HASHTABLE", HIGH_PRIORITY).addClassAndMethod(this)
							.addSourceLine(this, getPC());
                bugReporter.reportBug(bug);
			}
		}
	}

}
