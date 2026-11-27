package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/2128">GitHub issue #2128</a>
 */
class Issue2128Test extends AbstractIntegrationTest {
    @Test
    void testLazyFieldRandomIsNotReported() {
        performAnalysis("ghIssues/Issue2128.class");
        assertNoBugInMethod("DMI_RANDOM_USED_ONLY_ONCE", "Issue2128", "generateOffset");
        assertNoBugInMethod("DMI_RANDOM_USED_ONLY_ONCE", "Issue2128", "lazyInstance");
        assertNoBugInMethod("DMI_RANDOM_USED_ONLY_ONCE", "Issue2128", "lazyDoubleChecked");
        assertNoBugInMethod("DMI_RANDOM_USED_ONLY_ONCE", "Issue2128", "lazySeeded");
        assertNoBugInMethod("DMI_RANDOM_USED_ONLY_ONCE", "Issue2128", "lazySecureRandom");
        assertNoBugInMethod("DMI_RANDOM_USED_ONLY_ONCE", "Issue2128", "lazyEarlyReturn");
        assertNoBugInMethod("DMI_RANDOM_USED_ONLY_ONCE", "Issue2128", "lazyViaLocal");
    }

    @Test
    void testLocalRandomUsedOnceIsReported() {
        performAnalysis("ghIssues/Issue2128.class");
        assertBugInMethod("DMI_RANDOM_USED_ONLY_ONCE", "Issue2128", "localUsedOnce");
        assertBugInMethod("DMI_RANDOM_USED_ONLY_ONCE", "Issue2128", "localUsedOnceWithSeed");
        assertBugTypeCount("DMI_RANDOM_USED_ONLY_ONCE", 2);
    }
}
