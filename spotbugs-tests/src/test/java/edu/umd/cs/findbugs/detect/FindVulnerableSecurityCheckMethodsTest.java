package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

class FindVulnerableSecurityCheckMethodsTest extends AbstractIntegrationTest {

    static String bugType = "VSC_VULNERABLE_SECURITY_CHECK_METHODS";

    @Test
    void testingBadCases() {
        performAnalysis("vulnerablesecuritycheckmethodstest/FindVulnerableSecurityCheckMethodsTest.class");

        BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .build();
        assertThat(getBugCollection(), containsExactly(7, bugInstanceMatcher));
    }

    @Test
    void testingBadCase1() {
        performAnalysis("vulnerablesecuritycheckmethodstest/FindVulnerableSecurityCheckMethodsTest.class");
        assertVSCBug("badFindVulnerableSecurityCheckMethodsCheck", 23);
    }

    @Test
    void testingBadCase2() {
        performAnalysis("vulnerablesecuritycheckmethodstest/FindVulnerableSecurityCheckMethodsTest.class");
        assertVSCBug("badFindVulnerableSecurityCheckMethodsCheck2", 37);
    }

    @Test
    void testingBadCase3() {
        performAnalysis("vulnerablesecuritycheckmethodstest/FindVulnerableSecurityCheckMethodsTest.class");
        assertVSCBug("badFindVulnerableSecurityCheckMethodsCheck3", 53);
    }

    @Test
    void testingBadCase4() {
        performAnalysis("vulnerablesecuritycheckmethodstest/FindVulnerableSecurityCheckMethodsTest.class");
        assertVSCBug("badFindVulnerableSecurityCheckMethodsCheck4", 94);
    }

    @Test
    void testingBadCase5() {
        performAnalysis("vulnerablesecuritycheckmethodstest/FindVulnerableSecurityCheckMethodsTest.class");
        assertVSCBug("badFindVulnerableSecurityCheckMethodsCheck6", 128);
    }


    @Test
    void testingGoodCase1() {
        performAnalysis("vulnerablesecuritycheckmethodstest/FindVulnerableSecurityCheckMethodsTest.class");
        assertNoVSCBug("goodvulnerablesecuritycheckmethodstestCheck");
    }

    @Test
    void testingGoodCase2() {
        performAnalysis("vulnerablesecuritycheckmethodstest/FindVulnerableSecurityCheckMethodsTest.class");
        assertNoVSCBug("goodVulnerableSecurityCheckMethodsTestCheck2");
    }

    @Test
    void testingGoodCase3() {
        performAnalysis("vulnerablesecuritycheckmethodstest/FindVulnerableSecurityCheckMethodsTest.class");
        assertNoVSCBug("goodVulnerableSecurityCheckMethodsTestCheck4");
    }

    @Test
    void testingGoodCase4() {
        performAnalysis("vulnerablesecuritycheckmethodstest/FindVulnerableSecurityCheckMethodsTest.class");
        assertNoVSCBug("goodVulnerableSecurityCheckMethodsTestCheck5");
    }

    @Test
    void testingGoodCase5() {
        performAnalysis("vulnerablesecuritycheckmethodstest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertNoVSCBug("goodVulnerableSecurityCheckMethodsTestCheck");
    }

    @Test
    void testingGoodCase6() {
        performAnalysis("vulnerablesecuritycheckmethodstest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertNoVSCBug("goodVulnerableSecurityCheckMethodsTestCheck2");
    }

    @Test
    void testingGoodCase7() {
        performAnalysis("vulnerablesecuritycheckmethodstest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertNoVSCBug("goodVulnerableSecurityCheckMethodsTestCheck3");
    }

    @Test
    void testingGoodCase8() {
        performAnalysis("vulnerablesecuritycheckmethodstest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertNoVSCBug("goodVulnerableSecurityCheckMethodsTestCheck4");
    }

    @Test
    void testingGoodCase9() {
        performAnalysis("vulnerablesecuritycheckmethodstest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertNoVSCBug("goodVulnerableSecurityCheckMethodsTestCheck5");
    }

    @Test
    void testingGoodCase10() {
        performAnalysis("vulnerablesecuritycheckmethodstest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertNoVSCBug("badCalled");
    }

    @Test
    void testingGoodCase11() {
        performAnalysis("vulnerablesecuritycheckmethodstest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertNoVSCBug("goodVulnerableSecurityCheckMethodsTestCheck6");
    }

    @Test
    void testingGoodCase12() {
        performAnalysis("vulnerablesecuritycheckmethodstest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertNoVSCBug("goodVulnerableSecurityCheckMethodsTestCheck7");
    }

    @Test
    void testingGoodCase13() {
        performAnalysis("vulnerablesecuritycheckmethodstest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertNoVSCBug("goodFindVulnerableSecurityCheckMethodsCheck5");
    }

    @Test
    void testingGoodCase14() {
        performAnalysis("vulnerablesecuritycheckmethodstest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertNoVSCBug("goodVulnerableSecurityCheckMethodsTestCheck6");
    }

    @Test
    void testingGoodCase15() {
        performAnalysis("vulnerablesecuritycheckmethodstest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertNoVSCBug("goodVulnerableSecurityCheckMethodsTestCheck7");
    }

    @Test
    void testingGoodCase16() {
        performAnalysis("vulnerablesecuritycheckmethodstest/FindVulnerableSecurityCheckMethodsTest.class");
        assertNoVSCBug("goodVulnerableSecurityCheckMethodsTestCheck6");
    }

    @Test
    void testingGoodCase17() {
        performAnalysis("vulnerablesecuritycheckmethodstest/FindVulnerableSecurityCheckMethodsTest.class");
        assertNoVSCBug("goodVulnerableSecurityCheckMethodsTestCheck7");
    }

    @Test
    void testingGoodCase18() {
        performAnalysis("vulnerablesecuritycheckmethodstest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertNoVSCBug("goodVulnerableSecurityCheckMethodsTestCheck8");
    }

    private void assertNoVSCBug(String methodName) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inMethod(methodName)
                .build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }

    private void assertVSCBug(String methodName, int lineNumber) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inMethod(methodName)
                .atLine(lineNumber)
                .build();
        assertThat(getBugCollection(), hasItem(bugTypeMatcher));
    }
}
