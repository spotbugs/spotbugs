package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Bug3107916Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("sfBugs/Bug3107916.class");

        assertBugInMethodAtLine("NP_NULL_ON_SOME_PATH_MIGHT_BE_INFEASIBLE", "Bug3107916", "doCompare", 20);
        assertNoNpBugInMethod("doCompare2");
    }

    private void assertNoNpBugInMethod(String method) {
        final String[] bugtypes = new String[] {
            "NP_SYNC_AND_NULL_CHECK_FIELD", "NP_BOOLEAN_RETURN_NULL", "NP_OPTIONAL_RETURN_NULL",
            "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", "NP_ARGUMENT_MIGHT_BE_NULL",
            "NP_EQUALS_SHOULD_HANDLE_NULL_ARGUMENT", "NP_DEREFERENCE_OF_READLINE_VALUE",
            "NP_IMMEDIATE_DEREFERENCE_OF_READLINE", "NP_UNWRITTEN_FIELD", "NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD",
            "NP_ALWAYS_NULL", "NP_CLOSING_NULL", "NP_STORE_INTO_NONNULL_FIELD", "NP_ALWAYS_NULL_EXCEPTION",
            "NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
            "NP_NULL_ON_SOME_PATH_MIGHT_BE_INFEASIBLE", "NP_NULL_ON_SOME_PATH", "NP_NULL_ON_SOME_PATH_EXCEPTION",
            "NP_NULL_PARAM_DEREF", "NP_NULL_PARAM_DEREF_NONVIRTUAL", "NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS",
            "NP_NONNULL_PARAM_VIOLATION", "NP_NONNULL_RETURN_VIOLATION", "NP_TOSTRING_COULD_RETURN_NULL",
            "NP_CLONE_COULD_RETURN_NULL", "NP_LOAD_OF_KNOWN_NULL_VALUE", "NP_GUARANTEED_DEREF",
            "NP_GUARANTEED_DEREF_ON_EXCEPTION_PATH", "NP_NULL_INSTANCEOF",
            "NP_METHOD_RETURN_RELAXING_ANNOTATION", "NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION",
            "NP_METHOD_PARAMETER_RELAXING_ANNOTATION",
        };
        for (String bugtype : bugtypes) {
            assertNoBugInMethod(bugtype, "Bug3107916", method);
        }
    }
}
