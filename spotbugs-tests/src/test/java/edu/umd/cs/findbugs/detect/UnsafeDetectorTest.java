package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class UnsafeDetectorTest extends AbstractIntegrationTest {

    /** Tests the bad case for {@code sun.misc.Unsafe}. */
    @Test
    void testBadUnsafeSun() {
        performAnalysis("unsafeClasses/BadUnsafeSun.class");
        assertBugTypeCount("UNS_UNSAFE_CALL", 1);
        assertBugInMethodAtLine("UNS_UNSAFE_CALL", "unsafeClasses.BadUnsafeSun", "method", 12);
    }

    /** Tests the bad case for {@code sun.misc.Unsafe} with a fully qualified name. */
    @Test
    void testUnsafeSunQualified() {
        performAnalysis("unsafeClasses/BadUnsafeSunQualified.class");
        assertBugTypeCount("UNS_UNSAFE_CALL", 1);
        assertBugInMethodAtLine("UNS_UNSAFE_CALL", "unsafeClasses.BadUnsafeSunQualified", "method", 11);
    }

}
