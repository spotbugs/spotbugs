package edu.umd.cs.findbugs.detect;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.file.Paths;

import org.junit.Rule;
import org.junit.Test;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.test.SpotBugsRule;

public class ResolveMethodReferencesTest {
    @Rule
    public SpotBugsRule spotbugs = new SpotBugsRule();

    /**
     * @see <a href="https://github.com/spotbugs/spotbugs/issues/338">GitHub
     *      issue</a>
     */
    @Test
    public void testIssue338() {
        BugCollection bugCollection = spotbugs.performAnalysis(Paths.get("../spotbugsTestCases/build/classes/java/main/lambdas/Issue338.class"));
        assertThat(bugCollection, is(emptyIterable()));
        assertThat(bugCollection, instanceOf(SortedBugCollection.class));
        assertThat(((SortedBugCollection) bugCollection).missingClassIterator().hasNext(), is(false));
    }
}
