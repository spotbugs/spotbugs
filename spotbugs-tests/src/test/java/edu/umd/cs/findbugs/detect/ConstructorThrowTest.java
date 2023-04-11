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
    }

    @Test
    public void testConstructorThrowCheck2() {
        performAnalysis("ConstructorThrowTest2.class");

        assertNumOfCTBugs(1);
    }

    @Test
    public void testConstructorThrowCheck3() {
        performAnalysis("ConstructorThrowTest3.class");

        assertNumOfCTBugs(1);
    }

    @Test
    public void testConstructorThrowCheck4() {
        performAnalysis("ConstructorThrowTest4.class");

        assertNumOfCTBugs(1);
    }

    @Test
    public void testConstructorThrowCheck5() {
        performAnalysis("ConstructorThrowTest5.class");

        assertNumOfCTBugs(1);
    }

    @Test
    public void testConstructorThrowCheck6() {
        performAnalysis("ConstructorThrowTest6.class");

        assertNumOfCTBugs(1);
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
        // This is a false positive - this test should fail, there should be no CT bugs
        assertNumOfCTBugs(1);
    }

    private void assertNumOfCTBugs(int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("CT_CONSTRUCTOR_THROW").build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertCTBug(String className, String method, int line) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("CT_CONSTRUCTOR_THROW")
                .inClass(className)
                .inMethod(method)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }
}
