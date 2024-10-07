package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class FindPublicAttributesTest extends AbstractIntegrationTest {

    private static final String PRIMITIVE_PUBLIC = "PA_PUBLIC_PRIMITIVE_ATTRIBUTE";
    private static final String MUTABLE_PUBLIC = "PA_PUBLIC_MUTABLE_OBJECT_ATTRIBUTE";
    private static final String ARRAY_PUBLIC = "PA_PUBLIC_ARRAY_ATTRIBUTE";

    @Test
    void testPublicAttributesChecks() {
        performAnalysis("PublicAttributesTest.class");

        assertBugTypeCount(PRIMITIVE_PUBLIC, 3);
        assertBugTypeCount(ARRAY_PUBLIC, 4);
        assertBugTypeCount(MUTABLE_PUBLIC, 1);

        assertBugAtFieldAtLine(PRIMITIVE_PUBLIC, "PublicAttributesTest", "attr1", 11);
        assertBugAtFieldAtLine(PRIMITIVE_PUBLIC, "PublicAttributesTest", "attr2", 42);
        assertBugAtFieldAtLine(PRIMITIVE_PUBLIC, "PublicAttributesTest", "sattr1", 15);
        assertBugAtFieldAtLine(MUTABLE_PUBLIC, "PublicAttributesTest", "hm", 20);
        assertBugAtFieldAtLine(ARRAY_PUBLIC, "PublicAttributesTest", "items", 28);
        assertBugAtFieldAtLine(ARRAY_PUBLIC, "PublicAttributesTest", "nitems", 48);
        assertBugAtFieldAtLine(ARRAY_PUBLIC, "PublicAttributesTest", "sitems", 32);
        assertBugAtFieldAtLine(ARRAY_PUBLIC, "PublicAttributesTest", "SFITEMS", 34);
    }

    @Test
    void testGoodPublicAttributesChecks() {
        performAnalysis("PublicAttributesNegativeTest.class");
        assertBugTypeCount(PRIMITIVE_PUBLIC, 0);
        assertBugTypeCount(ARRAY_PUBLIC, 0);
        assertBugTypeCount(MUTABLE_PUBLIC, 0);
    }
}
