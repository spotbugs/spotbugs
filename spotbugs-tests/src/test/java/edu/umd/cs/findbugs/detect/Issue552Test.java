package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.test.SpotBugsRule;
import org.junit.Rule;
import org.junit.Test;

import java.nio.file.Paths;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/552">GitHub
 *      issue 552</a>
 */
public class Issue552Test extends AbstractIntegrationTest {
    @Rule
    public SpotBugsRule spotbugs = new SpotBugsRule();

    @Test
    public void testIssue552_1() {
        assertNoProblem(spotbugs.performAnalysis(Paths.get("../spotbugsTestCases/build/classes/java/main/lambdas/Issue552_1.class")));
    }

    @Test
    public void testIssue552_2() {
        assertNoProblem(spotbugs.performAnalysis(Paths.get("../spotbugsTestCases/build/classes/java/main/lambdas/Issue552_2.class")));
    }

    private void assertNoProblem(BugCollection bugCollection) {
        assertThat(bugCollection, is(emptyIterable()));
        assertThat(bugCollection, instanceOf(SortedBugCollection.class));
        assertThat(((SortedBugCollection) bugCollection).missingClassIterator().hasNext(), is(false));
    }
}
