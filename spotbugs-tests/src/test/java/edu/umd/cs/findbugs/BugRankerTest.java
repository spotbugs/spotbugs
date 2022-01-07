package edu.umd.cs.findbugs;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BugRankerTest {
    /**
     * @see <a href="https://github.com/spotbugs/spotbugs/issues/1161">GitHub issue</a>
     */
    @Test
    public void testTrimToMaxRank() {
        SortedBugCollection bugCollection = new SortedBugCollection();
        bugCollection.add(new BugInstance("type", Priorities.HIGH_PRIORITY).addClass("the/target/Class"));
        BugRanker.trimToMaxRank(bugCollection, 0);

        assertTrue(bugCollection.getCollection().isEmpty());
    }
}
