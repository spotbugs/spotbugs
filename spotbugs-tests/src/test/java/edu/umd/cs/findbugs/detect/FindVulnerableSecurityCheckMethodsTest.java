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
        performAnalysis("vulnerablesecuritycheckmethodstest/FindVulnerableSecurityCheckMethodsTest.class",
                "vulnerablesecuritycheckmethodstest/SecurityManager.class");

        BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .build();
        assertThat(getBugCollection(), containsExactly(7, bugInstanceMatcher));

        assertVSCBug("badFindVulnerableSecurityCheckMethodsCheck", 23);
        assertVSCBug("badFindVulnerableSecurityCheckMethodsCheck2", 37);
        assertVSCBug("badFindVulnerableSecurityCheckMethodsCheck3", 53);
        assertVSCBug("badCalled", 77);
        assertVSCBug("badFindVulnerableSecurityCheckMethodsCheck4", 94);
        assertVSCBug("badFindVulnerableSecurityCheckMethodsCheck5", 111);
        assertVSCBug("badFindVulnerableSecurityCheckMethodsCheck6", 128);

    }

    @Test
    void testingGoodCases() {
        performAnalysis("vulnerablesecuritycheckmethodstest/GoodVulnerableSecurityCheckMethodsTest.class",
                "vulnerablesecuritycheckmethodstest/FindVulnerableSecurityCheckMethodsTest.class",
                "vulnerablesecuritycheckmethodstest/SecurityManager.class");
        assertNoVSCBug("goodvulnerablesecuritycheckmethodstestCheck");
        assertNoVSCBug("goodVulnerableSecurityCheckMethodsTestCheck2");
        assertNoVSCBug("goodVulnerableSecurityCheckMethodsTestCheck4");
        assertNoVSCBug("goodVulnerableSecurityCheckMethodsTestCheck5");
        assertNoVSCBug("goodVulnerableSecurityCheckMethodsTestCheck6");
        assertNoVSCBug("goodVulnerableSecurityCheckMethodsTestCheck7");

        BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inClass("GoodVulnerableSecurityCheckMethodsTest")
                .build();
        assertThat(getBugCollection(), containsExactly(0, bugInstanceMatcher));
    }

    private void assertNoVSCBug(String methodName) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inClass("FindVulnerableSecurityCheckMethodsTest")
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
