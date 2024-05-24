package edu.umd.cs.findbugs.ba;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

public class Issue2995Test  extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue2995.class");

        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("SF_DEAD_STORE_DUE_TO_SWITCH_FALLTHROUGH").build();

        SortedBugCollection bugCollection = (SortedBugCollection) getBugCollection();
        assertThat(bugCollection, containsExactly(0, bugTypeMatcher));

    }
}
