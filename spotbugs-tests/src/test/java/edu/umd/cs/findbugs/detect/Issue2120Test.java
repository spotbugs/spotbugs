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

public class Issue2120Test extends AbstractIntegrationTest {
    @Before
    public void verifyJavaVersion() {
        assumeFalse(System.getProperty("java.specification.version").startsWith("1."));
        int javaVersion = Integer.parseInt(System.getProperty("java.specification.version"));
        assumeThat(javaVersion, is(greaterThanOrEqualTo(11)));
    }

    @Test
    public void test() {
        performAnalysis("../java14/Issue2120.class",
                "../java14/Issue2120$1MyEnum.class",
                "../java14/Issue2120$1MyRecord.class");
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS").build();
        assertThat(getBugCollection(), containsExactly(0, matcher));
    }
}
