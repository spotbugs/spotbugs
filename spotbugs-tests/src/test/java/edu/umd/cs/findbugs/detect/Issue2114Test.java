package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue2114Test extends AbstractIntegrationTest {

    @Test
    @EnabledForJreRange(min = JRE.JAVA_11)
    void testIssue() {
        performAnalysis("../java11/Issue2114.class");

        assertNoBugInMethod("DM_DEFAULT_ENCODING", "Issue2114", "filesReadString");
        assertNoBugInMethod("DM_DEFAULT_ENCODING", "Issue2114", "filesReadAllLines");
        assertNoBugInMethod("DM_DEFAULT_ENCODING", "Issue2114", "filesWriteString");
        assertNoBugInMethod("DM_DEFAULT_ENCODING", "Issue2114", "filesNewBufferedReader");
        assertNoBugInMethod("DM_DEFAULT_ENCODING", "Issue2114", "filesNewBufferedWriter");
    }
}
