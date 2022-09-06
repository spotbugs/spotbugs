package edu.umd.cs.findbugs.detect;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

public class Issue2142Test extends AbstractIntegrationTest {
    @Test
    public void test() {
        performAnalysis("ghIssues/Issue2142.class",
                "ghIssues/Issue2142$Inner.class");
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("SA_FIELD_SELF_ASSIGNMENT").build();
        assertThat(getBugCollection(), containsExactly(2, matcher));

        matcher = new BugInstanceMatcherBuilder()
                .bugType("SA_FIELD_SELF_ASSIGNMENT")
                .inClass("Issue2142")
                .inMethod("foo1")
                .atLine(6)
                .build();
        assertThat(getBugCollection(), hasItem(matcher));

        matcher = new BugInstanceMatcherBuilder()
                .bugType("SA_FIELD_SELF_ASSIGNMENT")
                .inClass("Issue2142$Inner")
                .inMethod("foo2")
                .atLine(10)
                .build();
        assertThat(getBugCollection(), hasItem(matcher));
    }
}
