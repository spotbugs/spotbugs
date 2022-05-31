package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

public class Issue2040Test extends AbstractIntegrationTest {
    @Test
    public void test() {
        performAnalysis("ghIssues/issue2040/Base.class",
                "ghIssues/issue2040/Derived.class",
                "ghIssues/issue2040/GenericBase.class",
                "ghIssues/issue2040/GenericDerived.class",
                "ghIssues/issue2040/Interface.class",
                "ghIssues/issue2040/Generic.class",
                "ghIssues/issue2040/Caller.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("THROWS_METHOD_THROWS_CLAUSE_THROWABLE")
                .build();
        assertThat(getBugCollection(), containsExactly(3, bugTypeMatcher));

        bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION")
                .build();
        assertThat(getBugCollection(), containsExactly(3, bugTypeMatcher));
    }
}
