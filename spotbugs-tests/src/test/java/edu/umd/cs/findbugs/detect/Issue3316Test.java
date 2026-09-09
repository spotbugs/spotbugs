package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue3316Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("../../../../src/classSamples/ecjSwitchTable/MyClass.class",
                "../../../../src/classSamples/ecjSwitchTable/MyClass$MyEnum.class");

        assertNoBugType("AT_STALE_THREAD_WRITE_OF_PRIMITIVE");
    }
}
