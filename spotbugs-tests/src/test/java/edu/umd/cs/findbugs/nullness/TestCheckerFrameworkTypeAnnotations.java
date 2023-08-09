package edu.umd.cs.findbugs.nullness;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Before;
import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

/**
 * Checks the effect of CheckerFramework @NonNull annotation on method return value
 */
public class TestCheckerFrameworkTypeAnnotations extends AbstractIntegrationTest {
    @Before
    public void performAnalysis() {
        performAnalysis("type/annotation/CheckerFrameworkTypeAnnotations.class");
    }

    @Test
    public void methodReturns() {
        // A matcher for all NP_NONNULL_RETURN_VIOLATION
        final BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("NP_NONNULL_RETURN_VIOLATION")
                .build();

        assertThat(getBugCollection(), containsExactly(2, matcher));
    }

    @Test
    public void returningNullOnNonNullMethod() {
        final BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("NP_NONNULL_RETURN_VIOLATION")
                .inMethod("returningNullOnNonNullMethod")
                .atLine(13)
                .build();
        assertThat(getBugCollection(), containsExactly(1, matcher));
    }

    @Test
    public void returningNullWithNonNullOnGenerics() {
        final BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("NP_NONNULL_RETURN_VIOLATION")
                .inMethod("returningNullWithNonNullOnGenerics")
                .build();

        assertThat(getBugCollection(), containsExactly(0, matcher));
    }

    @Test
    public void returningNullOnNonNullMethodWithNonNullOnGenerics() {
        final BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("NP_NONNULL_RETURN_VIOLATION")
                .inMethod("returningNullOnNonNullMethodWithNonNullOnGenerics")
                .atLine(25)
                .build();

        assertThat(getBugCollection(), containsExactly(1, matcher));
    }

    @Test
    public void methodParametersTests() {
        // A matcher for all NP_NONNULL_PARAM_VIOLATION
        final BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("NP_NONNULL_PARAM_VIOLATION")
                .build();

        assertThat(getBugCollection(), containsExactly(2, matcher));
    }

    @Test
    public void usingNullForNonNullParameter() {
        final BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("NP_NONNULL_PARAM_VIOLATION")
                .inMethod("usingNullForNonNullParameter")
                .atLine(30)
                .build();

        assertThat(getBugCollection(), containsExactly(1, matcher));
    }

    @Test
    public void usingNullParameterWithNonNullOnGenerics() {
        final BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("NP_NONNULL_PARAM_VIOLATION")
                .inMethod("usingNullParameterWithNonNullOnGenerics")
                .build();

        assertThat(getBugCollection(), containsExactly(0, matcher));
    }

    @Test
    public void usingNullOnNonNullParameterWithNonNullOnGenerics() {
        final BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("NP_NONNULL_PARAM_VIOLATION")
                .inMethod("usingNullOnNonNullParameterWithNonNullOnGenerics")
                .atLine(47)
                .build();

        assertThat(getBugCollection(), containsExactly(1, matcher));
    }

    @Test
    public void usingNonNullArrayOfNullable() {
        final BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
                .inMethod("usingNonNullArrayOfNullable")
                .build();

        assertThat(getBugCollection(), containsExactly(0, matcher));

    }

}
