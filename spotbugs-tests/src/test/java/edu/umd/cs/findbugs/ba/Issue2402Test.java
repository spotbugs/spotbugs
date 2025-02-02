package edu.umd.cs.findbugs.ba;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue2402Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/issue2402/TestA.class",
                "ghIssues/issue2402/TestA$UMap.class",
                "ghIssues/issue2402/TestB.class",
                "ghIssues/issue2402/TestB$UMap.class",
                "ghIssues/issue2402/TestC.class",
                "ghIssues/issue2402/TestC$UMap.class",
                "ghIssues/issue2402/TestD.class",
                "ghIssues/issue2402/TestD$UMap.class");

        assertBugTypeCount("HE_SIGNATURE_DECLARES_HASHING_OF_UNHASHABLE_CLASS", 4);
    }
}
