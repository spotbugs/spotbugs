package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

class FindHidingSubClassTest extends AbstractIntegrationTest {
    private final String BUG_TYPE = "HSBC_HIDING_SUB_CLASS";

    @Test
    void testBadFindHidingSubClassTest() {
        performAnalysis("findhidingsubclasstest/GrantAccessStatic.class",
                "findhidingsubclasstest/BadFindHidingSubClassTest.class");
        assertHSBCBug("displayAccountStatus", 15);
    }

    @Test
    void testBadFindHidingSubClassTest2() {
        performAnalysis("findhidingsubclasstest/BadSuperClass.class",
                "findhidingsubclasstest/BadHidingVsOverriding.class");
        assertHSBCBug("methodHiding", 19);
    }


    @Test
    void testBadFindHidingSubClassTest3() {
        performAnalysis("findhidingsubclasstest/SuperBadInEqualMultipleMethod.class",
                "findhidingsubclasstest/BadInEqualMultipleStaticMethod.class");
        assertHSBCBug("display", 25);
        assertHSBCBug("display2", 29);
    }

    @Test
    void testBadFindHidingSubClassTest4() {
        performAnalysis("findhidingsubclasstest/BadMainMethodCheck.class",
                "findhidingsubclasstest/BadSuperClassWithMain.class");
        assertHSBCBug("main", 18);
    }

    @Test
    void testBadFindHidingSubClassTest5() {
        performAnalysis("findhidingsubclasstest/BadGrandClass.class",
                "findhidingsubclasstest/BadParentClass.class",
                "findhidingsubclasstest/BadMultiLevelInheritance.class");
        assertHSBCBug("display", 20);
    }

    @Test
    void testBadFindHidingSubClassTest6() {
        performAnalysis("findhidingsubclasstest/SuperBadMultipleMethods.class",
                "findhidingsubclasstest/BadMultipleStaticMethods.class");
        assertHSBCBug("display", 19);
        assertHSBCBug("display2", 23);
    }

    @Test
    void testBadFindHidingSubClassTest7() {
        performAnalysis("findhidingsubclasstest/SuperBadNonOrderedMultipleStaticMethods.class",
                "findhidingsubclasstest/BadNonOrderedMultipleStaticMethods.class");
        assertHSBCBug("display", 24);
        assertHSBCBug("display2", 28);
    }

    @Test
    void testBadFindHidingSubClassTest8() {
        performAnalysis("findhidingsubclasstest/BadOuterInnerClass.class",
                "findhidingsubclasstest/BadOuterInnerClass$BadInner.class");
        assertHSBCBug("badMethod", 15);
    }

    @Test
    void testBadFindHidingSubClassTest9() {
        performAnalysis("findhidingsubclasstest/SuperBadProtected.class",
                "findhidingsubclasstest/BadProtected.class");
        assertHSBCBug("display", 15);
    }

    @Test
    void testGoodFindHidingSubClassTest() {
        performAnalysis("findhidingsubclasstest/GrantAccess.class",
                "findhidingsubclasstest/GoodFindHidingSubClassTest.class");
        assertNoHSBCBug();
    }

    @Test
    void testGoodFindHidingSubClassTest2() {
        performAnalysis("findhidingsubclasstest/SuperGoodInEqualMultipleMethod.class",
                "findhidingsubclasstest/GoodInEqualNonStaticMethods.class");
        assertNoHSBCBug();
    }

    @Test
    void testGoodFindHidingSubClassTest3() {
        performAnalysis("findhidingsubclasstest/GoodSuperClassWithMain.class",
                "findhidingsubclasstest/GoodMainMethodCheck.class");
        assertNoHSBCBug();
    }

    @Test
    void testGoodFindHidingSubClassTest4() {
        performAnalysis("findhidingsubclasstest/GoodGrandNonStatic.class",
                "findhidingsubclasstest/GoodParentNonStatic.class",
                "findhidingsubclasstest/GoodMultiLevelInheritanceNonStatic.class");
        assertNoHSBCBug();
    }

    @Test
    void testGoodFindHidingSubClassTest5() {
        performAnalysis("findhidingsubclasstest/GoodGrandClassPrivate.class",
                "findhidingsubclasstest/GoodParentClassPrivate.class",
                "findhidingsubclasstest/GoodMultiLevelInheritancePrivate.class");
        assertNoHSBCBug();
    }

    @Test
    void testGoodFindHidingSubClassTest6() {
        performAnalysis("findhidingsubclasstest/SuperMultipleNonStatic.class",
                "findhidingsubclasstest/GoodMultipleNonStaticMethods.class");
        assertNoHSBCBug();
    }

    @Test
    void testGoodFindHidingSubClassTest7() {
        performAnalysis("findhidingsubclasstest/SuperGoodMultipleStaticMethod.class",
                "findhidingsubclasstest/GoodMultipleStaticMethod.class");
        assertNoHSBCBug();
    }

    @Test
    void testGoodFindHidingSubClassTest8() {
        performAnalysis("findhidingsubclasstest/SuperGoodNonOrderedMultipleStaticMethods.class",
                "findhidingsubclasstest/GoodNonOrderedMultipleNonStaticMethods.class");
        assertNoHSBCBug();
    }

    @Test
    void testGoodFindHidingSubClassTest9() {
        performAnalysis("findhidingsubclasstest/GoodOuterInnerClassNonStatic.class",
                "findhidingsubclasstest/GoodOuterInnerClassNonStatic$GoodInnerNonStaticGood.class");
        assertNoHSBCBug();
    }

    @Test
    void testGoodFindHidingSubClassTest10() {
        performAnalysis("findhidingsubclasstest/GoodOuterInnerClassPrivate.class",
                "findhidingsubclasstest/GoodOuterInnerClassPrivate$GoodInnerClassPrivate.class");
        assertNoHSBCBug();
    }

    @Test
    void testGoodFindHidingSubClassTest11() {
        performAnalysis("findhidingsubclasstest/SuperGoodProtected.class",
                "findhidingsubclasstest/GoodProtected.class");
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
