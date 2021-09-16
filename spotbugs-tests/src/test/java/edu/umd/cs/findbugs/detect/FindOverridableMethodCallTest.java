package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.Matchers.hasItem;

import org.apache.bcel.Const;

import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class FindOverridableMethodCallTest extends AbstractIntegrationTest {
    @Test
    public void testOverridableMethodCalls() {
        performAnalysis("OverridableMethodCall.class");

        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR").build();
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcher));

        bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("MC_OVERRIDABLE_METHOD_CALL_IN_CLONE").build();
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcher));

        BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR")
                .inClass("OverridableMethodCall")
                .inMethod(Const.CONSTRUCTOR_NAME)
                .atLine(3)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));

        bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("MC_OVERRIDABLE_METHOD_CALL_IN_CLONE")
                .inClass("OverridableMethodCall")
                .inMethod("clone")
                .atLine(16)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }
}
