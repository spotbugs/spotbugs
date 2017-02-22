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

import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class TestingGround2 extends OpcodeStackDetector {

    BugReporter bugReporter;

    @SuppressFBWarnings("URF_UNREAD_FIELD")
    public TestingGround2(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    @Override
    public void visit(Code code) {
        boolean interesting = true;
        if (interesting) {
            // initialize any variables we want to initialize for the method
            super.visit(code); // make callbacks to sawOpcode for all opcodes
        }
    }

    @Override
    public void sawOpcode(int seen) {

    }

}
