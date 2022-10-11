package edu.umd.cs.findbugs.detect;

import org.junit.Before;
import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.annotations.Confidence;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;

public class Issue2183Test extends AbstractIntegrationTest {
    @Before
    public void verifyJavaVersion() {
        assumeFalse(System.getProperty("java.specification.version").startsWith("1."));
        int javaVersion = Integer.parseInt(System.getProperty("java.specification.version"));
        assumeThat(javaVersion, is(greaterThanOrEqualTo(11)));
    }

    @Test
    public void test() {
        performAnalysis("../java11/ghIssues/Issue2183.class");
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE")
                .inClass("Issue2183")
                .inMethod("test")
                .atLine(11)
                .withConfidence(Confidence.HIGH)
                .build();
        assertThat(getBugCollection(), hasItem(matcher));
    }
}
