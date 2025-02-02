package edu.umd.cs.findbugs.ba;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/1338">GitHub issue</a>
 */
class Issue1338Test extends AbstractIntegrationTest {

    // These tests require java 11 or better to run
    private static final String[] CLASS_LIST = { "../java11/module-info.class", "../java11/Issue1338.class", };

    /**
     * Test that calling a method call when initializing a try-with-resource variable doesn't result in redundant nullcheck of nonnull value.
     */
    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testMethodCallInTryWithResource() {
        performAnalysis(CLASS_LIST);
        assertNoBugType("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE");
    }
}
