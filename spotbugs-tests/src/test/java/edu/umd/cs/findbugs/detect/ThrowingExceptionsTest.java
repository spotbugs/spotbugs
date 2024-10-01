package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;

class ThrowingExceptionsTest extends AbstractIntegrationTest {

    @Test
    void throwingExceptionsTests() {
        performAnalysis("MethodsThrowingExceptions.class", "MethodsThrowingExceptions$ThrowThrowable.class",
                "MethodsThrowingExceptions$1.class", "MethodsThrowingExceptions$2.class");

        assertBugTypeCount("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION", 1);
        assertBugTypeCount("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", 1);
        assertBugTypeCount("THROWS_METHOD_THROWS_CLAUSE_THROWABLE", 2);

        assertBugInMethod("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION", "MethodsThrowingExceptions", "isCapitalizedThrowingRuntimeException");
        assertNoBugInMethod("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION", "MethodsThrowingExceptions", "isCapitalizedThrowingSpecializedException");

        assertBugInMethod("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", "MethodsThrowingExceptions", "methodThrowingBasicException");
        assertNoBugInMethod("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", "MethodsThrowingExceptions", "methodThrowingIOException");

        assertBugInMethod("THROWS_METHOD_THROWS_CLAUSE_THROWABLE", "MethodsThrowingExceptions", "methodThrowingThrowable");
        assertBugInMethod("THROWS_METHOD_THROWS_CLAUSE_THROWABLE", "MethodsThrowingExceptions$ThrowThrowable", "run");
    }
}
