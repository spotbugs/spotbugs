package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class UnnecessaryEnvUsageTest extends AbstractIntegrationTest {
    @Test
    public void testingVariuosEnvUsages() {
        performAnalysis("UnnecessaryEnvUsage.class");
        assertBug(2);
        assertBug("replaceableEnvUsage", 3);
        assertBug("replaceableEnvUsage", 4);
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
