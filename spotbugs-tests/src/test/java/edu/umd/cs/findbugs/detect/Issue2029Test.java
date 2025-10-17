package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;


class Issue2029Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue2029.class",
                "ghIssues/Issue2029$Inner.class");

        assertBugInMethodAtLine("FI_EXPLICIT_INVOCATION", "ghIssues.Issue2029$Inner", "bug", 6);
    }
}
