/*
 * Contributions to SpotBugs
 * Copyright (C) 2020, ritchan
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

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class BadMultiThreadTest extends AbstractIntegrationTest {
    @Test
    void testDoublecheck() {
        performAnalysis("Doublecheck.class");

        assertBugTypeCountBetween("DC_DOUBLECHECK", 2, 4);
        // FNs: stringDoubleCheck, longDoubleCheck
        assertBugInMethod("DC_DOUBLECHECK", "Doublecheck", "standardDoubleCheck");
        assertBugInMethod("DC_DOUBLECHECK", "Doublecheck", "getData");
    }

    @Test
    void testMismatchedWaitFalsePositive() {
        performAnalysis("MismatchedWaitFalsePositive.class");

        assertNoBugType("MWN_MISMATCHED_WAIT");
    }

    @Test
    void testTestFalsePositiveMWN() {
        performAnalysis("TestFalsePositiveMWN.class");

        assertNoBugType("MWN_MISMATCHED_WAIT");
        assertNoBugType("MWN_MISMATCHED_NOTIFY");
    }

    @Test
    void testNotThreadSafe() {
        performAnalysis("NotThreadSafe.class", "ThreadSafe.class");

        assertBugTypeCount("IS2_INCONSISTENT_SYNC", 2);
        assertBugAtField("IS2_INCONSISTENT_SYNC", "ThreadSafe", "x");
        assertBugAtField("IS2_INCONSISTENT_SYNC", "ThreadSafe", "y");
    }

    @Test
    void testSleepWithLock() {
        performAnalysis("SleepWithLock.class");

        assertBugTypeCount("SWL_SLEEP_WITH_LOCK_HELD", 1);
        assertBugInMethod("SWL_SLEEP_WITH_LOCK_HELD", "SleepWithLock", "sleepWithLock");
    }

    @Test
    void testSpinWait() {
        performAnalysis("SpinWait.class");

        assertBugTypeCount("SP_SPIN_ON_FIELD", 6);
        assertBugInMethodAtLine("SP_SPIN_ON_FIELD", "SpinWait", "waitForTrue", 7);
        assertBugInMethodAtLine("SP_SPIN_ON_FIELD", "SpinWait", "waitForVolatileTrue", 12);
        assertBugInMethodAtLine("SP_SPIN_ON_FIELD", "SpinWait", "waitForNonNull", 21);
        assertBugInMethodAtLine("SP_SPIN_ON_FIELD", "SpinWait", "waitForNonNullIndirect", 26);
        assertBugInMethodAtLine("SP_SPIN_ON_FIELD", "SpinWait", "waitForNonNullIndirect", 28);
        assertBugInMethodAtLine("SP_SPIN_ON_FIELD", "SpinWait", "waitForStatic", 35);
    }

    @Test
    void testTwoLockWait() {
        performAnalysis("TwoLockWait.class");

        assertBugTypeCount("TLW_TWO_LOCK_WAIT", 2);
        assertBugInMethod("TLW_TWO_LOCK_WAIT", "TwoLockWait", "waitForIt");
        assertBugInMethod("TLW_TWO_LOCK_WAIT", "TwoLockWait", "myMethod");
    }

    @Test
    void testTwoLocksWhileWaitingFalsePositive() {
        performAnalysis("TwoLocksWhileWaitingFalsePositive.class");
        assertNoBugType("TLW_TWO_LOCK_WAIT");
    }
}
