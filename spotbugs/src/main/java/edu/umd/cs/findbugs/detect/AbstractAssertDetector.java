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
import org.apache.bcel.classfile.Code;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.BugReporter;
import edu.umd.cs.findbugs.bcel.OpcodeStackDetector;

/**
 * Abstract base class for finding assertions
 */
public abstract class AbstractAssertDetector extends OpcodeStackDetector {

    private final BugReporter bugReporter;

    protected boolean inAssert = false;

    protected AbstractAssertDetector(BugReporter bugReporter) {
        this.bugReporter = bugReporter;
    }

    /**
     * Implement this method in a concrete detector
     */
    protected abstract void detect(int seen);

    /**
     * Resets the state before scanning a method.
     *
     * <p>An assertion never spans a method boundary, but {@code inAssert} is only cleared when
     * a {@code new AssertionError} is seen. A method that reads {@code $assertionsDisabled}
     * without ever throwing (for example a guard that just returns) therefore leaves the flag
     * set, and every method scanned afterwards is treated as if it were inside an assertion.
     * Detector instances are reused for the whole analysis run, so the state also leaks into
     * the following classes.</p>
     */
    @Override
    public void visit(Code obj) {
        inAssert = false;
        super.visit(obj);
    }

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
