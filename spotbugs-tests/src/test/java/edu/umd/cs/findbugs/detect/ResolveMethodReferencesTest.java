package edu.umd.cs.findbugs.detect;

import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.test.SpotBugsExtension;
import edu.umd.cs.findbugs.test.SpotBugsRunner;

@ExtendWith(SpotBugsExtension.class)
class ResolveMethodReferencesTest {

    /**
     * @see <a href="https://github.com/spotbugs/spotbugs/issues/338">GitHub
     *      issue</a>
     */
    @Test
    void testIssue338(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(Paths.get("../spotbugsTestCases/build/classes/java/main/lambdas/Issue338.class"));
        assertThat(bugCollection, is(emptyIterable()));
        assertThat(bugCollection, instanceOf(SortedBugCollection.class));
        assertThat(((SortedBugCollection) bugCollection).missingClassIterator().hasNext(), is(false));
    }
}
