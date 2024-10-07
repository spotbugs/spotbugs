package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/595">GitHub
 *      issue</a>
 */
class Issue595Test extends AbstractIntegrationTest {

    @Test
    void testIoOperationOk() {
        performAnalysis("rangeArray/IoOperationOk.class");
        assertNoBugType("RANGE_ARRAY_LENGTH");
        assertNoBugType("RANGE_ARRAY_OFFSET");
    }

    @Test
    void testIoOperationRangeArrayLengthExpected() {
        performAnalysis("rangeArray/IoOperationRangeArrayLengthExpected.class");
        assertBugTypeCount("RANGE_ARRAY_LENGTH", 5);
    }

    @Test
    void testIoOperationRangeArrayOffsetExpected() {
        performAnalysis("rangeArray/IoOperationRangeArrayOffsetExpected.class");
        assertBugTypeCount("RANGE_ARRAY_OFFSET", 2);
    }

    @Test
    void testStringConstructorOk() {
        performAnalysis("rangeArray/StringConstructorOk.class");
        assertNoBugType("RANGE_ARRAY_LENGTH");
        assertNoBugType("RANGE_ARRAY_OFFSET");
    }

    @Test
    void testStringConstructorRangeArrayLengthExpected() {
        performAnalysis("rangeArray/StringConstructorRangeArrayLengthExpected.class");
        assertBugTypeCount("RANGE_ARRAY_LENGTH", 5);
    }

    @Test
    void testStringConstructorRangeArrayOffsetExpected() {
        performAnalysis("rangeArray/StringConstructorRangeArrayOffsetExpected.class");
        assertBugTypeCount("RANGE_ARRAY_OFFSET", 2);
    }
}
