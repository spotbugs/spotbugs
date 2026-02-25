package edu.umd.cs.findbugs.ba;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/3916">GitHub issue #3916</a>
 */
class Issue3916Test extends AbstractIntegrationTest {

    @Test
    void testNullCheckViaInstanceof() {
        performAnalysis("ghIssues/Issue3916.class");
        assertBugInMethod("NP_LOAD_OF_KNOWN_NULL_VALUE", "Issue3916", "foo1");
        assertBugInMethod("NP_LOAD_OF_KNOWN_NULL_VALUE", "Issue3916", "foo2");
    }
}
