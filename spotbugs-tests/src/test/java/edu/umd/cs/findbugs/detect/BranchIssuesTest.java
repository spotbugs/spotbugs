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

class BranchIssuesTest extends AbstractIntegrationTest {
    @Test
    void testDuplicateBranches() {
        performAnalysis("DuplicateBranches.class");

        assertBugTypeCount("DB_DUPLICATE_BRANCHES", 1);
        assertBugTypeCountBetween("DB_DUPLICATE_SWITCH_CLAUSES", 1, 2);
        // FNs: doit3
        assertBugInMethod("DB_DUPLICATE_BRANCHES", "DuplicateBranches", "doit");
        assertBugInMethod("DB_DUPLICATE_SWITCH_CLAUSES", "DuplicateBranches", "doit2");
    }

    @Test
    void testEmptyIfStatement() {
        performAnalysis("EmptyIfStatement.class");

        assertBugTypeCount("UCF_USELESS_CONTROL_FLOW", 1);
        assertBugInMethod("UCF_USELESS_CONTROL_FLOW", "EmptyIfStatement", "main");
    }

    @Test
    void testUselessControlFlow() {
        performAnalysis("UselessControlFlow.class");

        assertBugTypeCount("UCF_USELESS_CONTROL_FLOW", 6);
        assertBugInMethod("UCF_USELESS_CONTROL_FLOW", "UselessControlFlow", "harmless1");
        assertBugInMethod("UCF_USELESS_CONTROL_FLOW", "UselessControlFlow", "report0");
        assertBugInMethod("UCF_USELESS_CONTROL_FLOW", "UselessControlFlow", "report1");
        assertBugInMethod("UCF_USELESS_CONTROL_FLOW", "UselessControlFlow", "report2");
        assertBugInMethod("UCF_USELESS_CONTROL_FLOW", "UselessControlFlow", "report3");
        assertBugInMethod("UCF_USELESS_CONTROL_FLOW", "UselessControlFlow", "report4");
    }

    @Test
    void testNonShortCircuit() {
        performAnalysis("NonShortCircuit.class");

        assertBugTypeCount("NS_NON_SHORT_CIRCUIT", 3);
        assertBugTypeCount("NS_DANGEROUS_NON_SHORT_CIRCUIT", 1);
        assertBugInMethod("NS_NON_SHORT_CIRCUIT", "NonShortCircuit", "f");
        assertBugInMethod("NS_NON_SHORT_CIRCUIT", "NonShortCircuit", "nonEmpty");
        assertBugInMethod("NS_NON_SHORT_CIRCUIT", "NonShortCircuit", "ordered");
        assertBugInMethod("NS_DANGEROUS_NON_SHORT_CIRCUIT", "NonShortCircuit", "arrayDanger");
    }

    @Test
    void testStringCompare() {
        performAnalysis("StringCompare.class");

        assertBugTypeCount("NS_DANGEROUS_NON_SHORT_CIRCUIT", 1);
        assertBugTypeCount("NS_NON_SHORT_CIRCUIT", 2);
        assertBugInMethodAtLine("NS_DANGEROUS_NON_SHORT_CIRCUIT", "StringCompare", "compare2", 13);
        assertBugInMethodAtLine("NS_NON_SHORT_CIRCUIT", "StringCompare", "compare", 5);
        assertBugInMethodAtLine("NS_NON_SHORT_CIRCUIT", "StringCompare", "compare", 9);
    }
}
