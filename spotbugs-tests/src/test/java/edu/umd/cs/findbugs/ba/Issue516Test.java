package edu.umd.cs.findbugs.ba;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.LocalVariableAnnotation;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

class Issue516Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue516.class");
        // Both ecj/javac compilers as of today do not generate LVT entry for unused variables in some cases.
        // I would consider this as a compiler bug, so may be it will be fixed some time later.
        // In this case the test will fail and would need to be adopted.
        // Note that in case the bug will be fixed for some compilers, the assumption will vary for different
        // compiler and different compiler versions.
        String variableName = LocalVariableAnnotation.UNKNOWN_NAME;
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder().bugType("DLS_DEAD_LOCAL_STORE")
                .atVariable(variableName).build();
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcher));
    }

}
