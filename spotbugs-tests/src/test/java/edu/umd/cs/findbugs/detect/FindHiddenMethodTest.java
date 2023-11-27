package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

class FindHiddenMethodTest extends AbstractIntegrationTest {
    private final String BUG_TYPE = "HSM_HIDING_METHOD";

    @Test
    void testBadFindHiddenMethodTest() {
        performAnalysis("findhiddenmethodtest/GrantAccessStatic.class",
                "findhiddenmethodtest/BadFindHiddenMethodTest.class");
        assertHSBCBug("displayAccountStatus", 15);
    }

    @Test
    void testBadFindHiddenMethodTest2() {
        performAnalysis("findhiddenmethodtest/BadSuperClass.class",
                "findhiddenmethodtest/BadHidingVsOverriding.class");
        assertHSBCBug("methodHiding", 19);
    }


    @Test
    void testBadFindHiddenMethodTest3() {
        performAnalysis("findhiddenmethodtest/SuperBadInEqualMultipleMethod.class",
                "findhiddenmethodtest/BadInEqualMultipleStaticMethod.class");
        assertHSBCBug("display", 25);
        assertHSBCBug("display2", 29);
    }

    @Test
    void testBadFindHiddenMethodTest4() {
        performAnalysis("findhiddenmethodtest/BadMainMethodCheck.class",
                "findhiddenmethodtest/BadSuperClassWithMain.class");
        assertHSBCBug("main", 18);
    }

    @Test
    void testBadFindHiddenMethodTest5() {
        performAnalysis("findhiddenmethodtest/BadGrandClass.class",
                "findhiddenmethodtest/BadParentClass.class",
                "findhiddenmethodtest/BadMultiLevelInheritance.class");
        assertHSBCBug("display", 20);
    }

    @Test
    void testBadFindHiddenMethodTest6() {
        performAnalysis("findhiddenmethodtest/SuperBadMultipleMethods.class",
                "findhiddenmethodtest/BadMultipleStaticMethods.class");
        assertHSBCBug("display", 19);
        assertHSBCBug("display2", 23);
    }

    @Test
    void testBadFindHiddenMethodTest7() {
        performAnalysis("findhiddenmethodtest/SuperBadNonOrderedMultipleStaticMethods.class",
                "findhiddenmethodtest/BadNonOrderedMultipleStaticMethods.class");
        assertHSBCBug("display", 24);
        assertHSBCBug("display2", 28);
    }

    @Test
    void testBadFindHiddenMethodTest8() {
        performAnalysis("findhiddenmethodtest/BadOuterInnerClass.class",
                "findhiddenmethodtest/BadOuterInnerClass$BadInner.class");
        assertHSBCBug("badMethod", 15);
    }

    @Test
    void testBadFindHiddenMethodTest9() {
        performAnalysis("findhiddenmethodtest/SuperBadProtected.class",
                "findhiddenmethodtest/BadProtected.class");
        assertHSBCBug("display", 15);
    }

    @Test
    void testGoodFindHiddenMethodTest() {
        performAnalysis("findhiddenmethodtest/GrantAccess.class",
                "findhiddenmethodtest/GoodFindHiddenMethodTest.class");
        assertNoHSBCBug();
    }

    @Test
    void testGoodFindHiddenMethodTest2() {
        performAnalysis("findhiddenmethodtest/SuperGoodInEqualMultipleMethod.class",
                "findhiddenmethodtest/GoodInEqualNonStaticMethods.class");
        assertNoHSBCBug();
    }

    @Test
    void testGoodFindHiddenMethodTest3() {
        performAnalysis("findhiddenmethodtest/GoodSuperClassWithMain.class",
                "findhiddenmethodtest/GoodMainMethodCheck.class");
        assertNoHSBCBug();
    }

    @Test
    void testGoodFindHiddenMethodTest4() {
        performAnalysis("findhiddenmethodtest/GoodGrandNonStatic.class",
                "findhiddenmethodtest/GoodParentNonStatic.class",
                "findhiddenmethodtest/GoodMultiLevelInheritanceNonStatic.class");
        assertNoHSBCBug();
    }

    @Test
    void testGoodFindHiddenMethodTest5() {
        performAnalysis("findhiddenmethodtest/GoodGrandClassPrivate.class",
                "findhiddenmethodtest/GoodParentClassPrivate.class",
                "findhiddenmethodtest/GoodMultiLevelInheritancePrivate.class");
        assertNoHSBCBug();
    }

    @Test
    void testGoodFindHiddenMethodTest6() {
        performAnalysis("findhiddenmethodtest/SuperMultipleNonStatic.class",
                "findhiddenmethodtest/GoodMultipleNonStaticMethods.class");
        assertNoHSBCBug();
    }

    @Test
    void testGoodFindHiddenMethodTest7() {
        performAnalysis("findhiddenmethodtest/SuperGoodMultipleStaticMethod.class",
                "findhiddenmethodtest/GoodMultipleStaticMethod.class");
        assertNoHSBCBug();
    }

    @Test
    void testGoodFindHiddenMethodTest8() {
        performAnalysis("findhiddenmethodtest/SuperGoodNonOrderedMultipleStaticMethods.class",
                "findhiddenmethodtest/GoodNonOrderedMultipleNonStaticMethods.class");
        assertNoHSBCBug();
    }

    @Test
    void testGoodFindHiddenMethodTest9() {
        performAnalysis("findhiddenmethodtest/GoodOuterInnerClassNonStatic.class",
                "findhiddenmethodtest/GoodOuterInnerClassNonStatic$GoodInnerNonStaticGood.class");
        assertNoHSBCBug();
    }

    @Test
    void testGoodFindHiddenMethodTest10() {
        performAnalysis("findhiddenmethodtest/GoodOuterInnerClassPrivate.class",
                "findhiddenmethodtest/GoodOuterInnerClassPrivate$GoodInnerClassPrivate.class");
        assertNoHSBCBug();
    }

    @Test
    void testGoodFindHiddenMethodTest11() {
        performAnalysis("findhiddenmethodtest/SuperGoodProtected.class",
                "findhiddenmethodtest/GoodProtected.class");
        assertNoHSBCBug();
    }

    private void assertNoHSBCBug() {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(BUG_TYPE)
                .build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }

    private void assertHSBCBug(String methodName, int lineNumber) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(BUG_TYPE)
                .inMethod(methodName)
                .atLine(lineNumber)
                .build();
        assertThat(getBugCollection(), hasItem(bugTypeMatcher));
    }
}
