package edu.umd.cs.findbugs.detect;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.file.Paths;
import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.test.SpotBugsRule;

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

    /**
     * {@code URF_UNREAD_FIELD} should be reported also in reflective classes.
     *
     * @see <a href="https://github.com/spotbugs/spotbugs/issues/2325">GitHub Issue</a>
     */
    @Test
    public void unreadFieldInReflectiveClass() {
        BugCollection bugCollection = spotbugs.performAnalysis(Paths.get("../spotbugsTestCases/build/classes/java/main/ghIssues/Issue2325.class"));

        Optional<BugInstance> reportedBug = bugCollection.getCollection().stream()
                .filter(bug -> "UUF_UNUSED_FIELD".equals(bug.getBugPattern().getType())).findAny();
        assertTrue("Expected unused field bug, but got: " + bugCollection.getCollection(), reportedBug.isPresent());
        assertEquals("Expected low priority unused field bug", Priorities.LOW_PRIORITY,
                reportedBug.get().getPriority());

        reportedBug = bugCollection.getCollection().stream()
                .filter(bug -> "URF_UNREAD_FIELD".equals(bug.getBugPattern().getType())).findAny();
        assertTrue("Expected unread field bug, but got: " + bugCollection.getCollection(), reportedBug.isPresent());
        assertEquals("Expected low priority unread field bug", Priorities.LOW_PRIORITY,
                reportedBug.get().getPriority());
    }
}
