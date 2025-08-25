package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;


class Issue3528Test extends AbstractIntegrationTest {
    @Test
    void testEquals() {
        performAnalysis("ghIssues/Issue3528.class");

        assertBugTypeCount("EQ_ALWAYS_TRUE", 1);
    }
}
