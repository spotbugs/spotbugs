package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.junit.Assert.assertThat;

import org.junit.Rule;
import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.SpotBugsRule;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/595">GitHub
 *      issue</a>
 */
public class Issue595Test extends AbstractIntegrationTest {
    @Rule
    public SpotBugsRule spotbugs = new SpotBugsRule();

    @Test
    public void testIoOperationOk() {
        performAnalysis("rangeArray/IoOperationOk.class");
        BugInstanceMatcher bugTypeMatcherLength = new BugInstanceMatcherBuilder().bugType("RANGE_ARRAY_LENGTH").build();
        BugInstanceMatcher bugTypeMatcherOffset = new BugInstanceMatcherBuilder().bugType("RANGE_ARRAY_OFFSET").build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcherLength));
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcherOffset));
    }

    @Test
    public void testIoOperationRangeArrayLengthExpected() {
        performAnalysis("rangeArray/IoOperationRangeArrayLengthExpected.class");
        BugInstanceMatcher bugTypeMatcherLength = new BugInstanceMatcherBuilder().bugType("RANGE_ARRAY_LENGTH").build();
        assertThat(getBugCollection(), containsExactly(5, bugTypeMatcherLength));
    }

    @Test
    public void testIoOperationRangeArrayOffsetExpected() {
        performAnalysis("rangeArray/IoOperationRangeArrayOffsetExpected.class");
        BugInstanceMatcher bugTypeMatcherOffset = new BugInstanceMatcherBuilder().bugType("RANGE_ARRAY_OFFSET").build();
        assertThat(getBugCollection(), containsExactly(2, bugTypeMatcherOffset));
    }

    @Test
    public void testStringConstructorOk() {
        performAnalysis("rangeArray/StringConstructorOk.class");
        BugInstanceMatcher bugTypeMatcherLength = new BugInstanceMatcherBuilder().bugType("RANGE_ARRAY_LENGTH").build();
        BugInstanceMatcher bugTypeMatcherOffset = new BugInstanceMatcherBuilder().bugType("RANGE_ARRAY_OFFSET").build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcherLength));
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcherOffset));
    }

    @Test
    public void testStringConstructorRangeArrayLengthExpected() {
        performAnalysis("rangeArray/StringConstructorRangeArrayLengthExpected.class");
        BugInstanceMatcher bugTypeMatcherLength = new BugInstanceMatcherBuilder().bugType("RANGE_ARRAY_LENGTH").build();
        assertThat(getBugCollection(), containsExactly(5, bugTypeMatcherLength));
    }

    @Test
    public void testStringConstructorRangeArrayOffsetExpected() {
        performAnalysis("rangeArray/StringConstructorRangeArrayOffsetExpected.class");
        BugInstanceMatcher bugTypeMatcherOffset = new BugInstanceMatcherBuilder().bugType("RANGE_ARRAY_OFFSET").build();
        assertThat(getBugCollection(), containsExactly(2, bugTypeMatcherOffset));
    }
}
