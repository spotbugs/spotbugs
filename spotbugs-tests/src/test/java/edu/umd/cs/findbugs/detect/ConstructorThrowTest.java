package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

public class ConstructorThrowTest extends AbstractIntegrationTest {
    @Test
    public void testConstructorThrowCheck1() {
        performAnalysis("constructorthrow/ConstructorThrowTest1.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(9);
    }

    @Test
    public void testConstructorThrowCheck2() {
        performAnalysis("constructorthrow/ConstructorThrowTest2.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(10);
    }

    @Test
    public void testConstructorThrowCheck3() {
        performAnalysis("constructorthrow/ConstructorThrowTest3.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(9);
    }

    @Test
    public void testConstructorThrowCheck4() {
        performAnalysis("constructorthrow/ConstructorThrowTest4.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(20);
    }

    @Test
    public void testConstructorThrowCheck5() {
        performAnalysis("constructorthrow/ConstructorThrowTest5.class");
        assertNumOfCTBugs(2);
        assertCTBugInLine(11);
        assertCTBugInLine(16);
    }

    @Test
    public void testConstructorThrowCheck6() {
        performAnalysis("constructorthrow/ConstructorThrowTest6.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(11);
    }

    @Test
    public void testConstructorThrowCheck7() {
        performAnalysis("constructorthrow/ConstructorThrowTest7.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(11); // Preferable 12, but 11 is ok.
    }

    @Test
    public void testConstructorThrowCheck8() {
        performAnalysis("constructorthrow/ConstructorThrowTest8.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(11); // Preferable 12, but 11 is ok.
    }

    @Test
    public void testConstructorThrowCheck9() {
        performAnalysis("constructorthrow/ConstructorThrowTest9.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(11); // Preferable 12, but 11 is ok.
    }

    @Test
    public void testConstructorThrowCheck10() {
        performAnalysis("constructorthrow/ConstructorThrowTest10.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(9);
    }

    @Test
    public void testConstructorThrowCheck11() {
        performAnalysis("constructorthrow/ConstructorThrowTest11.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(15);
    }

    @Test
    public void testConstructorThrowCheck12() {
        performAnalysis("constructorthrow/ConstructorThrowTest12.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(15);
    }

    @Test
    public void testConstructorThrowCheck13() {
        performAnalysis("constructorthrow/ConstructorThrowTest13.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(19);
    }

    @Test
    public void testConstructorThrowCheck14() {
        performAnalysis("constructorthrow/ConstructorThrowTest14.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(24);
    }

    @Test
    public void testConstructorThrowCheck15() {
        performAnalysis("constructorthrow/ConstructorThrowTest15.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(15);
    }

    @Test
    public void testConstructorThrowCheck16() {
        performAnalysis("constructorthrow/ConstructorThrowTest16.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(10);
    }

    @Test
    public void testGoodConstructorThrowCheck1() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest1.class");
        assertNumOfCTBugs(0);
    }

    @Test
    public void testGoodConstructorThrowCheck2() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest2.class");
        assertNumOfCTBugs(0);
    }

    @Test
    public void testGoodConstructorThrowCheck3() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest3.class");
        assertNumOfCTBugs(0);
    }

    @Test
    public void testGoodConstructorThrowCheck4() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest4.class");
        assertNumOfCTBugs(0);
    }

    @Test
    public void testGoodConstructorThrowCheck5() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest5.class");
        assertNumOfCTBugs(0);
    }

    @Test
    public void testGoodConstructorThrowCheck6() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest6.class");
        assertNumOfCTBugs(0);
    }

    @Test
    public void testGoodConstructorThrowCheck7() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest7.class");
        assertNumOfCTBugs(0);
    }

    @Test
    public void testGoodConstructorThrowCheck8() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest8.class");
        assertNumOfCTBugs(0);
    }

    @Test
    public void testGoodConstructorThrowCheck9() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest9.class");
        assertNumOfCTBugs(0);
    }

    @Test
    public void testGoodConstructorThrowCheck10() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest10.class");
        assertNumOfCTBugs(0);
    }

    @Test
    public void testGoodConstructorThrowCheck11() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest11.class");
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
