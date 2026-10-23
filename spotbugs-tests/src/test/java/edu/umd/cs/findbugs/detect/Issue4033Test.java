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
        assertNoBugType("EI_EXPOSE_REP");
        assertNoBugType("EI_EXPOSE_REP2");
    }

    @Test
    void uncopiedValuesAreStillExposed() {
        performAnalysis("../java17/ghIssues/Issue4033$Uncopied.class");
        assertBugTypeCount("EI_EXPOSE_REP", 1);
        assertBugTypeCount("EI_EXPOSE_REP2", 1);
    }
}
