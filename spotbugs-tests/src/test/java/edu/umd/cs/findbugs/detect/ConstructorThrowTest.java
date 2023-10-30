package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

class ConstructorThrowTest extends AbstractIntegrationTest {

    @Test
    void testConstructorThrowCheck1() {
        performAnalysis("constructorthrow/ConstructorThrowTest1.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(9);
    }

    @Test
    void testConstructorThrowCheck2() {
        performAnalysis("constructorthrow/ConstructorThrowTest2.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(10);
    }

    @Test
    void testConstructorThrowCheck3() {
        performAnalysis("constructorthrow/ConstructorThrowTest3.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(9);
    }

    @Test
    void testConstructorThrowCheck4() {
        performAnalysis("constructorthrow/ConstructorThrowTest4.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(20);
    }

    @Test
    void testConstructorThrowCheck5() {
        performAnalysis("constructorthrow/ConstructorThrowTest5.class");
        assertNumOfCTBugs(2);
        assertCTBugInLine(11);
        assertCTBugInLine(16);
    }

    @Test
    void testConstructorThrowCheck6() {
        performAnalysis("constructorthrow/ConstructorThrowTest6.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(11);
    }

    @Test
    void testConstructorThrowCheck7() {
        performAnalysis("constructorthrow/ConstructorThrowTest7.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(11); // Preferable 12, but 11 is ok.
    }

    @Test
    void testConstructorThrowCheck8() {
        performAnalysis("constructorthrow/ConstructorThrowTest8.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(11); // Preferable 12, but 11 is ok.
    }

    @Test
    void testConstructorThrowCheck9() {
        performAnalysis("constructorthrow/ConstructorThrowTest9.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(11); // Preferable 12, but 11 is ok.
    }

    @Test
    void testConstructorThrowCheck10() {
        performAnalysis("constructorthrow/ConstructorThrowTest10.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(9);
    }

    @Test
    void testConstructorThrowCheck11() {
        performAnalysis("constructorthrow/ConstructorThrowTest11.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(15);
    }

    @Test
    void testConstructorThrowCheck12() {
        performAnalysis("constructorthrow/ConstructorThrowTest12.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(15);
    }

    @Test
    void testConstructorThrowCheck13() {
        performAnalysis("constructorthrow/ConstructorThrowTest13.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(19);
    }

    @Test
    void testConstructorThrowCheck14() {
        performAnalysis("constructorthrow/ConstructorThrowTest14.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(24);
    }

    @Test
    void testConstructorThrowCheck15() {
        performAnalysis("constructorthrow/ConstructorThrowTest15.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(15);
    }

    @Test
    void testConstructorThrowCheck16() {
        performAnalysis("constructorthrow/ConstructorThrowTest16.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(10);
    }

    @Test
    void testGoodConstructorThrowCheck1() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest1.class");
        assertNumOfCTBugs(0);
    }

    @Test
    void testGoodConstructorThrowCheck2() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest2.class");
        assertNumOfCTBugs(0);
    }

    @Test
    void testGoodConstructorThrowCheck3() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest3.class");
        assertNumOfCTBugs(0);
    }

    @Test
    void testGoodConstructorThrowCheck4() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest4.class");
        assertNumOfCTBugs(0);
    }

    @Test
    void testGoodConstructorThrowCheck5() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest5.class");
        assertNumOfCTBugs(0);
    }

    @Test
    void testGoodConstructorThrowCheck6() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest6.class");
        assertNumOfCTBugs(0);
    }

    @Test
    void testGoodConstructorThrowCheck7() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest7.class");
        assertNumOfCTBugs(0);
    }

    @Test
    void testGoodConstructorThrowCheck8() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest8.class");
        assertNumOfCTBugs(0);
    }

    @Test
    void testGoodConstructorThrowCheck9() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest9.class");
        assertNumOfCTBugs(0);
    }

    @Test
    void testGoodConstructorThrowCheck10() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest10.class");
        assertNumOfCTBugs(0);
    }

    @Test
    void testGoodConstructorThrowCheck11() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest11.class");
        assertNumOfCTBugs(0);
    }

    @Test
    void testGoodConstructorThrowCheck12() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest12.class");
        assertNumOfCTBugs(0);
    }

    @Test
    void testGoodConstructorThrowCheck13() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest12.class",
                "constructorthrow/ConstructorThrowNegativeTest13.class");
        assertNumOfCTBugs(0);
    }

    private void assertNumOfCTBugs(int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("CT_CONSTRUCTOR_THROW").build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertCTBugInLine(int line) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("CT_CONSTRUCTOR_THROW")
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }
}
