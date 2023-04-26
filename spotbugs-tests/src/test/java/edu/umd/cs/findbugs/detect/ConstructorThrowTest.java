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
        performAnalysis("ConstructorThrowTest1.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(7);
    }

    @Test
    public void testConstructorThrowCheck2() {
        performAnalysis("ConstructorThrowTest2.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(9);   // 8
    }

    @Test
    public void testConstructorThrowCheck3() {
        performAnalysis("ConstructorThrowTest3.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(11);
    }

    @Test
    public void testConstructorThrowCheck4() {
        performAnalysis("ConstructorThrowTest4.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(18); // fails, 14 instead of 18
    }

    @Test
    public void testConstructorThrowCheck5() {
        performAnalysis("ConstructorThrowTest5.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(5); // fails, 19 instead of 5
        assertCTBugInLine(14);
    }

    @Test
    public void testConstructorThrowCheck6() {
        performAnalysis("ConstructorThrowTest6.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(9);
    }

    @Test
    public void testConstructorThrowCheck7() {
        performAnalysis("ConstructorThrowTest7.class");
        assertNumOfCTBugs(1);   // fails
        assertCTBugInLine(9); // Preferable 10, but 9 is ok.
    }

    @Test
    public void testConstructorThrowCheck8() {
        performAnalysis("ConstructorThrowTest8.class");
        assertNumOfCTBugs(1); // fails
        assertCTBugInLine(9); // Preferable 10, but 9 is ok.
    }

    @Test
    public void testConstructorThrowCheck9() {
        performAnalysis("ConstructorThrowTest9.class");
        assertNumOfCTBugs(1); // fails
        assertCTBugInLine(9); // Preferable 10, but 9 is ok.
    }

    @Test
    public void testConstructorThrowCheck10() {
        performAnalysis("ConstructorThrowTest10.class");
        assertNumOfCTBugs(1); // fails
        assertCTBugInLine(7);
    }

    @Test
    public void testGoodConstructorThrowCheck1() {
        performAnalysis("ConstructorThrowNegativeTest1.class");
        assertNumOfCTBugs(0);
    }

    @Test
    public void testGoodConstructorThrowCheck2() {
        performAnalysis("ConstructorThrowNegativeTest2.class");
        assertNumOfCTBugs(0);
    }

    @Test
    public void testGoodConstructorThrowCheck3() {
        performAnalysis("ConstructorThrowNegativeTest3.class");
        assertNumOfCTBugs(0); // fails
    }
    @Test
    public void testGoodConstructorThrowCheck4() {
        performAnalysis("ConstructorThrowNegativeTest4.class");
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
