package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.junit.Assert.assertThat;

import org.junit.Rule;
import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.SpotBugsRule;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class Issue595Test extends AbstractIntegrationTest {
    @Rule
    public SpotBugsRule spotbugs = new SpotBugsRule();

    /**
     * @see <a href="https://github.com/spotbugs/spotbugs/issues/595">GitHub
     *      issue</a>
     */
    @Test
    public void testRangeArrayLength() {
        performAnalysis("rangeArray/RangeArrayLengthAndOffsetBugExample.class");

        BugInstanceMatcher bugTypeMatcherLength = new BugInstanceMatcherBuilder().bugType("RANGE_ARRAY_LENGTH").build();
        BugInstanceMatcher bugTypeMatcherOffset = new BugInstanceMatcherBuilder().bugType("RANGE_ARRAY_OFFSET").build();

        assertThat(getBugCollection(), containsExactly(4, bugTypeMatcherOffset));
        assertThat(getBugCollection(), containsExactly(13, bugTypeMatcherLength));
    }
}
