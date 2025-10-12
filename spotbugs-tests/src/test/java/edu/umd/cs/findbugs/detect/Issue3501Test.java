package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;


class Issue3501Test extends AbstractIntegrationTest {
    @Test
    void testEquals() {
        performAnalysis("ghIssues/Issue3501.class");

        assertBugTypeCount("IT_NO_SUCH_ELEMENT", 0);
    }
}
