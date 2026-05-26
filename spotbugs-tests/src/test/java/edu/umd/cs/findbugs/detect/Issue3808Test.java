package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3808Test extends AbstractIntegrationTest {

    @Test
    void methodSuppressionsApplyToLambdaBodies() {
        performAnalysis("ghIssues/Issue3808.class");

        assertBugInClassCount("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", "ghIssues.Issue3808", 1);
        assertBugInClassCount("US_USELESS_SUPPRESSION_ON_METHOD", "ghIssues.Issue3808", 0);
    }
}
