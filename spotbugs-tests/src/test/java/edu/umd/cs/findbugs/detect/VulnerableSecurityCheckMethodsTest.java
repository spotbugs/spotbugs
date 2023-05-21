package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class VulnerableSecurityCheckMethodsTest extends AbstractIntegrationTest {
    @Test
    public void testingBadCases() {
        performAnalysis("vulnerablesecuritycheckmethodstest/FindVulnerableSecurityCheckMethodsTest.class");
        assertError(1, "badFindVulnerableSecurityCheckMethodsCheck");
    }

    @Test
    public void testingGoodCase1() {
        performAnalysis("vulnerablesecuritycheckmethodstest/FindVulnerableSecurityCheckMethodsTest.class");
        assertError(0, "goodvulnerablesecuritycheckmethodstestCheck");
    }

    @Test
    public void testingGoodCase2() {
        performAnalysis("vulnerablesecuritycheckmethodstest/FindVulnerableSecurityCheckMethodsTest.class");
        assertError(0, "goodVulnerableSecurityCheckMethodsTestCheck2");
    }

    @Test
    public void testingGoodCase3() {
        performAnalysis("vulnerablesecuritycheckmethodstest/FindVulnerableSecurityCheckMethodsTest.class");
        assertError(0, "goodVulnerableSecurityCheckMethodsTestCheck4");
    }

    @Test
    public void testingGoodCase5() {
        performAnalysis("vulnerablesecuritycheckmethodstest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertError(0, "goodVulnerableSecurityCheckMethodsTestCheck");
    }

    @Test
    public void testingGoodCase6() {
        performAnalysis("vulnerablesecuritycheckmethodstest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertError(0, "goodVulnerableSecurityCheckMethodsTestCheck2");
    }

    @Test
    public void testingGoodCase7() {
        performAnalysis("vulnerablesecuritycheckmethodstest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertError(0, "goodVulnerableSecurityCheckMethodsTestCheck4");
    }

    private BugInstanceMatcher createBugInstanceMatcher(String methodName) {
        String bugType = "VSC_FIND_VULNERABLE_SECURITY_CHECK_METHODS";
        return new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inMethod(methodName)
                .build();
    }

    private void assertError(int count, String methodName) {
        BugInstanceMatcher bugTypeMatcher = createBugInstanceMatcher(methodName);
        assertThat(getBugCollection(), containsExactly(count, bugTypeMatcher));
    }
}