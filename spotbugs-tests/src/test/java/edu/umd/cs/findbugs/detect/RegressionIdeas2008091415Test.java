package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

public class RegressionIdeas2008091415Test extends AbstractIntegrationTest {
    @Test
    public void test() {
        performAnalysis("bugIdeas/Ideas_2008_09_14.class", "bugIdeas/Ideas_2008_09_15.class");

        assertBugInMethod("Ideas_2008_09_14", "foo", 6);
        assertBugInMethod("Ideas_2008_09_15", "alternativesToInstanceof", 6);
        assertBugInMethod("Ideas_2008_09_15", "alternativesToInstanceofAndCheckedCast", 12);
    }

    private void assertBugInMethod(String className, String method, int line) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("BC_IMPOSSIBLE_CAST")
                .inClass(className)
                .inMethod(method)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), containsExactly(1, bugInstanceMatcher));
    }
}
