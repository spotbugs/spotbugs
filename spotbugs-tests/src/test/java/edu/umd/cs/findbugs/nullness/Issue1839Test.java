package edu.umd.cs.findbugs.nullness;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/1839">GitHub Issue</a>
 */
class Issue1839Test extends AbstractIntegrationTest {

    @Test
    @DisabledOnJre({ JRE.JAVA_8, JRE.JAVA_11 })
    void test() {
        performAnalysis("../java17/Issue1839.class");
        assertNoBugType("NP_NONNULL_RETURN_VIOLATION");
    }
}
