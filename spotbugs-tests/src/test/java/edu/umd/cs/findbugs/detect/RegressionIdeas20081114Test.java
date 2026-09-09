package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;


class RegressionIdeas20081114Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2008_11_14.class");

        assertBugTypeCountBetween("GC_UNRELATED_TYPES", 3, 9);
        assertBugTypeCount("EC_UNRELATED_CLASS_AND_INTERFACE", 1);
        assertBugTypeCount("EC_UNRELATED_TYPES", 2);

        assertBugInMethodAtLine("GC_UNRELATED_TYPES", "Ideas_2008_11_14", "foo", 32);
        assertBugInMethodAtLine("GC_UNRELATED_TYPES", "Ideas_2008_11_14", "foo", 33);
        assertBugInMethodAtLine("GC_UNRELATED_TYPES", "Ideas_2008_11_14", "foo", 34);
        assertBugInMethodAtLine("EC_UNRELATED_CLASS_AND_INTERFACE", "Ideas_2008_11_14", "foo", 36);
        // assertBugInMethodAtLine("GC_UNRELATED_TYPES", "Ideas_2008_11_14", "foo", 37); // FN
        assertBugInMethodAtLine("EC_UNRELATED_TYPES", "Ideas_2008_11_14", "foo", 38);
        assertBugInMethodAtLine("EC_UNRELATED_TYPES", "Ideas_2008_11_14", "foo", 39);

        // assertBugInMethodAtLine("GC_UNRELATED_TYPES", "Ideas_2008_11_14", "testOne", 17); // FN
        // assertBugInMethodAtLine("GC_UNRELATED_TYPES", "Ideas_2008_11_14", "testOne", 18); // FN
        // assertBugInMethodAtLine("GC_UNRELATED_TYPES", "Ideas_2008_11_14", "testOne", 19); // FN
        // assertBugInMethodAtLine("GC_UNRELATED_TYPES", "Ideas_2008_11_14", "testOne", 20); // FN
        // assertBugInMethodAtLine("GC_UNRELATED_TYPES", "Ideas_2008_11_14", "testOne", 21); // FN
    }
}
