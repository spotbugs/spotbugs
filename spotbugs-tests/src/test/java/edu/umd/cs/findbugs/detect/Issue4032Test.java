package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue4032Test extends AbstractIntegrationTest {

    @Test
    void testAnonymousClassGetterExposesInternalArray() {
        performAnalysis("ghIssues/Issue4032.class",
                "ghIssues/Issue4032$NamedProvider.class",
                "ghIssues/Issue4032$Provider.class",
                "ghIssues/Issue4032$1.class");

        assertBugInMethodAtField("EI_EXPOSE_REP", "Issue4032$NamedProvider", "getNames", "names");
        assertBugInMethodAtField("EI_EXPOSE_REP", "Issue4032$1", "getNames", "names");
        assertBugTypeCount("EI_EXPOSE_REP", 2);
    }
}
