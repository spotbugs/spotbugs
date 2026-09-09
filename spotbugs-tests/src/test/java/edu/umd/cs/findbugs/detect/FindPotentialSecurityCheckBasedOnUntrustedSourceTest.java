package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class FindPotentialSecurityCheckBasedOnUntrustedSourceTest extends AbstractIntegrationTest {

    @Test
    void testUntrustedSources() {
        performAnalysis("PotentialSecurityCheckBasedOnUntrustedSource.class",
                "PotentialSecurityCheckBasedOnUntrustedSource$1.class",
                "PotentialSecurityCheckBasedOnUntrustedSource$2.class");

        final String bugType = "USC_POTENTIAL_SECURITY_CHECK_BASED_ON_UNTRUSTED_SOURCE";

        assertBugTypeCount(bugType, 2);
        assertBugInMethodAtLine(bugType, "PotentialSecurityCheckBasedOnUntrustedSource", "badOpenFile", 11);
        assertBugInMethodAtLine(bugType, "PotentialSecurityCheckBasedOnUntrustedSource", "badOpenFileLambda", 40);
    }
}
