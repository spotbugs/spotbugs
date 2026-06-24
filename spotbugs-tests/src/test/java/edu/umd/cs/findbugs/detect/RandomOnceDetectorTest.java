package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class RandomOnceDetectorTest extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("random/RandomOnceTest.class");
        assertBugTypeCount("DMI_RANDOM_USED_ONLY_ONCE", 3);
        assertBugInMethod("DMI_RANDOM_USED_ONLY_ONCE", "RandomOnceTest", "useOnceInIf");
        assertBugInMethod("DMI_RANDOM_USED_ONLY_ONCE", "RandomOnceTest", "useOnceInSwitch");
        assertBugInMethod("DMI_RANDOM_USED_ONLY_ONCE", "RandomOnceTest", "useOnceInEnhancedSwitch");
    }
}
