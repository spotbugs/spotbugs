package edu.umd.cs.findbugs;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

class WarningSuppressorTest extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("suppress/SuppressedBugs.class");

        assertMethodBugsCount("suppressedNpePrefix", 0);
        assertMethodBugsCount("nonSuppressedNpePrefix", 2);

        assertMethodBugsCount("suppressedNpeExact", 0);
        assertMethodBugsCount("nonSuppressedNpeExact", 2);
        assertMethodBugsCount("nonSuppressedNpeExactDifferentCase", 2);

        assertMethodBugsCount("suppressedNpeRegex", 0);
        assertMethodBugsCount("nonSuppressedNpeRegex", 2);
    }

    private void assertMethodBugsCount(String methodName, int expectedCount) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder().inMethod(methodName).build();
        assertThat(getBugCollection(), containsExactly(expectedCount, matcher));
    }
}
