package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue2120Test extends AbstractIntegrationTest {

    @Test
    @EnabledForJreRange(min = JRE.JAVA_17)
    void testIssue() {
        performAnalysis("../java17/Issue2120.class",
                "../java17/Issue2120$1MyEnum.class",
                "../java17/Issue2120$1MyRecord.class");
        assertNoBugType("UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS");
    }
}
