package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

class ArrayEqualsTest extends AbstractIntegrationTest {
    @Test
    void testNegative() {
        performAnalysis("com/google/errorprone/bugpatterns/ArrayEqualsNegativeCases.class");
        assertNumOfBugs(0);
    }

    @Test
    void testPositive() {
        performAnalysis("com/google/errorprone/bugpatterns/ArrayEqualsPositiveCases.class");
        assertNumOfBugs(5);

        assertExactBug("ArrayEqualsPositiveCases", "intArray", 30);
        assertExactBug("ArrayEqualsPositiveCases", "objectArray", 42);
        assertExactBug("ArrayEqualsPositiveCases", "firstMethodCall", 54);
        assertExactBug("ArrayEqualsPositiveCases", "secondMethodCall", 66);
        assertExactBug("ArrayEqualsPositiveCases", "bothMethodCalls", 78);
    }

    private void assertNumOfBugs(int num) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("EC_BAD_ARRAY_COMPARE")
                .build();
        assertThat(getBugCollection(), containsExactly(num, matcher));
    }

    private void assertExactBug(String className, String method, int line) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("EC_BAD_ARRAY_COMPARE")
                .inClass(className)
                .inMethod(method)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), containsExactly(1, matcher));
    }
}
