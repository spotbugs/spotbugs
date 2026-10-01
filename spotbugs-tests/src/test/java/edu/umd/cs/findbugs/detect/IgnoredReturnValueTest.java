package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class IgnoredReturnValueTest extends AbstractIntegrationTest {
    @Test
    void testRV() {
        performAnalysis("RV.class");

        assertBugTypeCount("RV_EXCEPTION_NOT_THROWN", 1);
        assertBugTypeCount("RV_RETURN_VALUE_IGNORED", 2);
        assertBugTypeCount("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE", 1);
        assertBugInMethodAtLine("RV_EXCEPTION_NOT_THROWN", "RV", "f", 11);
        assertBugInMethodAtLine("RV_RETURN_VALUE_IGNORED", "RV", "f", 10);
        assertBugInMethodAtLine("RV_RETURN_VALUE_IGNORED", "RV", "main", 5);
        assertBugInMethodAtLine("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE", "RV", "g", 15);
    }

}
