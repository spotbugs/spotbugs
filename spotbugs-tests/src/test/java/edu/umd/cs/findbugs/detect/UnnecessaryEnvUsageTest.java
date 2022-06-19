package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class UnnecessaryEnvUsageTest extends AbstractIntegrationTest {

    @Test
    public void testingVariuosEnvUsages() {
        performAnalysis("UnnecessaryEnvUsage.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("ENV_USE_PROPERTY_INSTEAD_OF_ENV")
                .build();
        assertThat(getBugCollection(), containsExactly(2, bugTypeMatcher));
    }

    @Test
    public void testingReplaceableEnvUsage() {
        performAnalysis("UnnecessaryEnvUsage.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("ENV_USE_PROPERTY_INSTEAD_OF_ENV")
                .inMethod("replaceableEnvUsage")
                .build();
        assertThat(getBugCollection(), containsExactly(2, bugTypeMatcher));
    }

    @Test
    public void testingValidEnvUsage() {
        performAnalysis("UnnecessaryEnvUsage.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("ENV_USE_PROPERTY_INSTEAD_OF_ENV")
                .inMethod("recessaryEnvUsage")
                .build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }
}
