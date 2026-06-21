package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue4035Test extends AbstractIntegrationTest {

    @Test
    void testExplicitPrivateConstructor() {
        performAnalysis("ghIssues/Issue4035.class");
        assertBugInMethod("DMI_RANDOM_USED_ONLY_ONCE", "ghIssues.Issue4035", "numberProvider");
        assertBugInMethod("DMI_RANDOM_USED_ONLY_ONCE", "ghIssues.Issue4035", "boundedProvider");
    }

    @Test
    void testLombokNoSuppressConfig() {
        performAnalysis("../../../../src/classSamples/issue4035/Issue4035Lombok.class");
        assertBugInMethod("DMI_RANDOM_USED_ONLY_ONCE", "ghIssues.Issue4035Lombok", "numberProvider");
        assertBugInMethod("DMI_RANDOM_USED_ONLY_ONCE", "ghIssues.Issue4035Lombok", "boundedProvider");
    }

    @Test
    void testLombokWithAddSuppressFBWarnings() {
        performAnalysis("../../../../src/classSamples/issue4035/Issue4035LombokSuppress.class");
        assertBugInMethod("DMI_RANDOM_USED_ONLY_ONCE", "ghIssues.Issue4035Lombok", "numberProvider");
        assertBugInMethod("DMI_RANDOM_USED_ONLY_ONCE", "ghIssues.Issue4035Lombok", "boundedProvider");
    }
}
