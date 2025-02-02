package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue2331Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("infiniteLoop/Issue2331.class");
        assertBugTypeCount("IL_INFINITE_LOOP", 1);
    }
}
