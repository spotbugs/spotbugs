package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue4032Test extends AbstractIntegrationTest {

    @Test
    void testExposeRepInAnonymousClass() {
        performAnalysis("ghIssues/Issue4032.class", "ghIssues/Issue4032$Provider.class",
                "ghIssues/Issue4032$NamedProvider.class", "ghIssues/Issue4032$1.class");

        assertBugInMethod("EI_EXPOSE_REP", "ghIssues.Issue4032$NamedProvider", "getNames");
        assertBugInMethod("EI_EXPOSE_REP", "ghIssues.Issue4032$1", "getNames");
        assertNoBugInMethod("EI_EXPOSE_REP", "ghIssues.Issue4032", "getNamesCopy");
    }
}
