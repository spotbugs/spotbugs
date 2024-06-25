package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/595">GitHub
 *      issue</a>
 */
class Issue595Test extends AbstractIntegrationTest {

    @Test
    void testIoOperationOk() {
        performAnalysis("rangeArray/IoOperationOk.class");
        BugInstanceMatcher bugTypeMatcherLength = new BugInstanceMatcherBuilder().bugType("RANGE_ARRAY_LENGTH").build();
        BugInstanceMatcher bugTypeMatcherOffset = new BugInstanceMatcherBuilder().bugType("RANGE_ARRAY_OFFSET").build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcherLength));
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcherOffset));
    }

    @Test
    void testIoOperationRangeArrayLengthExpected() {
        performAnalysis("rangeArray/IoOperationRangeArrayLengthExpected.class");
        BugInstanceMatcher bugTypeMatcherLength = new BugInstanceMatcherBuilder().bugType("RANGE_ARRAY_LENGTH").build();
        assertThat(getBugCollection(), containsExactly(5, bugTypeMatcherLength));
    }

    @Test
    void testIoOperationRangeArrayOffsetExpected() {
        performAnalysis("rangeArray/IoOperationRangeArrayOffsetExpected.class");
        BugInstanceMatcher bugTypeMatcherOffset = new BugInstanceMatcherBuilder().bugType("RANGE_ARRAY_OFFSET").build();
        assertThat(getBugCollection(), containsExactly(2, bugTypeMatcherOffset));
    }

    @Test
    void testStringConstructorOk() {
        performAnalysis("rangeArray/StringConstructorOk.class");
        BugInstanceMatcher bugTypeMatcherLength = new BugInstanceMatcherBuilder().bugType("RANGE_ARRAY_LENGTH").build();
        BugInstanceMatcher bugTypeMatcherOffset = new BugInstanceMatcherBuilder().bugType("RANGE_ARRAY_OFFSET").build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcherLength));
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcherOffset));
    }

    @Test
    void testStringConstructorRangeArrayLengthExpected() {
        performAnalysis("rangeArray/StringConstructorRangeArrayLengthExpected.class");
        BugInstanceMatcher bugTypeMatcherLength = new BugInstanceMatcherBuilder().bugType("RANGE_ARRAY_LENGTH").build();
        assertThat(getBugCollection(), containsExactly(5, bugTypeMatcherLength));
    }

    @Test
    void testStringConstructorRangeArrayOffsetExpected() {
        performAnalysis("rangeArray/StringConstructorRangeArrayOffsetExpected.class");
        BugInstanceMatcher bugTypeMatcherOffset = new BugInstanceMatcherBuilder().bugType("RANGE_ARRAY_OFFSET").build();
        assertThat(getBugCollection(), containsExactly(2, bugTypeMatcherOffset));
    }
}
