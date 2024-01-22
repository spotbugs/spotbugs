package edu.umd.cs.findbugs.nullness;

import java.nio.file.Paths;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.SpotBugsExtension;
import edu.umd.cs.findbugs.test.SpotBugsRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;


/**
 * Unit test to reproduce <a href="https://github.com/spotbugs/spotbugs/issues/456">#456</a>.
 */
@ExtendWith(SpotBugsExtension.class)
class IssueRequireNotNullTest {

    @Test
    void testIssue(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/nullnessAnnotations/TestNonNull7.class"));
        assertThat(bugCollection, emptyIterable());
    }
}
