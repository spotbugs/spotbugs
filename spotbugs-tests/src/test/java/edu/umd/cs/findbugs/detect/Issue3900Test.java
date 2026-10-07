package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue3900Test extends AbstractIntegrationTest {

    private static final String BUG_TYPE = "RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT";

    private static final String CLASS_NAME = "ghIssues.Issue3900";

    @Test
    void ignoredResultIsReported() {
        analyze();

        assertBugInMethod(BUG_TYPE, CLASS_NAME, "ignoredCopyOf");
        assertBugInMethod(BUG_TYPE, CLASS_NAME, "ignoredCopyOfObjects");
        assertBugInMethod(BUG_TYPE, CLASS_NAME, "ignoredCopyOfRange");
        assertBugInMethod(BUG_TYPE, CLASS_NAME, "ignoredCopyOfRangeObjects");
        assertBugInMethod(BUG_TYPE, CLASS_NAME, "ignoredPureStaticOfClassUsingAssert");
        assertBugInMethod(BUG_TYPE, CLASS_NAME, "ignoredConstructorOfClassUsingAssert");
        assertBugTypeCount(BUG_TYPE, 6);
    }

    @Test
    void usedResultIsNotReported() {
        analyze();

        assertNoBugInMethod(BUG_TYPE, CLASS_NAME, "usedCopyOf");
        assertNoBugInMethod(BUG_TYPE, CLASS_NAME, "usedCopyOfRange");
        assertNoBugInMethod(BUG_TYPE, CLASS_NAME, "usedPureStaticOfClassUsingAssert");
    }

    private void analyze() {
        performAnalysis("ghIssues/Issue3900.class", "ghIssues/Issue3900UsingAssert.class");
    }
}
