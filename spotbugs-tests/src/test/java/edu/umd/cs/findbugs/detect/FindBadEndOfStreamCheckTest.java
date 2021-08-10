package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class FindBadEndOfStreamCheckTest extends AbstractIntegrationTest {
    @Test
    public void testBadEndOfFileChecks() {
        performAnalysis("endOfStreamCheck/BadEndOfStreamCheck.class");

        assertNumOfEOSBugs(16);

        assertEOSBug("badFileInputStream1", 12);
        assertEOSBug("badFileInputStream2", 25);
        assertEOSBug("badFileInputStream3", 39);
        assertEOSBug("badFileInputStream4", 55);
        assertEOSBug("badFileInputStream5", 70);
        assertEOSBug("badFileInputStream6", 83);
        assertEOSBug("badFileInputStream7", 97);
        assertEOSBug("badFileInputStream8", 113);
        assertEOSBug("badFileReader1", 128);
        assertEOSBug("badFileReader2", 141);
        assertEOSBug("badFileReader3", 155);
        assertEOSBug("badFileReader4", 171);
        assertEOSBug("badFileReader5", 186);
        assertEOSBug("badFileReader6", 199);
        assertEOSBug("badFileReader7", 213);
        assertEOSBug("badFileReader8", 229);
    }

    @Test
    public void testGoodEndOfFileChecks() {
        performAnalysis("endOfStreamCheck/GoodEndOfStreamCheck.class");

        assertNumOfEOSBugs(0);
    }

    private void assertNumOfEOSBugs(int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("EOS_BAD_END_OF_STREAM_CHECK").build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertEOSBug(String method, int line) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("EOS_BAD_END_OF_STREAM_CHECK")
                .inClass("BadEndOfStreamCheck")
                .inMethod(method)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }
}
