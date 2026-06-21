package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3644Test extends AbstractIntegrationTest {

    @Test
    void testAbstractMethodsDoNotReportThrowsClause() {
        performAnalysis("ghIssues/Issue3644AbstractMethod.class", "ghIssues/Issue3644InterfaceMethod.class");

        assertBugInMethod("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", "ghIssues.Issue3644AbstractMethod",
                "concreteMethod");
        assertBugInClassCount("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", "ghIssues.Issue3644AbstractMethod", 1);
        assertBugInClassCount("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", "ghIssues.Issue3644InterfaceMethod", 0);
    }
}
