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

        assertBugTypeCount("NP_LOAD_OF_KNOWN_NULL_VALUE", 3);

        // foo1: classic != null check (baseline regression)
        assertBugInMethod("NP_LOAD_OF_KNOWN_NULL_VALUE", "Issue3916", "foo1");
        // foo2: instanceof where variable type == check type → not-instanceof implies null
        assertBugInMethod("NP_LOAD_OF_KNOWN_NULL_VALUE", "Issue3916", "foo2");
        // foo3: negated instanceof !(s instanceof String) → body only reached when null
        assertBugInMethod("NP_LOAD_OF_KNOWN_NULL_VALUE", "Issue3916", "foo3");

        // foo4: instanceof checks a subtype (String) of the variable's type (Object)
        // → not-instanceof does NOT imply null
        assertNoBugInMethod("NP_LOAD_OF_KNOWN_NULL_VALUE", "Issue3916", "foo4");
    }
}
