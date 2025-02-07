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

        // We're also getting many (true positive) US_USELESS_SUPPRESSION_ON_METHOD for this sample code

        assertMethodBugsCount("suppressedNpePrefix", 0);
        assertBugInMethodAtLine("NP_ALWAYS_NULL", "suppress.SuppressedBugs", "nonSuppressedNpePrefix", 19);
        assertBugInMethodAtLine("NP_LOAD_OF_KNOWN_NULL_VALUE", "suppress.SuppressedBugs", "nonSuppressedNpePrefix", 19);

        assertMethodBugsCount("suppressedNpeExact", 0);
        assertBugInMethodAtLine("NP_ALWAYS_NULL", "suppress.SuppressedBugs", "nonSuppressedNpeExact", 33);
        assertBugInMethodAtLine("NP_LOAD_OF_KNOWN_NULL_VALUE", "suppress.SuppressedBugs", "nonSuppressedNpeExact", 33);
        assertBugInMethodAtLine("NP_ALWAYS_NULL", "suppress.SuppressedBugs", "nonSuppressedNpeExactDifferentCase", 41);
        assertBugInMethodAtLine("NP_LOAD_OF_KNOWN_NULL_VALUE", "suppress.SuppressedBugs", "nonSuppressedNpeExactDifferentCase", 41);

        assertMethodBugsCount("suppressedNpeRegex", 0);
        assertBugInMethodAtLine("NP_ALWAYS_NULL", "suppress.SuppressedBugs", "nonSuppressedNpeRegex", 55);
        assertBugInMethodAtLine("NP_LOAD_OF_KNOWN_NULL_VALUE", "suppress.SuppressedBugs", "nonSuppressedNpeRegex", 55);
    }

    private void assertMethodBugsCount(String methodName, int expectedCount) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder().inMethod(methodName).build();
        assertThat(getBugCollection(), containsExactly(expectedCount, matcher));
    }
}
