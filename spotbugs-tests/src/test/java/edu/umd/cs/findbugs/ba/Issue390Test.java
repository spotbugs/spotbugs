package edu.umd.cs.findbugs.ba;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

class Issue390Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue390.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder().bugType("JUA_DONT_ASSERT_INSTANCEOF_IN_TESTS").build();
        MatcherAssert.assertThat(getBugCollection(), containsExactly(1, bugTypeMatcher));
    }

    /**
     * @see <a href="https://github.com/spotbugs/spotbugs/issues/1272">The reported GitHub issue</a>
     */
    @Test
    void testXmlGeneratability() throws IOException {
        performAnalysis("ghIssues/Issue390.class");
        StringWriter writer = new StringWriter();
        getBugCollection().setWithMessages(true);
        getBugCollection().writeXML(writer);
        assertThat(writer.toString(), containsString("JUA_DONT_ASSERT_INSTANCEOF_IN_TESTS"));
    }
}
