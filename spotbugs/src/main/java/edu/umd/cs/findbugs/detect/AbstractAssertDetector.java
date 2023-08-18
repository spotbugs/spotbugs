/*
 * SpotBugs - Find bugs in Java programs
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

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

/**
 * Abstract base class for finding assertions
 */
public abstract class AbstractAssertDetector extends OpcodeStackDetector {

    private final BugReporter bugReporter;

    protected boolean inAssert = false;

    public AbstractAssertDetector(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    /**
     * Implement this method in a concrete detector
     */
    abstract protected void detect(int seen);

    /**
     * Searches for assertion opening, and closing points.
     * When in assert, will call the detect method.
     */
    @Override
    public void sawOpcode(int seen) {
        if (inAssert) {
            detect(seen);
        }
        if (seen == Const.GETSTATIC && "$assertionsDisabled".equals(getNameConstantOperand())) {
            inAssert = true;
        }
        if (seen == Const.NEW && getClassConstantOperand().equals("java/lang/AssertionError")) {
            inAssert = false;
        }
    }

    protected void reportBug(BugInstance bug) {
        bugReporter.reportBug(bug);
    }
}
