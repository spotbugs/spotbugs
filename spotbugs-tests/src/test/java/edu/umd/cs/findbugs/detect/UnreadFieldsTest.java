package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.test.SpotBugsRule;
import org.junit.Rule;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;

public class UnreadFieldsTest {
    @Rule
    public SpotBugsRule spotbugs = new SpotBugsRule();

    /**
     * {@code UWF_NULL_FIELD} should report the line number of the target field.
     *
     * @see <a href="https://github.com/spotbugs/spotbugs/issues/1368">GitHub Issue</a>
     */
    @Test
    public void bugInstanceShouldContainLineNumber() {
        BugCollection bugCollection = spotbugs.performAnalysis(Paths.get("../spotbugsTestCases/build/classes/java/main/ghIssues/Issue1368.class"));
        Optional<BugInstance> reportedBug = bugCollection.getCollection().stream()
                .filter(bug -> "UWF_NULL_FIELD".equals(bug.getBugPattern().getType()))
                .findAny();
        assertTrue(reportedBug.isPresent());
        assertThat(reportedBug.get().getPrimarySourceLineAnnotation().getStartLine(), is(not(-1)));
    }
}
