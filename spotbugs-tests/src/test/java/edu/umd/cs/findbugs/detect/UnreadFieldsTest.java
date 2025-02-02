package edu.umd.cs.findbugs.detect;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.Priorities;
import edu.umd.cs.findbugs.test.SpotBugsExtension;
import edu.umd.cs.findbugs.test.SpotBugsRunner;

@ExtendWith(SpotBugsExtension.class)
class UnreadFieldsTest {

    /**
     * {@code UWF_NULL_FIELD} should report the line number of the target field.
     *
     * @see <a href="https://github.com/spotbugs/spotbugs/issues/1368">GitHub Issue</a>
     */
    @Test
    void bugInstanceShouldContainLineNumber(SpotBugsRunner spotbugs) {
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
    void unreadFieldInReflectiveClass(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(Paths.get("../spotbugsTestCases/build/classes/java/main/ghIssues/Issue2325.class"));

        Optional<BugInstance> reportedBug = bugCollection.getCollection().stream()
                .filter(bug -> "UUF_UNUSED_FIELD".equals(bug.getBugPattern().getType())).findAny();
        assertTrue(reportedBug.isPresent(), "Expected unused field bug, but got: " + bugCollection.getCollection());
        assertEquals(Priorities.LOW_PRIORITY, reportedBug.get().getPriority(),
                "Expected low priority unused field bug");

        reportedBug = bugCollection.getCollection().stream()
                .filter(bug -> "URF_UNREAD_FIELD".equals(bug.getBugPattern().getType())).findAny();
        assertTrue(reportedBug.isPresent(), "Expected unread field bug, but got: " + bugCollection.getCollection());
        assertEquals(Priorities.LOW_PRIORITY, reportedBug.get().getPriority(),
                "Expected low priority unread field bug");
    }
}
