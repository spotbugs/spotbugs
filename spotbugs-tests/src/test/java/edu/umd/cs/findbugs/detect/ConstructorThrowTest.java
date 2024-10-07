package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class ConstructorThrowTest extends AbstractIntegrationTest {

    private static final String CT_THROW = "CT_CONSTRUCTOR_THROW";

    @Test
    void testConstructorThrowCheck1() {
        performAnalysis("constructorthrow/ConstructorThrowTest1.class");
        assertBugTypeCount(CT_THROW, 1);
        assertBugAtLine(CT_THROW, 9);
    }

    @Test
    void testConstructorThrowCheck2() {
        performAnalysis("constructorthrow/ConstructorThrowTest2.class");
        assertBugTypeCount(CT_THROW, 1);
        assertBugAtLine(CT_THROW, 11);
    }

    @Test
    void testConstructorThrowCheck3() {
        performAnalysis("constructorthrow/ConstructorThrowTest3.class");
        assertBugTypeCount(CT_THROW, 1);
        assertBugAtLine(CT_THROW, 9);
    }

    @Test
    void testConstructorThrowCheck4() {
        performAnalysis("constructorthrow/ConstructorThrowTest4.class");
        assertBugTypeCount(CT_THROW, 1);
        assertBugAtLine(CT_THROW, 20);
    }

    @Test
    void testConstructorThrowCheck5() {
        performAnalysis("constructorthrow/ConstructorThrowTest5.class");
        assertBugTypeCount(CT_THROW, 2);
        assertBugAtLine(CT_THROW, 11);
        assertBugAtLine(CT_THROW, 16);
    }

    @Test
    void testConstructorThrowCheck6() {
        performAnalysis("constructorthrow/ConstructorThrowTest6.class");
        assertBugTypeCount(CT_THROW, 1);
        assertBugAtLine(CT_THROW, 11);
    }

    @Test
    void testConstructorThrowCheck7() {
        performAnalysis("constructorthrow/ConstructorThrowTest7.class");
        assertNoBugType(CT_THROW); // It doesn't work for lambdas
    }

    @Test
    void testConstructorThrowCheck8() {
        performAnalysis("constructorthrow/ConstructorThrowTest8.class");
        assertNoBugType(CT_THROW); // It doesn't work for lambdas
    }

    @Test
    void testConstructorThrowCheck9() {
        performAnalysis("constructorthrow/ConstructorThrowTest9.class");
        assertNoBugType(CT_THROW); // It doesn't work for lambdas
    }

    @Test
    void testConstructorThrowCheck10() {
        performAnalysis("constructorthrow/ConstructorThrowTest10.class");
        assertBugTypeCount(CT_THROW, 1);
        assertBugAtLine(CT_THROW, 9);
    }

    @Test
    void testConstructorThrowCheck11() {
        performAnalysis("constructorthrow/ConstructorThrowTest11.class");
        assertBugTypeCount(CT_THROW, 1);
        assertBugAtLine(CT_THROW, 15);
    }

    @Test
    void testConstructorThrowCheck12() {
        performAnalysis("constructorthrow/ConstructorThrowTest12.class");
        assertBugTypeCount(CT_THROW, 1);
        assertBugAtLine(CT_THROW, 15);
    }

    @Test
    void testConstructorThrowCheck13() {
        performAnalysis("constructorthrow/ConstructorThrowTest13.class");
        assertBugTypeCount(CT_THROW, 1);
        assertBugAtLine(CT_THROW, 19);
    }

    @Test
    void testConstructorThrowCheck14() {
        performAnalysis("constructorthrow/ConstructorThrowTest14.class");
        assertBugTypeCount(CT_THROW, 1);
        assertBugAtLine(CT_THROW, 24);
    }

    @Test
    void testConstructorThrowCheck15() {
        performAnalysis("constructorthrow/ConstructorThrowTest15.class");
        assertBugTypeCount(CT_THROW, 1);
        assertBugAtLine(CT_THROW, 15);
    }

    @Test
    void testConstructorThrowCheck16() {
        performAnalysis("constructorthrow/ConstructorThrowTest16.class");
        assertBugTypeCount(CT_THROW, 1);
        assertBugAtLine(CT_THROW, 10);
    }

    @Test
    void testConstructorThrowCheck17() {
        performAnalysis("constructorthrow/ConstructorThrowTest17.class");
        assertBugTypeCount(CT_THROW, 1);
        assertBugAtLine(CT_THROW, 9);
    }

    @Test
    void testConstructorThrowCheck18() {
        performAnalysis("constructorthrow/ConstructorThrowTest18.class");
        assertBugTypeCount(CT_THROW, 1);
        assertBugAtLine(CT_THROW, 11);
    }

    @Test
    void testConstructorThrowCheck19() {
        performAnalysis("constructorthrow/ConstructorThrowTest19.class");
        assertBugTypeCount(CT_THROW, 1);
        assertBugAtLine(CT_THROW, 11);
    }

    @Test
    void testConstructorThrowCheck20() {
        performAnalysis("constructorthrow/ConstructorThrowTest20.class");
        assertBugTypeCount(CT_THROW, 1);
        assertBugAtLine(CT_THROW, 13);
    }

    @Test
    void testConstructorThrowCheck21() {
        performAnalysis("constructorthrow/ConstructorThrowTest21.class");
        assertBugTypeCount(CT_THROW, 1);
        assertBugAtLine(CT_THROW, 9);
    }

    @Test
    void testConstructorThrowCheck22() {
        performAnalysis("constructorthrow/ConstructorThrowTest22.class");
        assertBugTypeCount(CT_THROW, 1);
        assertBugAtLine(CT_THROW, 11);
    }

    @Test
    void testConstructorThrowCheck23() {
        performAnalysis("constructorthrow/ConstructorThrowTest23.class");
        assertNoBugType(CT_THROW); // It doesn't work for lambdas
    }

    @Test
    void testConstructorThrowCheck24() {
        performAnalysis("constructorthrow/ConstructorThrowTest24.class");
        assertBugTypeCount(CT_THROW, 1);
        assertBugAtLine(CT_THROW, 11);
    }

    @Test
    void testGoodConstructorThrowCheck1() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest1.class");
        assertNoBugType(CT_THROW);
    }

    @Test
    void testGoodConstructorThrowCheck2() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest2.class");
        assertNoBugType(CT_THROW);
    }

    @Test
    void testGoodConstructorThrowCheck3() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest3.class");
        assertNoBugType(CT_THROW);
    }

    @Test
    void testGoodConstructorThrowCheck4() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest4.class");
        assertNoBugType(CT_THROW);
    }

    @Test
    void testGoodConstructorThrowCheck5() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest5.class");
        assertNoBugType(CT_THROW);
    }

    @Test
    void testGoodConstructorThrowCheck6() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest6.class");
        assertNoBugType(CT_THROW);
    }

    @Test
    void testGoodConstructorThrowCheck7() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest7.class");
        assertNoBugType(CT_THROW);
    }

    @Test
    void testGoodConstructorThrowCheck8() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest8.class");
        assertNoBugType(CT_THROW);
    }

    @Test
    void testGoodConstructorThrowCheck9() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest9.class");
        assertNoBugType(CT_THROW);
    }

    @Test
    void testGoodConstructorThrowCheck10() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest10.class");
        assertNoBugType(CT_THROW);
    }

    @Test
    void testGoodConstructorThrowCheck11() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest11.class");
        assertNoBugType(CT_THROW);
    }

    @Test
    void testGoodConstructorThrowCheck12() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest12.class");
        assertNoBugType(CT_THROW);
    }

    @Test
    void testGoodConstructorThrowCheck13() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest12.class",
                "constructorthrow/ConstructorThrowNegativeTest13.class");
        assertNoBugType(CT_THROW);
    }

    @Test
    void testGoodConstructorThrowCheck14() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest14.class");
        assertNoBugType(CT_THROW);
    }

    @Test
    void testGoodConstructorThrowCheck15() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest15.class");
        assertNoBugType(CT_THROW);
    }

    @Test
    void testGoodConstructorThrowCheck16() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest16.class");
        assertNoBugType(CT_THROW);
    }

    @Test
    void testGoodConstructorThrowCheck17() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest17.class");
        assertNoBugType(CT_THROW);
    }

    @Test
    void testGoodConstructorThrowCheck18() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest18.class");
        assertNoBugType(CT_THROW);
    }

    @Test
    void testGoodConstructorThrowCheck19() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest19.class");
        assertNoBugType(CT_THROW);
    }
}
