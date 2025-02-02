package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class RegressionIdeas2008091415Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2008_09_14.class", "bugIdeas/Ideas_2008_09_15.class");

        assertBugInMethodAtLine("BC_IMPOSSIBLE_CAST", "Ideas_2008_09_14", "foo", 6);
        assertBugInMethodAtLine("BC_IMPOSSIBLE_CAST", "Ideas_2008_09_15", "alternativesToInstanceof", 6);
        assertBugInMethodAtLine("BC_IMPOSSIBLE_CAST", "Ideas_2008_09_15", "alternativesToInstanceofAndCheckedCast", 12);
    }
}
