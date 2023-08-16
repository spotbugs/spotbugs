package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

public class Issue2436Test extends AbstractIntegrationTest {

    @Test
    public void test() {
        performAnalysis("ghIssues/Issue2436.class");
        BugInstanceMatcher bugMatcher = new BugInstanceMatcherBuilder()
                .bugType("SE_NO_SERIALVERSIONID")
                .build();
        assertThat(getBugCollection(), containsExactly(1, bugMatcher));
    }

}
