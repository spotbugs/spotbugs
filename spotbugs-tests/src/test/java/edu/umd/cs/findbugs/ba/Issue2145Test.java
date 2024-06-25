package edu.umd.cs.findbugs.ba;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.SortedBugCollection;
import org.junit.jupiter.api.Test;

class Issue2145Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue2145.class");
        SortedBugCollection bugCollection = (SortedBugCollection) getBugCollection();
        assertThat(bugCollection.getErrors(), hasSize(0));
    }

}
