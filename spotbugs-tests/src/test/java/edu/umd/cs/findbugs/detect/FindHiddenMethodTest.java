package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;

class FindHiddenMethodTest extends AbstractIntegrationTest {
    private static final String BUG_TYPE = "HSM_HIDING_METHOD";

    @Test
    void testBadFindHiddenMethodTest() {
        performAnalysis("findhiddenmethodtest/GrantAccessStatic.class",
                "findhiddenmethodtest/BadFindHiddenMethodTest.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "BadFindHiddenMethodTest", "displayAccountStatus", 15);
    }

    @Test
    void testBadFindHiddenMethodTest2() {
        performAnalysis("findhiddenmethodtest/BadSuperClass.class",
                "findhiddenmethodtest/BadHidingVsOverriding.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "BadHidingVsOverriding", "methodHiding", 19);
    }


    @Test
    void testBadFindHiddenMethodTest3() {
        performAnalysis("findhiddenmethodtest/SuperBadInEqualMultipleMethod.class",
                "findhiddenmethodtest/BadInEqualMultipleStaticMethod.class");
        assertBugTypeCount(BUG_TYPE, 2);
        assertBugInMethodAtLine(BUG_TYPE, "BadInEqualMultipleStaticMethod", "display", 25);
        assertBugInMethodAtLine(BUG_TYPE, "BadInEqualMultipleStaticMethod", "display2", 29);
    }

    @Test
    void testBadFindHiddenMethodTest4() {
        performAnalysis("findhiddenmethodtest/BadMainMethodCheck.class",
                "findhiddenmethodtest/BadSuperClassWithMain.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "BadMainMethodCheck", "main", 18);
    }

    @Test
    void testBadFindHiddenMethodTest5() {
        performAnalysis("findhiddenmethodtest/BadGrandClass.class",
                "findhiddenmethodtest/BadParentClass.class",
                "findhiddenmethodtest/BadMultiLevelInheritance.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "BadMultiLevelInheritance", "display", 20);
    }

    @Test
    void testBadFindHiddenMethodTest6() {
        performAnalysis("findhiddenmethodtest/SuperBadMultipleMethods.class",
                "findhiddenmethodtest/BadMultipleStaticMethods.class");
        assertBugTypeCount(BUG_TYPE, 2);
        assertBugInMethodAtLine(BUG_TYPE, "BadMultipleStaticMethods", "display", 19);
        assertBugInMethodAtLine(BUG_TYPE, "BadMultipleStaticMethods", "display2", 23);
    }

    @Test
    void testBadFindHiddenMethodTest7() {
        performAnalysis("findhiddenmethodtest/SuperBadNonOrderedMultipleStaticMethods.class",
                "findhiddenmethodtest/BadNonOrderedMultipleStaticMethods.class");
        assertBugTypeCount(BUG_TYPE, 2);
        assertBugInMethodAtLine(BUG_TYPE, "BadNonOrderedMultipleStaticMethods", "display", 24);
        assertBugInMethodAtLine(BUG_TYPE, "BadNonOrderedMultipleStaticMethods", "display2", 28);
    }

    @Test
    void testBadFindHiddenMethodTest8() {
        performAnalysis("findhiddenmethodtest/BadOuterInnerClass.class",
                "findhiddenmethodtest/BadOuterInnerClass$BadInner.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "BadOuterInnerClass$BadInner", "badMethod", 15);
    }

    @Test
    void testBadFindHiddenMethodTest9() {
        performAnalysis("findhiddenmethodtest/SuperBadProtected.class",
                "findhiddenmethodtest/BadProtected.class");
        assertBugTypeCount(BUG_TYPE, 1);
        assertBugInMethodAtLine(BUG_TYPE, "BadProtected", "display", 15);
    }

    @Test
    void testGoodFindHiddenMethodTest() {
        performAnalysis("findhiddenmethodtest/GrantAccess.class",
                "findhiddenmethodtest/GoodFindHiddenMethodTest.class");
        assertNoBugType(BUG_TYPE);
    }

    @Test
    void testGoodFindHiddenMethodTest2() {
        performAnalysis("findhiddenmethodtest/SuperGoodInEqualMultipleMethod.class",
                "findhiddenmethodtest/GoodInEqualNonStaticMethods.class");
        assertNoBugType(BUG_TYPE);
    }

    @Test
    void testGoodFindHiddenMethodTest3() {
        performAnalysis("findhiddenmethodtest/GoodSuperClassWithMain.class",
                "findhiddenmethodtest/GoodMainMethodCheck.class");
        assertNoBugType(BUG_TYPE);
    }

    @Test
    void testGoodFindHiddenMethodTest4() {
        performAnalysis("findhiddenmethodtest/GoodGrandNonStatic.class",
                "findhiddenmethodtest/GoodParentNonStatic.class",
                "findhiddenmethodtest/GoodMultiLevelInheritanceNonStatic.class");
        assertNoBugType(BUG_TYPE);
    }

    @Test
    void testGoodFindHiddenMethodTest5() {
        performAnalysis("findhiddenmethodtest/GoodGrandClassPrivate.class",
                "findhiddenmethodtest/GoodParentClassPrivate.class",
                "findhiddenmethodtest/GoodMultiLevelInheritancePrivate.class");
        assertNoBugType(BUG_TYPE);
    }

    @Test
    void testGoodFindHiddenMethodTest6() {
        performAnalysis("findhiddenmethodtest/SuperMultipleNonStatic.class",
                "findhiddenmethodtest/GoodMultipleNonStaticMethods.class");
        assertNoBugType(BUG_TYPE);
    }

    @Test
    void testGoodFindHiddenMethodTest7() {
        performAnalysis("findhiddenmethodtest/SuperGoodMultipleStaticMethod.class",
                "findhiddenmethodtest/GoodMultipleStaticMethod.class");
        assertNoBugType(BUG_TYPE);
    }

    @Test
    void testGoodFindHiddenMethodTest8() {
        performAnalysis("findhiddenmethodtest/SuperGoodNonOrderedMultipleStaticMethods.class",
                "findhiddenmethodtest/GoodNonOrderedMultipleNonStaticMethods.class");
        assertNoBugType(BUG_TYPE);
    }

    @Test
    void testGoodFindHiddenMethodTest9() {
        performAnalysis("findhiddenmethodtest/GoodOuterInnerClassNonStatic.class",
                "findhiddenmethodtest/GoodOuterInnerClassNonStatic$GoodInnerNonStaticGood.class");
        assertNoBugType(BUG_TYPE);
    }

    @Test
    void testGoodFindHiddenMethodTest10() {
        performAnalysis("findhiddenmethodtest/GoodOuterInnerClassPrivate.class",
                "findhiddenmethodtest/GoodOuterInnerClassPrivate$GoodInnerClassPrivate.class");
        assertNoBugType(BUG_TYPE);
    }

    @Test
    void testGoodFindHiddenMethodTest11() {
        performAnalysis("findhiddenmethodtest/SuperGoodProtected.class",
                "findhiddenmethodtest/GoodProtected.class");
        assertNoBugType(BUG_TYPE);
    }
}
