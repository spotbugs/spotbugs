package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class UnreliableMethodCallBeforeAndOutsideDoPrivilegedTest extends AbstractIntegrationTest {

    @Test
    public void test() throws Exception {
        performAnalysis("UnreliableMethodCallBeforeAndOutsideDoPrivileged.class",
                "UnreliableMethodCallBeforeAndOutsideDoPrivileged$1.class",
                "UnreliableMethodCallBeforeAndOutsideDoPrivileged$2.class");

        assertNumOfDPBugs(1);
        assertDPBug("badOpenFile", 11);
    }

    private void assertNumOfDPBugs(int num) throws Exception {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("DP_UNRELIABLE_METHOD_CALL_BEFORE_AND_INSIDE_DO_PRIVILEGED").build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertDPBug(String methodName, int line) throws Exception {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("DP_UNRELIABLE_METHOD_CALL_BEFORE_AND_INSIDE_DO_PRIVILEGED")
                .inClass("UnreliableMethodCallBeforeAndOutsideDoPrivileged")
                .inMethod(methodName)
                .atLine(line)
                .build();

        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }
}
