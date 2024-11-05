package edu.umd.cs.findbugs.ba;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.LocalVariableAnnotation;

class Issue516Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue516.class");
        // Both ecj/javac compilers as of today do not generate LVT entry for unused variables in some cases.
        // I would consider this as a compiler bug, so may be it will be fixed some time later.
        // In this case the test will fail and would need to be adopted.
        // Note that in case the bug will be fixed for some compilers, the assumption will vary for different
        // compiler and different compiler versions.
        assertBugAtVar("DLS_DEAD_LOCAL_STORE", "Issue516", "missingLvtEntry", LocalVariableAnnotation.UNKNOWN_NAME, 10);
    }
}
