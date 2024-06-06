package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.SystemProperties;
import edu.umd.cs.findbugs.annotations.Confidence;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class FindRefComparisonTest extends AbstractIntegrationTest {

    @AfterAll
    static void tearDown() {
        SystemProperties.removeProperty("findbugs.refcomp.reportAll");
    }

    @Test
    void testComparisonsWhenPropertyUnset() {
        SystemProperties.removeProperty("findbugs.refcomp.reportAll");
        performAnalysis("RefComparisons.class");
        assertNumOfRefCompBugs(3);
        assertRefCompBug("integerBadComparison", 4);
        assertRefCompBug("myClass1BadComparison", 11);
        assertRefCompBug("myClass2BadComparison", 18);
    }

    @Test
    void testComparisonsWhenReportAllIsNotEnabled() {
        SystemProperties.setProperty("findbugs.refcomp.reportAll", "false");
        performAnalysis("RefComparisons.class");
        assertNumOfRefCompBugs(1);
        assertRefCompBug("integerBadComparison", 4);
    }

    @Test
    void testComparisonsWhenReportAllIsEnabled() {
        SystemProperties.setProperty("findbugs.refcomp.reportAll", "true");
        performAnalysis("RefComparisons.class");
        assertNumOfRefCompBugs(3);
        assertRefCompBug("integerBadComparison", 4);
        assertRefCompBug("myClass1BadComparison", 11);
        assertRefCompBug("myClass2BadComparison", 18);
    }

    private void assertNumOfRefCompBugs(int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("RC_REF_COMPARISON").build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertRefCompBug(String method, int line) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("RC_REF_COMPARISON")
                .inClass("RefComparisons")
                .inMethod(method)
                .atLine(line)
                .withConfidence(Confidence.HIGH)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

}
