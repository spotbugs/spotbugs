package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

public class RegressionIdeas20111219Test extends AbstractIntegrationTest {
    @Test
    public void test() {
        performAnalysis("bugIdeas/Ideas_2011_12_19.class");

        assertNoNpBugInMethod("f");
        assertBugInMethod("NP_ALWAYS_NULL", "f2", 21);
        assertBugInMethod("NP_LOAD_OF_KNOWN_NULL_VALUE", "f2", 21);
        assertNoNpBugInMethod("f3");
        assertNoNpBugInMethod("f4");
        assertBugInMethod("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "f5a", 48);
        assertNoNpBugInMethod("f5b");
        assertNoNpBugInMethod("f5c");
        assertNoNpBugInMethod("f5e");
        assertNoNpBugInMethod("f5f");
        assertBugInMethod("NP_NULL_ON_SOME_PATH", "f5g", 81);
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
            "NP_GUARANTEED_DEREF_ON_EXCEPTION_PATH", "NP_NULL_INSTANCEOF", "BC_NULL_INSTANCEOF",
            "NP_METHOD_RETURN_RELAXING_ANNOTATION", "NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION",
            "NP_METHOD_PARAMETER_RELAXING_ANNOTATION",
        };
        for (int i = 0; i < bugtypes.length; i++) {
            final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                    .bugType(bugtypes[i])
                    .inClass("Bug3277814")
                    .inMethod(method)
                    .build();
            assertThat(getBugCollection(), containsExactly(0, bugInstanceMatcher));
        }
    }

    private void assertBugInMethod(String bugtype, String method, int line) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype)
                .inClass("Ideas_2011_12_19")
                .inMethod(method)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }
}
