package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

class UnnecessaryEnvUsageTest extends AbstractIntegrationTest {
    @Test
    void testingEnvUsages() {
        performAnalysis("UnnecessaryEnvUsage.class");
        assertBug(23);
        assertBug("replaceableEnvUsage", 5);
        assertBug("replaceableEnvUsage", 6);
        assertBug("replaceableEnvUsage", 7);
        assertBug("replaceableEnvUsage", 8);
        assertBug("replaceableEnvUsage", 9);
        assertBug("replaceableEnvUsage", 10);
        assertBug("replaceableEnvUsage", 11);
        assertBug("replaceableEnvUsage", 12);
        assertBug("replaceableEnvUsage", 13);
        assertBug("replaceableEnvUsage", 14);
        assertBug("replaceableEnvUsage", 15);
        assertBug("replaceableEnvUsage", 16);

        assertBug("replaceableEnvUsageFromMap", 20);
        assertBug("replaceableEnvUsageFromMapWithVar", 25);
        assertBug("replaceableEnvUsageFromMapWithVar", 26);

        assertBug("replaceableEnvUsageWithVar", 31);
        assertBug("replaceableEnvUsageWithField", 36);

        assertBug("replaceableEnvUsageWithVarFromMap", 41);
        assertBug("replaceableEnvUsageWithFieldFromMap", 46);

        assertBug("replaceableEnvUsageWithVarFromMapWithVar", 53);
        assertBug("replaceableEnvUsageWithVarFromMapWithVar", 54);

        assertBug("replaceableEnvUsageWithFieldFromMapWithVar", 61);
        assertBug("replaceableEnvUsageWithFieldFromMapWithVar", 62);
    }

    private void assertBug(int num) {
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("ENV_USE_PROPERTY_INSTEAD_OF_ENV")
                .build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    public void assertBug(String methodName, int line) {
        performAnalysis("UnnecessaryEnvUsage.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("ENV_USE_PROPERTY_INSTEAD_OF_ENV")
                .inClass("UnnecessaryEnvUsage")
                .inMethod(methodName)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(bugTypeMatcher));
    }
}
