package edu.umd.cs.findbugs.ba;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.SortedBugCollection;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class Issue527Test extends AbstractIntegrationTest {

    @Test
    public void testSimpleLambdas() {
        performAnalysis("lambdas/Issue527.class");
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("NP_NULL_ON_SOME_PATH").build();
        SortedBugCollection bugCollection = (SortedBugCollection) getBugCollection();
        assertThat(bugCollection, containsExactly(2, bugTypeMatcher));
        Iterator<String> missingIter = bugCollection.missingClassIterator();
        List<String> strings = new ArrayList<>();
        missingIter.forEachRemaining(x -> strings.add(x));
        assertEquals(Collections.EMPTY_LIST, strings);
    }

}
