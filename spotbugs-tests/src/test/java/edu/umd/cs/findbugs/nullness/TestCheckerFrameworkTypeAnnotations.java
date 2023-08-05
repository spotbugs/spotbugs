package edu.umd.cs.findbugs.nullness;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

/**
 * Checks the effect of CheckerFramework @NonNull annotation on method return value
 */
public class TestCheckerFrameworkTypeAnnotations extends AbstractIntegrationTest {

    @Test
    public void test() {
        performAnalysis("type/annotation/CheckerFrameworkTypeAnnotations.class");

        // A matcher for all NP_NONNULL_RETURN_VIOLATION
        final BugInstanceMatcher matcherMethodReturns = new BugInstanceMatcherBuilder()
                .bugType("NP_NONNULL_RETURN_VIOLATION")
                .build();

        // Per-method NP_NONNULL_RETURN_VIOLATION matchers
        final BugInstanceMatcher matcherReturningNullOnNonNullMethod = new BugInstanceMatcherBuilder()
                .bugType("NP_NONNULL_RETURN_VIOLATION")
                .inMethod("returningNullOnNonNullMethod")
                .build();

        final BugInstanceMatcher matcherReturningNullWithNonNullOnGenerics = new BugInstanceMatcherBuilder()
                .bugType("NP_NONNULL_RETURN_VIOLATION")
                .inMethod("returningNullWithNonNullOnGenerics")
                .build();

        final BugInstanceMatcher matcherReturningNullOnNonNullMethodWithNonNullOnGenerics = new BugInstanceMatcherBuilder()
                .bugType("NP_NONNULL_RETURN_VIOLATION")
                .inMethod("returningNullOnNonNullMethodWithNonNullOnGenerics")
                .build();

        // A matcher for all NP_NONNULL_PARAM_VIOLATION
        final BugInstanceMatcher matcherMethodParameters = new BugInstanceMatcherBuilder()
                .bugType("NP_NONNULL_PARAM_VIOLATION")
                .build();

        // Per-method NP_NONNULL_PARAM_VIOLATION matchers
        final BugInstanceMatcher matcherNonNullParameter = new BugInstanceMatcherBuilder()
                .bugType("NP_NONNULL_PARAM_VIOLATION")
                .inMethod("usingNullForNonNullParameter")
                .build();

        final BugInstanceMatcher matcherParameterWithNonNullOnGenerics = new BugInstanceMatcherBuilder()
                .bugType("NP_NONNULL_PARAM_VIOLATION")
                .inMethod("usingNullParameterWithNonNullOnGenerics")
                .build();

        final BugInstanceMatcher matcherNonNullParameterWithNonNullOnGenerics = new BugInstanceMatcherBuilder()
                .bugType("NP_NONNULL_PARAM_VIOLATION")
                .inMethod("usingNullOnNonNullParameterWithNonNullOnGenerics")
                .build();

        assertThat(getBugCollection(), containsExactly(1, matcherReturningNullOnNonNullMethod));
        assertThat(getBugCollection(), containsExactly(0, matcherReturningNullWithNonNullOnGenerics));
        assertThat(getBugCollection(), containsExactly(1, matcherReturningNullOnNonNullMethodWithNonNullOnGenerics));

        assertThat(getBugCollection(), containsExactly(2, matcherMethodReturns));


        assertThat(getBugCollection(), containsExactly(1, matcherNonNullParameter));
        assertThat(getBugCollection(), containsExactly(0, matcherParameterWithNonNullOnGenerics));
        assertThat(getBugCollection(), containsExactly(1, matcherNonNullParameterWithNonNullOnGenerics));

        assertThat(getBugCollection(), containsExactly(2, matcherMethodParameters));

    }

}
