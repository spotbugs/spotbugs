package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;


class RegressionIdeas20090430Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2009_04_30.class");
        assertBugTypeCount("EC_NULL_ARG", 1);
        assertBugTypeCount("BC_IMPOSSIBLE_CAST", 1);
        assertBugTypeCount("BC_IMPOSSIBLE_DOWNCAST", 1);
        assertBugTypeCount("BC_IMPOSSIBLE_DOWNCAST_OF_TOARRAY", 1);

        assertBugInMethodAtLine("EC_NULL_ARG", "Ideas_2009_04_30", "main", 12);
        assertBugInMethodAtLine("BC_IMPOSSIBLE_DOWNCAST", "Ideas_2009_04_30", "main", 14);
        assertBugInMethodAtLine("BC_IMPOSSIBLE_DOWNCAST_OF_TOARRAY", "Ideas_2009_04_30", "main", 18);
        assertBugInMethodAtLine("BC_IMPOSSIBLE_CAST", "Ideas_2009_04_30", "main", 20);
    }
}
