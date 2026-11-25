package edu.umd.cs.findbugs.detect;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.hasItem;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/4426">GitHub issue #4426</a>
 */
class Issue4426Test extends AbstractIntegrationTest {

    /** Line of {@code sink += n0.v;} in both sample classes. */
    private static final int DEREFERENCE_LINE = 12;

    @Test
    void testNullAssignedUnderBranch() {
        performAnalysis("ghIssues/issue4426/seed/Exit.class",
                "ghIssues/issue4426/seed/Exit$Node.class");

        assertBugInMethodAtLine("NP_NULL_ON_SOME_PATH", "ghIssues.issue4426.seed.Exit", "main", DEREFERENCE_LINE);
    }

    @Test
    void testNullAssignedInsideLoopBody() {
        performAnalysis("ghIssues/issue4426/variant/Exit.class",
                "ghIssues/issue4426/variant/Exit$Node.class");

        assertThat(getBugCollection(), anyOf(
                hasItem(npAtDereference("NP_NULL_ON_SOME_PATH")),
                hasItem(npAtDereference("NP_ALWAYS_NULL"))));
    }

    @Test
    void testLoopUntilNonNullIsSafe() {
        performAnalysis("ghIssues/issue4426/LoopUntilNonNull.class",
                "ghIssues/issue4426/LoopUntilNonNull$Node.class");

        for (String bugType : new String[] { "NP_NULL_ON_SOME_PATH", "NP_ALWAYS_NULL",
            "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE" }) {
            assertNoBugInMethod(bugType, "ghIssues.issue4426.LoopUntilNonNull", "main");
        }
    }

    @Test
    void testNullOverwrittenInsideLoopBodyIsSafe() {
        performAnalysis("ghIssues/issue4426/OverwrittenInLoop.class",
                "ghIssues/issue4426/OverwrittenInLoop$Node.class");

        for (String bugType : new String[] { "NP_NULL_ON_SOME_PATH", "NP_ALWAYS_NULL", "NP_GUARANTEED_DEREF" }) {
            assertNoBugInMethod(bugType, "ghIssues.issue4426.OverwrittenInLoop", "main");
        }
    }

    private static BugInstanceMatcher npAtDereference(String bugType) {
        return new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inClass("ghIssues.issue4426.variant.Exit")
                .inMethod("main")
                .atLine(DEREFERENCE_LINE)
                .build();
    }
}
