package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class CloneIdiomTest extends AbstractIntegrationTest {
    @Test
    void testCloneIdiom() {
        performAnalysis("CloneIdiom1.class", "CloneIdiom2.class", "CloneIdiom3.class");

        assertBugTypeCount("CN_IDIOM", 1);
        assertBugTypeCount("CN_IDIOM_NO_SUPER_CALL", 1);

        assertBugInClass("CN_IDIOM", "CloneIdiom1");
        assertBugInClass("CN_IDIOM_NO_SUPER_CALL", "CloneIdiom3");
    }
}
