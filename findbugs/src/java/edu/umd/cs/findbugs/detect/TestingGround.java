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
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

public class TestingGround extends OpcodeStackDetector {

    //    final BugReporter bugReporter;

    //    final BugAccumulator accumulator;

    public TestingGround(BugReporter bugReporter) {
        //        this.bugReporter = bugReporter;
        //        this.accumulator = new BugAccumulator(bugReporter);
    }

    @Override
    public void visit(Code code) {
        System.out.println(getFullyQualifiedMethodName());
        super.visit(code);
        System.out.println();
    }

    @Override
    public void sawOpcode(int seen) {
        System.out.printf("%3d %-15s %s%n", getPC(), OPCODE_NAMES[seen], stack);

    }

}
