package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;

class UnnecessaryEnvUsageTest extends AbstractIntegrationTest {
    @Test
    void testingEnvUsages() {
        final String bugType = "ENV_USE_PROPERTY_INSTEAD_OF_ENV";
        final String className = "UnnecessaryEnvUsage";

        performAnalysis("UnnecessaryEnvUsage.class");
        assertBugTypeCount(bugType, 23);
        assertBugInMethodAtLine(bugType, className, "replaceableEnvUsage", 5);
        assertBugInMethodAtLine(bugType, className, "replaceableEnvUsage", 6);
        assertBugInMethodAtLine(bugType, className, "replaceableEnvUsage", 7);
        assertBugInMethodAtLine(bugType, className, "replaceableEnvUsage", 8);
        assertBugInMethodAtLine(bugType, className, "replaceableEnvUsage", 9);
        assertBugInMethodAtLine(bugType, className, "replaceableEnvUsage", 10);
        assertBugInMethodAtLine(bugType, className, "replaceableEnvUsage", 11);
        assertBugInMethodAtLine(bugType, className, "replaceableEnvUsage", 12);
        assertBugInMethodAtLine(bugType, className, "replaceableEnvUsage", 13);
        assertBugInMethodAtLine(bugType, className, "replaceableEnvUsage", 14);
        assertBugInMethodAtLine(bugType, className, "replaceableEnvUsage", 15);
        assertBugInMethodAtLine(bugType, className, "replaceableEnvUsage", 16);

        assertBugInMethodAtLine(bugType, className, "replaceableEnvUsageFromMap", 20);
        assertBugInMethodAtLine(bugType, className, "replaceableEnvUsageFromMapWithVar", 25);
        assertBugInMethodAtLine(bugType, className, "replaceableEnvUsageFromMapWithVar", 26);

        assertBugInMethodAtLine(bugType, className, "replaceableEnvUsageWithVar", 31);
        assertBugInMethodAtLine(bugType, className, "replaceableEnvUsageWithField", 36);

        assertBugInMethodAtLine(bugType, className, "replaceableEnvUsageWithVarFromMap", 41);
        assertBugInMethodAtLine(bugType, className, "replaceableEnvUsageWithFieldFromMap", 46);

        assertBugInMethodAtLine(bugType, className, "replaceableEnvUsageWithVarFromMapWithVar", 53);
        assertBugInMethodAtLine(bugType, className, "replaceableEnvUsageWithVarFromMapWithVar", 54);

        assertBugInMethodAtLine(bugType, className, "replaceableEnvUsageWithFieldFromMapWithVar", 61);
        assertBugInMethodAtLine(bugType, className, "replaceableEnvUsageWithFieldFromMapWithVar", 62);
    }
}
