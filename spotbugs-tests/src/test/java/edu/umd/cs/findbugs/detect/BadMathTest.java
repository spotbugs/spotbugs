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

class BadMathTest extends AbstractIntegrationTest {
    @Test
    void testBadRemainderCheck() {
        performAnalysis("BadRemainderCheck.class");

        assertBugTypeCount("IM_MULTIPLYING_RESULT_OF_IREM", 1);
        assertBugInMethod("IM_MULTIPLYING_RESULT_OF_IREM", "BadRemainderCheck", "isOnHourBoundary");
    }

    @Test
    void testModuloFailure() {
        performAnalysis("ModuloFailure.class");

        assertBugTypeCount("IM_MULTIPLYING_RESULT_OF_IREM", 1);
        assertBugInMethodAtLine("IM_MULTIPLYING_RESULT_OF_IREM", "ModuloFailure", "main", 9);
        // FN at line 6 (if x is negative)
    }

    @Test
    void testTestFloatEquality() {
        performAnalysis("TestFloatEquality.class");

        assertBugTypeCountBetween("FE_FLOATING_POINT_EQUALITY", 3, 7);
        // 4 FNs in main
        assertBugInMethodAtLine("FE_FLOATING_POINT_EQUALITY", "TestFloatEquality", "validCoin100", 5);
        assertBugInMethodAtLine("FE_FLOATING_POINT_EQUALITY", "TestFloatEquality", "sum", 73);
        assertBugInMethodAtLine("FE_FLOATING_POINT_EQUALITY", "TestFloatEquality", "isMyDouble", 81);
    }

    @Test
    void testPreferZeroLengthArrays() {
        performAnalysis("PreferZeroLengthArrays.class");

        assertBugTypeCount("PZLA_PREFER_ZERO_LENGTH_ARRAYS", 1);
        assertBugInMethod("PZLA_PREFER_ZERO_LENGTH_ARRAYS", "PreferZeroLengthArrays", "foo");
    }
}
