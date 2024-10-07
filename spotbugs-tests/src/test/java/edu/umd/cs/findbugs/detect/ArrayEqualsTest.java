package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;


class ArrayEqualsTest extends AbstractIntegrationTest {
    @Test
    void testNegative() {
        performAnalysis("com/google/errorprone/bugpatterns/ArrayEqualsNegativeCases.class");
        assertNoBugType("EC_BAD_ARRAY_COMPARE");
    }

    @Test
    void testPositive() {
        performAnalysis("com/google/errorprone/bugpatterns/ArrayEqualsPositiveCases.class");
        assertBugTypeCount("EC_BAD_ARRAY_COMPARE", 5);

        assertBugInMethodAtLine("EC_BAD_ARRAY_COMPARE", "ArrayEqualsPositiveCases", "intArray", 30);
        assertBugInMethodAtLine("EC_BAD_ARRAY_COMPARE", "ArrayEqualsPositiveCases", "objectArray", 42);
        assertBugInMethodAtLine("EC_BAD_ARRAY_COMPARE", "ArrayEqualsPositiveCases", "firstMethodCall", 54);
        assertBugInMethodAtLine("EC_BAD_ARRAY_COMPARE", "ArrayEqualsPositiveCases", "secondMethodCall", 66);
        assertBugInMethodAtLine("EC_BAD_ARRAY_COMPARE", "ArrayEqualsPositiveCases", "bothMethodCalls", 78);
    }
}
