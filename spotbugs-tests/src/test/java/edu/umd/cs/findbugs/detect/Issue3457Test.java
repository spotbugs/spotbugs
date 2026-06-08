package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3457Test extends AbstractIntegrationTest {

    @Test
    void testSingletonGetterWithHelperMethod() {
        performAnalysis("ghIssues/Issue3457WithHelper.class", "ghIssues/Issue3457WithLiteral.class",
                "ghIssues/Issue3457SynchronizedHelper.class");

        assertBugInMethod("SING_SINGLETON_GETTER_NOT_SYNCHRONIZED", "ghIssues.Issue3457WithLiteral", "getInstance");
        assertBugInMethod("SING_SINGLETON_GETTER_NOT_SYNCHRONIZED", "ghIssues.Issue3457WithHelper", "getInstance");
        assertBugInClassCount("SING_SINGLETON_GETTER_NOT_SYNCHRONIZED", "ghIssues.Issue3457SynchronizedHelper", 0);
    }
}
