package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;

class FindVulnerableSecurityCheckMethodsTest extends AbstractIntegrationTest {

    private static final String BUG_TYPE = "VSC_VULNERABLE_SECURITY_CHECK_METHODS";

    @Test
    void testingBadCases() {
        performAnalysis("vulnerablesecuritycheckmethodstest/FindVulnerableSecurityCheckMethodsTest.class",
                "vulnerablesecuritycheckmethodstest/SecurityManager.class");

        assertBugTypeCount(BUG_TYPE, 7);
        String className = "FindVulnerableSecurityCheckMethodsTest";

        assertBugInMethodAtLine(BUG_TYPE, className, "badFindVulnerableSecurityCheckMethodsCheck", 23);
        assertBugInMethodAtLine(BUG_TYPE, className, "badFindVulnerableSecurityCheckMethodsCheck2", 37);
        assertBugInMethodAtLine(BUG_TYPE, className, "badFindVulnerableSecurityCheckMethodsCheck3", 53);
        assertBugInMethodAtLine(BUG_TYPE, className, "badCalled", 77);
        assertBugInMethodAtLine(BUG_TYPE, className, "badFindVulnerableSecurityCheckMethodsCheck4", 94);
        assertBugInMethodAtLine(BUG_TYPE, className, "badFindVulnerableSecurityCheckMethodsCheck5", 111);
        assertBugInMethodAtLine(BUG_TYPE, className, "badFindVulnerableSecurityCheckMethodsCheck6", 128);

    }

    @Test
    void testingGoodCases() {
        performAnalysis("vulnerablesecuritycheckmethodstest/GoodVulnerableSecurityCheckMethodsTest.class",
                "vulnerablesecuritycheckmethodstest/FindVulnerableSecurityCheckMethodsTest.class",
                "vulnerablesecuritycheckmethodstest/SecurityManager.class");
        assertNoBugInMethod(BUG_TYPE, "FindVulnerableSecurityCheckMethodsTest", "goodvulnerablesecuritycheckmethodstestCheck");
        assertNoBugInMethod(BUG_TYPE, "FindVulnerableSecurityCheckMethodsTest", "goodVulnerableSecurityCheckMethodsTestCheck2");
        assertNoBugInMethod(BUG_TYPE, "FindVulnerableSecurityCheckMethodsTest", "goodVulnerableSecurityCheckMethodsTestCheck4");
        assertNoBugInMethod(BUG_TYPE, "FindVulnerableSecurityCheckMethodsTest", "goodVulnerableSecurityCheckMethodsTestCheck5");
        assertNoBugInMethod(BUG_TYPE, "FindVulnerableSecurityCheckMethodsTest", "goodVulnerableSecurityCheckMethodsTestCheck6");
        assertNoBugInMethod(BUG_TYPE, "FindVulnerableSecurityCheckMethodsTest", "goodVulnerableSecurityCheckMethodsTestCheck7");

        assertNoBugInClass(BUG_TYPE, "GoodVulnerableSecurityCheckMethodsTest");
    }
}
