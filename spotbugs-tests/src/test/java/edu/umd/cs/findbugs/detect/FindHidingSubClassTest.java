package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

import org.junit.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class FindHidingSubClassTest extends AbstractIntegrationTest {
    static String bugType = "HSBC_HIDING_SUB_CLASS";

    @Test
    public void testBadFindHidingSubClassTest() {
        performAnalysis("findhidingsubclasstest/GrantAccessStatic.class",
                "findhidingsubclasstest/GrantUserAccessStatic.class");
        assertHSBCBug("displayAccountStatus", 15);
    }

    @Test
    public void testBadFindHidingSubClassTest2() {
        performAnalysis("findhidingsubclasstest/BadSuperClass.class",
                "findhidingsubclasstest/BadSubClass.class");
        assertHSBCBug("methodHiding", 16);
    }


    @Test
    public void testBadFindHidingSubClassTest3() {
        performAnalysis("findhidingsubclasstest/SuperBadInEqualMultipleMethod.class",
                "findhidingsubclasstest/SubBadInEqualMultipleMethod.class");
        assertHSBCBug("display", 25);
        assertHSBCBug("display2", 29);
    }

    @Test
    public void testBadFindHidingSubClassTest4() {
        performAnalysis("findhidingsubclasstest/BadMainMethodCheck.class",
                "findhidingsubclasstest/BadSuperClassWithMain.class");
        assertHSBCBug("main", 17);
    }

    @Test
    public void testBadFindHidingSubClassTest5() {
        performAnalysis("findhidingsubclasstest/BadGrandClass.class",
                "findhidingsubclasstest/BadParentClass.class",
                "findhidingsubclasstest/BadChildClass.class");
        assertHSBCBug("display", 20);
    }

    @Test
    public void testBadFindHidingSubClassTest6() {
        performAnalysis("findhidingsubclasstest/SuperBadMultipleMethods.class",
                "findhidingsubclasstest/SubBadMultipleMethods.class");
        assertHSBCBug("display", 19);
        assertHSBCBug("display2", 23);
    }

    @Test
    public void testBadFindHidingSubClassTest7() {
        performAnalysis("findhidingsubclasstest/SuperBadNonOrderedMultipleStaticMethods.class",
                "findhidingsubclasstest/SubBadNonOrderedMultipleStaticMethods.class");
        assertHSBCBug("display", 24);
        assertHSBCBug("display2", 28);
    }

    @Test
    public void testBadFindHidingSubClassTest8() {
        performAnalysis("findhidingsubclasstest/BadOuterInnerClass.class",
                "findhidingsubclasstest/BadOuterInnerClass$BadInner.class");
        assertHSBCBug("badMethod", 15);
    }

    @Test
    public void testBadFindHidingSubClassTest9() {
        performAnalysis("findhidingsubclasstest/SuperBadProtected.class",
                "findhidingsubclasstest/SubBadProtected.class");
        assertHSBCBug("display", 11);
    }

    @Test
    public void testGoodFindHidingSubClassTest() {
        performAnalysis("findhidingsubclasstest/GrantAccess.class",
                "findhidingsubclasstest/GoodFindHidingSubClassTest.class");
        assertNoHSBCBug();
    }

    @Test
    public void testGoodFindHidingSubClassTest2() {
        performAnalysis("findhidingsubclasstest/SuperGoodInEqualMultipleMethod.class",
                "findhidingsubclasstest/SubGoodInEqualMultipleMethod.class");
        assertNoHSBCBug();
    }

    @Test
    public void testGoodFindHidingSubClassTest3() {
        performAnalysis("findhidingsubclasstest/GoodSuperClassWithMain.class",
                "findhidingsubclasstest/GoodMainMethodCheck.class");
        assertNoHSBCBug();
    }

    @Test
    public void testGoodFindHidingSubClassTest4() {
        performAnalysis("findhidingsubclasstest/GoodGrandNonStatic.class",
                "findhidingsubclasstest/GoodParentNonStatic.class",
                "findhidingsubclasstest/GoodChildNonStatic.class");
        assertNoHSBCBug();
    }

    @Test
    public void testGoodFindHidingSubClassTest5() {
        performAnalysis("findhidingsubclasstest/GoodGrandClassPrivate.class",
                "findhidingsubclasstest/GoodParentClassPrivate.class",
                "findhidingsubclasstest/GoodChildClassPrivate.class");
        assertNoHSBCBug();
    }

    @Test
    public void testGoodFindHidingSubClassTest6() {
        performAnalysis("findhidingsubclasstest/SuperMultipleNonStatic.class",
                "findhidingsubclasstest/SubMultipleNonStatic.class");
        assertNoHSBCBug();
    }

    @Test
    public void testGoodFindHidingSubClassTest7() {
        performAnalysis("findhidingsubclasstest/SuperGoodMultipleStaticMethod.class",
                "findhidingsubclasstest/SubGoodMultipleStaticMethod.class");
        assertNoHSBCBug();
    }

    @Test
    public void testGoodFindHidingSubClassTest8() {
        performAnalysis("findhidingsubclasstest/SuperGoodNonOrderedMultipleStaticMethods.class",
                "findhidingsubclasstest/SubGoodNonOrderedMultipleStaticMethods.class");
        assertNoHSBCBug();
    }

    @Test
    public void testGoodFindHidingSubClassTest9() {
        performAnalysis("findhidingsubclasstest/GoodOuterInnerClassNonStatic.class",
                "findhidingsubclasstest/GoodOuterInnerClassNonStatic$GoodInnerNonStaticGood.class");
        assertNoHSBCBug();
    }

    @Test
    public void testGoodFindHidingSubClassTest10() {
        performAnalysis("findhidingsubclasstest/GoodOuterInnerClassPrivate.class",
                "findhidingsubclasstest/GoodOuterInnerClassPrivate$GoodInnerClassPrivate.class");
        assertNoHSBCBug();
    }

    @Test
    public void testGoodFindHidingSubClassTest11() {
        performAnalysis("findhidingsubclasstest/SuperGoodProtected.class",
                "findhidingsubclasstest/SubGoodProtected.class");
        assertNoHSBCBug();
    }

    private void assertNoHSBCBug() {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }

    private void assertHSBCBug(String methodName, int lineNumber) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inMethod(methodName)
                .atLine(lineNumber)
                .build();
        assertThat(getBugCollection(), hasItem(bugTypeMatcher));
    }

}