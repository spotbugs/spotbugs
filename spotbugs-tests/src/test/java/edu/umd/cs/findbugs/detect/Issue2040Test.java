package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue2040Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("ghIssues/issue2040/Base.class",
                "ghIssues/issue2040/Derived.class",
                "ghIssues/issue2040/GenericBase.class",
                "ghIssues/issue2040/GenericDerived.class",
                "ghIssues/issue2040/Interface.class",
                "ghIssues/issue2040/Generic.class",
                "ghIssues/issue2040/Caller.class");

        assertBugTypeCount("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION", 3);
        assertBugInMethodAtLine("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION", "Derived", "lambda$lambda2$1", 36);
        assertBugInMethodAtLine("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION", "Derived", "runtimeExThrownFromCatch", 97);
        assertBugInMethodAtLine("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION", "Derived", "runtimeExceptionThrowingMethod", 45);

        assertBugTypeCount("THROWS_METHOD_THROWS_CLAUSE_THROWABLE", 4);
        assertBugInMethod("THROWS_METHOD_THROWS_CLAUSE_THROWABLE", "Base", "method");
        assertBugInMethod("THROWS_METHOD_THROWS_CLAUSE_THROWABLE", "Derived", "throwableThrowingMethod");
        assertBugInMethod("THROWS_METHOD_THROWS_CLAUSE_THROWABLE", "GenericBase", "method1");
        assertBugInMethod("THROWS_METHOD_THROWS_CLAUSE_THROWABLE", "GenericBase", "method2");

        assertBugTypeCount("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", 5);
        assertBugInMethod("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", "Derived", "exceptionThrowingMethod");
        assertBugInMethod("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", "Derived", "exThrownFromCatch");
        assertBugInMethod("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", "GenericBase", "method");
        assertBugInMethod("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", "GenericBase", "method2");
        assertBugInMethod("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", "Interface", "iMethod");
    }
}
