package edu.umd.cs.findbugs.detect;

import org.junit.Before;
import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;

public class Issue2369Test extends AbstractIntegrationTest {
    @Before
    public void verifyJavaVersion() {
        assumeFalse(System.getProperty("java.specification.version").startsWith("1."));
        int javaVersion = Integer.parseInt(System.getProperty("java.specification.version"));
        assumeThat(javaVersion, is(greaterThanOrEqualTo(14)));
    }

    @Test
    public void test() {
        performAnalysis("../java14/Issue2369.class");
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("MS_EXPOSE_REP")
                .build();

        assertThat(getBugCollection(), containsExactly(0, matcher));
    }
}
