package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

class OverridingMethodMustInvokeSuperTest extends AbstractIntegrationTest {

    @Test
    void testInvokeSuper() {
        performAnalysis(
                "NeedsCallOfSuper.class",
                "NeedsCallOfSuper$DerivedClass.class",
                "NeedsCallOfSuper$ConcreteClass.class",
                "NeedsCallOfSuper$GenericClass.class");
        BugInstanceMatcher bugMatcher = new BugInstanceMatcherBuilder().bugType("OVERRIDING_METHODS_MUST_INVOKE_SUPER").build();
        assertThat(getBugCollection(), containsExactly(1, bugMatcher));
    }

}
