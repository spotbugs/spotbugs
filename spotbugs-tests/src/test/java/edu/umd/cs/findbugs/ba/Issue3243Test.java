package edu.umd.cs.findbugs.ba;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;

public class Issue3243Test extends AbstractIntegrationTest {

    @Test
    public void testBugSample() {
        performAnalysis("ghIssues/Issue3243.class");

        assertThat(getBugCollection(), emptyIterable());
    }
}
