package edu.umd.cs.findbugs.detect;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

public class DontUseFloatsAsLoopCountersTest extends AbstractIntegrationTest {
    @Test
    public void testChecks() {
        performAnalysis("DontUseFloatsAsLoopCounters.class");
        assertNumOfEOSBugs(3);
        assertBug("main", 5);
        assertBug("main", 9);
        assertBug("main", 12);
    }

    private void assertBug(String method, int line) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("FL_FLOATS_AS_LOOP_COUNTERS")
                .inClass("DontUseFloatsAsLoopCounters")
                .inMethod(method)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

    private void assertNumOfEOSBugs(int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("FL_FLOATS_AS_LOOP_COUNTERS").build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }
}
