package edu.umd.cs.findbugs.ba;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.SortedBugCollection;

class Issue527Test extends AbstractIntegrationTest {

    @Test
    void testSimpleLambdas() {
        performAnalysis("lambdas/Issue527.class");
        assertBugTypeCount("NP_NULL_ON_SOME_PATH", 2);
        SortedBugCollection bugCollection = (SortedBugCollection) getBugCollection();
        Iterator<String> missingIter = bugCollection.missingClassIterator();
        List<String> strings = new ArrayList<>();
        missingIter.forEachRemaining(x -> strings.add(x));
        assertEquals(Collections.EMPTY_LIST, strings);
    }
}
