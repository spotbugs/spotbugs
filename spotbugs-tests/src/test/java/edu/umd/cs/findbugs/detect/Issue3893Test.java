package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3893Test extends AbstractIntegrationTest {

    @Test
    void reportsStInInnerClassDespiteLocalShadowing() {
        performAnalysis("ghIssues/Issue3893.class", "ghIssues/Issue3893$Inner.class");
        assertBugInMethod("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", "ghIssues.Issue3893", "accessTopLevel");
        assertBugInMethod("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", "ghIssues.Issue3893$Inner", "accessWithShadowing");
        assertBugInMethod("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", "ghIssues.Issue3893$Inner", "accessQualified");
    }
}
