package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue3598482Test extends AbstractIntegrationTest {

    @Test
    void reportsImpossibleInstanceofForNullByteArray() {
        performAnalysis("ghIssues/Issue3598482.class");
        assertBugInMethod("BC_IMPOSSIBLE_INSTANCEOF", "ghIssues.Issue3598482", "nullByteArrayInstanceof");
        assertBugInMethod("NP_NULL_INSTANCEOF", "ghIssues.Issue3598482", "nullByteArrayInstanceof");
    }
}
