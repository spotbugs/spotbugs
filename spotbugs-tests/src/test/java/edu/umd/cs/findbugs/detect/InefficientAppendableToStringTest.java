/*
 * Contributions to SpotBugs
 * Copyright (C) 2026, Nikita Dhamapurkar
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

/**
 * Integration tests for {@link InefficientAppendableToString} detector.
 *
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/704">Issue #704</a>
 */
class InefficientAppendableToStringTest extends AbstractIntegrationTest {

    @Test
    void testInefficientAppendableToString() {
        performAnalysis("inefficient/InefficientAppendableToStringDemo.class");

        // Should detect inefficient toString() calls before append
        assertBugTypeCount("IATS_INEFFICIENT_APPEND_TO_STRING", 4);

        // Verify bugs are in the expected methods
        assertBugInMethod("IATS_INEFFICIENT_APPEND_TO_STRING",
                "inefficient.InefficientAppendableToStringDemo", "appendStringBuilderToString_bad");
        assertBugInMethod("IATS_INEFFICIENT_APPEND_TO_STRING",
                "inefficient.InefficientAppendableToStringDemo", "appendStringBufferToString_bad");
        assertBugInMethod("IATS_INEFFICIENT_APPEND_TO_STRING",
                "inefficient.InefficientAppendableToStringDemo", "appendCharSequenceToString_bad");
        assertBugInMethod("IATS_INEFFICIENT_APPEND_TO_STRING",
                "inefficient.InefficientAppendableToStringDemo", "appendInChain_bad");
    }
}
