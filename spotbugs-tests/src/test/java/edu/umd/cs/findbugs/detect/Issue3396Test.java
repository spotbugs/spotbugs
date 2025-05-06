package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3396Test extends AbstractIntegrationTest {

    @Test
    void testClassLevelSuppression() {
        performAnalysis("ghIssues/issue3396/Issue3396.class");

        assertBugInClassCount("US_USELESS_SUPPRESSION_ON_CLASS", "ghIssues.issue3396.Issue3396", 0);
        assertBugInClassCount("US_USELESS_SUPPRESSION_ON_FIELD", "ghIssues.issue3396.Issue3396", 0);
        assertBugInClassCount("US_USELESS_SUPPRESSION_ON_METHOD", "ghIssues.issue3396.Issue3396", 0);
        assertBugInClassCount("US_USELESS_SUPPRESSION_ON_PARAMETER", "ghIssues.issue3396.Issue3396", 0);
    }
    
    @Test
    void testPackageLevelSuppression() {
        performAnalysis("ghIssues/issue3396/package-info.class");

        assertBugInClassCount("US_USELESS_SUPPRESSION_ON_PACKAGE", "ghIssues.issue3396.Issue3396", 0);
    }
}
