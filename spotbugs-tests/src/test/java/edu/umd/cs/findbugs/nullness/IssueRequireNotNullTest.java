package edu.umd.cs.findbugs.nullness;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.BugCollection;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;


/**
 * Unit test to reproduce <a href="https://github.com/spotbugs/spotbugs/issues/456">#456</a>.
 */
class IssueRequireNotNullTest extends AbstractIntegrationTest {
    final String[] bugtypes = new String[] {
        "NP_SYNC_AND_NULL_CHECK_FIELD", "NP_BOOLEAN_RETURN_NULL", "NP_OPTIONAL_RETURN_NULL",
        "NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", "NP_ARGUMENT_MIGHT_BE_NULL",
        "NP_EQUALS_SHOULD_HANDLE_NULL_ARGUMENT", "NP_DEREFERENCE_OF_READLINE_VALUE",
        "NP_IMMEDIATE_DEREFERENCE_OF_READLINE", "NP_UNWRITTEN_FIELD", "NP_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD",
        "NP_ALWAYS_NULL", "NP_CLOSING_NULL", "NP_STORE_INTO_NONNULL_FIELD", "NP_ALWAYS_NULL_EXCEPTION",
        "NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", "NP_NULL_ON_SOME_PATH_MIGHT_BE_INFEASIBLE",
        "NP_NULL_ON_SOME_PATH", "NP_NULL_ON_SOME_PATH_EXCEPTION", "NP_NULL_PARAM_DEREF", "NP_NULL_PARAM_DEREF_NONVIRTUAL",
        "NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS", "NP_NONNULL_PARAM_VIOLATION", "NP_NONNULL_RETURN_VIOLATION",
        "NP_TOSTRING_COULD_RETURN_NULL", "NP_CLONE_COULD_RETURN_NULL", "NP_LOAD_OF_KNOWN_NULL_VALUE", "NP_GUARANTEED_DEREF",
        "NP_GUARANTEED_DEREF_ON_EXCEPTION_PATH", "NP_NULL_INSTANCEOF", "BC_NULL_INSTANCEOF",
        "NP_METHOD_RETURN_RELAXING_ANNOTATION", "NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION",
        "NP_METHOD_PARAMETER_RELAXING_ANNOTATION",
    };

    @Test
    void testIssue() {
        performAnalysis("nullnessAnnotations/TestNonNull7.class");

        assertHasNoNpBug(getBugCollection());
        assertBugNum(getBugCollection(), "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", 1);
        assertBug(getBugCollection(), "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "objectsrequireNonNullFalse");
    }

    private void assertHasNoNpBug(BugCollection bugCollection) {
        for (String bugtype : bugtypes) {
            assertBugNum(bugCollection, bugtype, 0);
        }
    }

    private void assertBugNum(BugCollection bugCollection, String bugType, int bugNum) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .build();
        assertThat(bugCollection, containsExactly(bugNum, bugInstanceMatcher));
    }

    private void assertBug(BugCollection bugCollection, String bugType, String method) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inMethod(method)
                .build();
        assertThat(bugCollection, containsExactly(1, bugInstanceMatcher));
    }
}
