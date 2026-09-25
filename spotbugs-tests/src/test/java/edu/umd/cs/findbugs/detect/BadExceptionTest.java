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

class BadExceptionTest extends AbstractIntegrationTest {
    @Test
    void testDETest() {
        performAnalysis("DETest.class");

        assertBugTypeCount("DE_MIGHT_IGNORE", 1);
        assertBugInMethod("DE_MIGHT_IGNORE", "DETest", "main");
    }

    @Test
    void testDontCatchIllegalMonitor() {
        performAnalysis("DontCatchIllegalMonitor.class");

        assertBugTypeCount("IMSE_DONT_CATCH_IMSE", 1);
        assertBugInMethod("IMSE_DONT_CATCH_IMSE", "DontCatchIllegalMonitor", "foo");
    }
}
