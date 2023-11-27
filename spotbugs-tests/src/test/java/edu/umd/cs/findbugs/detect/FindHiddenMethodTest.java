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
    void testBadFindHidingSubClassTest() {
        performAnalysis("findhiddenmethodtest/GrantAccessStatic.class",
                "findhiddenmethodtest/BadFindHidingSubClassTest.class");
        assertHSBCBug("displayAccountStatus", 15);
    }

    @Test
    void testBadFindHidingSubClassTest2() {
        performAnalysis("findhiddenmethodtest/BadSuperClass.class",
                "findhiddenmethodtest/BadHidingVsOverriding.class");
        assertHSBCBug("methodHiding", 19);
    }


    @Test
    void testBadFindHidingSubClassTest3() {
        performAnalysis("findhiddenmethodtest/SuperBadInEqualMultipleMethod.class",
                "findhiddenmethodtest/BadInEqualMultipleStaticMethod.class");
        assertHSBCBug("display", 25);
        assertHSBCBug("display2", 29);
    }

    @Test
    void testBadFindHidingSubClassTest4() {
        performAnalysis("findhiddenmethodtest/BadMainMethodCheck.class",
                "findhiddenmethodtest/BadSuperClassWithMain.class");
        assertHSBCBug("main", 18);
    }

    @Test
    void testBadFindHidingSubClassTest5() {
        performAnalysis("findhiddenmethodtest/BadGrandClass.class",
                "findhiddenmethodtest/BadParentClass.class",
                "findhiddenmethodtest/BadMultiLevelInheritance.class");
        assertHSBCBug("display", 20);
    }

    @Test
    void testBadFindHidingSubClassTest6() {
        performAnalysis("findhiddenmethodtest/SuperBadMultipleMethods.class",
                "findhiddenmethodtest/BadMultipleStaticMethods.class");
        assertHSBCBug("display", 19);
        assertHSBCBug("display2", 23);
    }

    @Test
    void testBadFindHidingSubClassTest7() {
        performAnalysis("findhiddenmethodtest/SuperBadNonOrderedMultipleStaticMethods.class",
                "findhiddenmethodtest/BadNonOrderedMultipleStaticMethods.class");
        assertHSBCBug("display", 24);
        assertHSBCBug("display2", 28);
    }

    @Test
    void testBadFindHidingSubClassTest8() {
        performAnalysis("findhiddenmethodtest/BadOuterInnerClass.class",
                "findhiddenmethodtest/BadOuterInnerClass$BadInner.class");
        assertHSBCBug("badMethod", 15);
    }

    @Test
    void testBadFindHidingSubClassTest9() {
        performAnalysis("findhiddenmethodtest/SuperBadProtected.class",
                "findhiddenmethodtest/BadProtected.class");
        assertHSBCBug("display", 15);
    }

    @Test
    void testGoodFindHidingSubClassTest() {
        performAnalysis("findhiddenmethodtest/GrantAccess.class",
                "findhiddenmethodtest/GoodFindHidingSubClassTest.class");
        assertNoHSBCBug();
    }

    @Test
    void testGoodFindHidingSubClassTest2() {
        performAnalysis("findhiddenmethodtest/SuperGoodInEqualMultipleMethod.class",
                "findhiddenmethodtest/GoodInEqualNonStaticMethods.class");
        assertNoHSBCBug();
    }

    @Test
    void testGoodFindHidingSubClassTest3() {
        performAnalysis("findhiddenmethodtest/GoodSuperClassWithMain.class",
                "findhiddenmethodtest/GoodMainMethodCheck.class");
        assertNoHSBCBug();
    }

    @Test
    void testGoodFindHidingSubClassTest4() {
        performAnalysis("findhiddenmethodtest/GoodGrandNonStatic.class",
                "findhiddenmethodtest/GoodParentNonStatic.class",
                "findhiddenmethodtest/GoodMultiLevelInheritanceNonStatic.class");
        assertNoHSBCBug();
    }

    @Test
    void testGoodFindHidingSubClassTest5() {
        performAnalysis("findhiddenmethodtest/GoodGrandClassPrivate.class",
                "findhiddenmethodtest/GoodParentClassPrivate.class",
                "findhiddenmethodtest/GoodMultiLevelInheritancePrivate.class");
        assertNoHSBCBug();
    }

    @Test
    void testGoodFindHidingSubClassTest6() {
        performAnalysis("findhiddenmethodtest/SuperMultipleNonStatic.class",
                "findhiddenmethodtest/GoodMultipleNonStaticMethods.class");
        assertNoHSBCBug();
    }

    @Test
    void testGoodFindHidingSubClassTest7() {
        performAnalysis("findhiddenmethodtest/SuperGoodMultipleStaticMethod.class",
                "findhiddenmethodtest/GoodMultipleStaticMethod.class");
        assertNoHSBCBug();
    }

    @Test
    void testGoodFindHidingSubClassTest8() {
        performAnalysis("findhiddenmethodtest/SuperGoodNonOrderedMultipleStaticMethods.class",
                "findhiddenmethodtest/GoodNonOrderedMultipleNonStaticMethods.class");
        assertNoHSBCBug();
    }

    @Test
    void testGoodFindHidingSubClassTest9() {
        performAnalysis("findhiddenmethodtest/GoodOuterInnerClassNonStatic.class",
                "findhiddenmethodtest/GoodOuterInnerClassNonStatic$GoodInnerNonStaticGood.class");
        assertNoHSBCBug();
    }

    @Test
    void testGoodFindHidingSubClassTest10() {
        performAnalysis("findhiddenmethodtest/GoodOuterInnerClassPrivate.class",
                "findhiddenmethodtest/GoodOuterInnerClassPrivate$GoodInnerClassPrivate.class");
        assertNoHSBCBug();
    }

    @Test
    void testGoodFindHidingSubClassTest11() {
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
