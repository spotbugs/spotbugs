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
        assertCTBugInLine(13);
    }

    @Test
    public void testConstructorThrowCheck4() {
        performAnalysis("constructorthrow/ConstructorThrowTest4.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(20); // fails, 16 instead of 20
    }

    @Test
    public void testConstructorThrowCheck5() {
        performAnalysis("constructorthrow/ConstructorThrowTest5.class");
        assertNumOfCTBugs(1);
        assertCTBugInLine(7); // fails, 21 instead of 7
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
        assertNumOfCTBugs(1); // fails
        assertCTBugInLine(11); // Preferable 12, but 11 is ok.
    }

    @Test
    public void testConstructorThrowCheck8() {
        performAnalysis("constructorthrow/ConstructorThrowTest8.class");
        assertNumOfCTBugs(1); // fails
        assertCTBugInLine(11); // Preferable 12, but 11 is ok.
    }

    @Test
    public void testConstructorThrowCheck9() {
        performAnalysis("constructorthrow/ConstructorThrowTest9.class");
        assertNumOfCTBugs(1); // fails
        assertCTBugInLine(11); // Preferable 12, but 11 is ok.
    }

    @Test
    public void testConstructorThrowCheck10() {
        performAnalysis("constructorthrow/ConstructorThrowTest10.class");
        assertNumOfCTBugs(1); // fails
        assertCTBugInLine(9);
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
        assertNumOfCTBugs(0); // fails
    }

    @Test
    public void testGoodConstructorThrowCheck4() {
        performAnalysis("constructorthrow/ConstructorThrowNegativeTest4.class");
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
