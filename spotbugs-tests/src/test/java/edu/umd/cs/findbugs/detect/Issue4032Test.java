package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue4032Test extends AbstractIntegrationTest {

    @Test
    void anonymousClassGetterExposesInternalArray() {
        performAnalysis("ghIssues/Issue4032.class",
                "ghIssues/Issue4032$Provider.class",
                "ghIssues/Issue4032$NamedProvider.class",
                "ghIssues/Issue4032$1.class");

        // Named nested class: already reported before the fix.
        assertBugInMethodAtField("EI_EXPOSE_REP", "Issue4032$NamedProvider", "getNames", "names");
        // Anonymous class implementing the same public interface: the false negative fixed by #4032.
        assertBugInMethodAtField("EI_EXPOSE_REP", "Issue4032$1", "getNames", "names");
        assertBugTypeCount("EI_EXPOSE_REP", 2);
    }
}
