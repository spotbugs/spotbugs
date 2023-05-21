package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

import org.junit.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class VulnerableSecurityCheckMethodsTest extends AbstractIntegrationTest {
    static String bugType = "VSC_FIND_VULNERABLE_SECURITY_CHECK_METHODS";

    @Test
    public void testingBadCases() {
        performAnalysis("vulnerablesecuritycheckmethodstest/FindVulnerableSecurityCheckMethodsTest.class");

        BugInstanceMatcher bugInstanceMatcher = createBugInstanceMatcher();
        assertThat(getBugCollection(), containsExactly(6, bugInstanceMatcher));
    }

    @Test
    public void testingBadCase1() {
        performAnalysis("vulnerablesecuritycheckmethodstest/FindVulnerableSecurityCheckMethodsTest.class");
        assertVSCBug("badFindVulnerableSecurityCheckMethodsCheck", 23);
    }

    @Test
    public void testingBadCase2() {
        performAnalysis("vulnerablesecuritycheckmethodstest/FindVulnerableSecurityCheckMethodsTest.class");
        assertVSCBug("badFindVulnerableSecurityCheckMethodsCheck2", 37);
    }

    @Test
    public void testingBadCase3() {
        performAnalysis("vulnerablesecuritycheckmethodstest/FindVulnerableSecurityCheckMethodsTest.class");
        assertVSCBug("badFindVulnerableSecurityCheckMethodsCheck3", 53);
    }

    @Test
    public void testingBadCase4() {
        performAnalysis("vulnerablesecuritycheckmethodstest/FindVulnerableSecurityCheckMethodsTest.class");
        assertVSCBug("badFindVulnerableSecurityCheckMethodsCheck4", 94);
    }
    /*
    @Test
    public void testingBadCase5() {
        performAnalysis("vulnerablesecuritycheckmethodstest/FindVulnerableSecurityCheckMethodsTest.class");
        assertVSCBug("badFindVulnerableSecurityCheckMethodsCheck5", 94);
    }


    @Test
    public void testingBadCase6() {
        performAnalysis("vulnerablesecuritycheckmethodstest/FindVulnerableSecurityCheckMethodsTest.class");
        assertVSCBug("badCalled", 111);
    }
    */

    @Test
    public void testingGoodCase1() {
        performAnalysis("vulnerablesecuritycheckmethodstest/FindVulnerableSecurityCheckMethodsTest.class");
        assertNoVSCBug("goodvulnerablesecuritycheckmethodstestCheck");
    }

    @Test
    public void testingGoodCase2() {
        performAnalysis("vulnerablesecuritycheckmethodstest/FindVulnerableSecurityCheckMethodsTest.class");
        assertNoVSCBug("goodVulnerableSecurityCheckMethodsTestCheck2");
    }

    @Test
    public void testingGoodCase3() {
        performAnalysis("vulnerablesecuritycheckmethodstest/FindVulnerableSecurityCheckMethodsTest.class");
        assertNoVSCBug("goodVulnerableSecurityCheckMethodsTestCheck4");
    }

    @Test
    public void testingGoodCase4() {
        performAnalysis("vulnerablesecuritycheckmethodstest/FindVulnerableSecurityCheckMethodsTest.class");
        assertNoVSCBug("goodVulnerableSecurityCheckMethodsTestCheck5");
    }

    @Test
    public void testingGoodCase5() {
        performAnalysis("vulnerablesecuritycheckmethodstest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertNoVSCBug("goodVulnerableSecurityCheckMethodsTestCheck");
    }

    @Test
    public void testingGoodCase6() {
        performAnalysis("vulnerablesecuritycheckmethodstest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertNoVSCBug("goodVulnerableSecurityCheckMethodsTestCheck2");
    }

    @Test
    public void testingGoodCase7() {
        performAnalysis("vulnerablesecuritycheckmethodstest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertNoVSCBug("goodVulnerableSecurityCheckMethodsTestCheck3");
    }

    @Test
    public void testingGoodCase8() {
        performAnalysis("vulnerablesecuritycheckmethodstest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertNoVSCBug("goodVulnerableSecurityCheckMethodsTestCheck4");
    }

    @Test
    public void testingGoodCase9() {
        performAnalysis("vulnerablesecuritycheckmethodstest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertNoVSCBug("goodVulnerableSecurityCheckMethodsTestCheck5");
    }

    @Test
    public void testingGoodCase10() {
        performAnalysis("vulnerablesecuritycheckmethodstest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertNoVSCBug("badCalled");
    }

    @Test
    public void testingGoodCase11() {
        performAnalysis("vulnerablesecuritycheckmethodstest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertNoVSCBug("goodVulnerableSecurityCheckMethodsTestCheck6");
    }

    @Test
    public void testingGoodCase12() {
        performAnalysis("vulnerablesecuritycheckmethodstest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertNoVSCBug("goodVulnerableSecurityCheckMethodsTestCheck7");
    }

    @Test
    public void testingGoodCase13() {
        performAnalysis("vulnerablesecuritycheckmethodstest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertNoVSCBug("goodFindVulnerableSecurityCheckMethodsCheck5");
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

    private void assertNoVSCBug(String methodName) {
        final BugInstanceMatcher bugTypeMatcher = createBugInstanceMatcher(methodName);
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }

    private void assertVSCBug(String methodName, int lineNumber) {
        final BugInstanceMatcher bugTypeMatcher = createBugInstanceMatcher(methodName, lineNumber);
        assertThat(getBugCollection(), hasItem(bugTypeMatcher));
        //assertThat("", getBugCollection().getCollection().contains(new BugInstance("VSC_FIND_VULNERABLE_SECURITY_CHECK_METHODS", Priorities.NORMAL_PRIORITY)));
        //assertThat(getBugCollection(), hasItem(new BugInstance("VSC_FIND_VULNERABLE_SECURITY_CHECK_METHODS", Priorities.NORMAL_PRIORITY)));
    }
}
