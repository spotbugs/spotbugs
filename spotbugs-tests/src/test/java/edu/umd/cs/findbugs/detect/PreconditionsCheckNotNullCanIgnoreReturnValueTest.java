package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class PreconditionsCheckNotNullCanIgnoreReturnValueTest extends AbstractIntegrationTest {

    @Test
    void testDoNotWarnOnCanIgnoreReturnValue() {
        performAnalysis("bugPatterns/RV_RETURN_VALUE_IGNORED_Guava_Preconditions.class");
        assertNoBugType("RV_RETURN_VALUE_IGNORED");
    }

    @Test
    void testIgnoredTrim() {
        performAnalysis("IgnoredTrim.class");

        assertBugTypeCount("RV_RETURN_VALUE_IGNORED", 1);
        assertBugInMethodAtLine("RV_RETURN_VALUE_IGNORED", "IgnoredTrim", "f", 4);
    }
}
