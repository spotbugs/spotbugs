package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/4033">GitHub issue #4033</a>
 */
class Issue4033Test extends AbstractIntegrationTest {

    @Test
    void nullOrCopiedValuesAreNotExposed() {
        performAnalysis("../java17/ghIssues/Issue4033.class",
                "../java17/ghIssues/Issue4033$Feature.class",
                "../java17/ghIssues/Issue4033$MultiPolygon.class",
                "../java17/ghIssues/Issue4033$Holder.class",
                "../java17/ghIssues/Issue4033$NullStore.class");
        assertNoMissingClasses();
        assertNoBugType("EI_EXPOSE_REP");
        assertNoBugType("EI_EXPOSE_REP2");
        assertNoOtherFindReturnRefBugs();
    }

    @Test
    void uncopiedValuesAreStillExposed() {
        performAnalysis("../java17/ghIssues/Issue4033Uncopied.class");
        assertNoMissingClasses();
        assertBugTypeCount("EI_EXPOSE_REP", 1);
        assertBugInMethodAtLine("EI_EXPOSE_REP", "Issue4033Uncopied", "getNames", 20);
        assertBugTypeCount("EI_EXPOSE_REP2", 1);
        assertBugInMethodAtLine("EI_EXPOSE_REP2", "Issue4033Uncopied", "<init>", 16);
        assertNoOtherFindReturnRefBugs();
    }

    private void assertNoOtherFindReturnRefBugs() {
        assertNoBugType("MS_EXPOSE_REP");
        assertNoBugType("MS_EXPOSE_BUF");
        assertNoBugType("EI_EXPOSE_BUF");
        assertNoBugType("EI_EXPOSE_BUF2");
        assertNoBugType("EI_EXPOSE_STATIC_REP2");
        assertNoBugType("EI_EXPOSE_STATIC_BUF2");
    }
}
