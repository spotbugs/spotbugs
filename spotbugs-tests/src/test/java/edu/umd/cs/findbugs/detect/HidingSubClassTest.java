package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

import org.junit.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class HidingSubClassTest extends AbstractIntegrationTest {
    static String bugType = "HSBC_FIND_HIDING_SUB_CLASS";

    @Test
    public void testBadFindHidingSubClassTest() {
        performAnalysis("findhidingsubclasstest/BadFindHidingSubClassTest.class",
                "findhidingsubclasstest/GrantAccessStatic.class",
                "findhidingsubclasstest/GrantUserAccessStatic.class");
        assertHSBCBug();
    }

    @Test
    public void testBadFindHidingSubClassTest2() {
        performAnalysis("findhidingsubclasstest/BadFindHidingSubClassTest2.class",
                "findhidingsubclasstest/Demo.class",
                "findhidingsubclasstest/Sample.class");
        assertHSBCBug();
    }

    @Test
    public void testBadFindHidingSubClassTest3() {
        performAnalysis("findhidingsubclasstest/OuterInnerClass.class",
                "findhidingsubclasstest/OuterInnerClass$Inner.class");
        assertHSBCBug();
    }

    @Test
    public void testBadFindHidingSubClassTest4() {
        performAnalysis("findhidingsubclasstest/BadProtected.class",
                "findhidingsubclasstest/SuperProtected.class",
                "findhidingsubclasstest/SubProtected.class");
        assertHSBCBug();
    }

    /*
    @Test
    public void testBadFindHidingSubClassTest5() {
        performAnalysis("findhidingsubclasstest/BadMultiLevelInheritance.class",
                "findhidingsubclasstest/BadGrandClass.class",
                "findhidingsubclasstest/BadParentClass.class",
                "findhidingsubclasstest/BadChildClass.class");
        assertHSBCBug();
    }
    */

    @Test
    public void testBadFindHidingSubClassTest6() {
        performAnalysis("findhidingsubclasstest/BadMultipleStaticMethods.class",
                "findhidingsubclasstest/SuperBadMultipleMethod.class",
                "findhidingsubclasstest/SubBadMultipleMethod.class");
        assertHSBCBug();
    }

    @Test
    public void testGoodFindHidingSubClassTest() {
        performAnalysis("findhidingsubclasstest/GoodFindHidingSubClassTest.class",
                "findhidingsubclasstest/GrantAccess.class",
                "findhidingsubclasstest/GoodFindHidingSubClassTest.class");
        assertNoHSBCBug("main");
    }

    @Test
    public void testGoodFindHidingSubClassTest2() {
        performAnalysis("findhidingsubclasstest/GoodFindHidingSubClassTest2.class",
                "findhidingsubclasstest/GrantUserAccessStatic2.class",
                "findhidingsubclasstest/GrantAccessStatic2.class");
        assertNoHSBCBug("main");
    }

    @Test
    public void testGoodFindHidingSubClassTest3() {
        performAnalysis("findhidingsubclasstest/CheckMainMethod.class",
                "findhidingsubclasstest/DumbPublicClass.class");
        assertNoHSBCBug("main");
    }

    @Test
    public void testGoodFindHidingSubClassTest4() {
        performAnalysis("findhidingsubclasstest/OuterInnerClassNonStatic.class");
        assertNoHSBCBug("main");
    }

    @Test
    public void testGoodFindHidingSubClassTest5() {
        performAnalysis("findhidingsubclasstest/GoodMultipleStaticMethod.class",
                "findhidingsubclasstest/SuperMulti.class",
                "findhidingsubclasstest/SubMulti.class");
        assertNoHSBCBug("main");
    }

    @Test
    public void testGoodFindHidingSubClassTest6() {
        performAnalysis("findhidingsubclasstest/GoodMultipleNonStaticMethods.class",
                "findhidingsubclasstest/SuperMultipleNonStatic.class",
                "findhidingsubclasstest/SubMultipleNonStatic.class");
        assertNoHSBCBug("main");
    }

    @Test
    public void testGoodFindHidingSubClassTest7() {
        performAnalysis("findhidingsubclasstest/GoodMultiLevelInheritance.class",
                "findhidingsubclasstest/GoodGrandClass.class",
                "findhidingsubclasstest/GoodParentClass.class",
                "findhidingsubclasstest/GoodChildClass.class");
        assertNoHSBCBug("main");
    }

    @Test
    public void testGoodFindHidingSubClassTest8() {
        performAnalysis("findhidingsubclasstest/GoodMultiLevelInheritanceNonStatic.class",
                "findhidingsubclasstest/GoodGrandNonStatic.class",
                "findhidingsubclasstest/GoodParentNonStatic.class",
                "findhidingsubclasstest/GoodChildNonStatic.class");
        assertNoHSBCBug("main");
    }


    private BugInstanceMatcher createBugInstanceMatcher() {
        return new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .build();
    }

    private BugInstanceMatcher createBugInstanceMatcher(String methodName) {
        return new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inMethod(methodName)
                .build();
    }

    private BugInstanceMatcher createBugInstanceMatcher(String methodName, int lineNUmber) {
        return new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inMethod(methodName)
                .atLine(lineNUmber)
                .build();
    }

    private void assertNoHSBCBug(String methodName) {
        final BugInstanceMatcher bugTypeMatcher = createBugInstanceMatcher(methodName);
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }

    private void assertHSBCBug() {
        final BugInstanceMatcher bugTypeMatcher = createBugInstanceMatcher();
        assertThat(getBugCollection(), hasItem(bugTypeMatcher));
    }

}
