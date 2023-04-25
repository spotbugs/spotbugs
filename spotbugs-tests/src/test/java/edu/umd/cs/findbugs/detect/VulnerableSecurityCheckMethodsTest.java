package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class VulnerableSecurityCheckMethodsTest extends AbstractIntegrationTest {
    static String bugType = "VSC_FIND_VULNERABLE_SECURITY_CHECK_METHODS";

    @Test
    public void testingBadCases() {
        performAnalysis("vulnerableSecurityCheckMethodsTest/FindVulnerableSecurityCheckMethodsTest.class");
        assertError(1, true, "badFindVulnerableSecurityCheckMethodsCheck");
    }

    @Test
    public void testingGoodCase1() {
        performAnalysis("vulnerableSecurityCheckMethodsTest/FindVulnerableSecurityCheckMethodsTest.class");
        assertError(0, true, "goodVulnerableSecurityCheckMethodsTestCheck");
    }

    @Test
    public void testingGoodCase2() {
        performAnalysis("vulnerableSecurityCheckMethodsTest/FindVulnerableSecurityCheckMethodsTest.class");
        assertError(0, true, "goodVulnerableSecurityCheckMethodsTestCheck2");
    }

    @Test
    public void testingGoodCase3() {
        performAnalysis("vulnerableSecurityCheckMethodsTest/FindVulnerableSecurityCheckMethodsTest.class");
        assertError(0, true, "goodVulnerableSecurityCheckMethodsTestCheck4");
    }

    @Test
    public void testingAllCases2() {
        performAnalysis("vulnerableSecurityCheckMethodsTest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertError(0, false, "");
    }

    @Test
    public void testingGoodCase5() {
        performAnalysis("vulnerableSecurityCheckMethodsTest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertError(0, true, "goodVulnerableSecurityCheckMethodsTestCheck");
    }

    @Test
    public void testingGoodCase6() {
        performAnalysis("vulnerableSecurityCheckMethodsTest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertError(0, true, "goodVulnerableSecurityCheckMethodsTestCheck2");
    }

    @Test
    public void testingGoodCase7() {
        performAnalysis("vulnerableSecurityCheckMethodsTest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertError(0, true, "goodVulnerableSecurityCheckMethodsTestCheck4");
    }

    private BugInstanceMatcher createBugInstanceMatcher(boolean withMethod, String methodName) {
        if (!withMethod) {
            return new BugInstanceMatcherBuilder()
                    .bugType(bugType)
                    .build();
        }
        return new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inMethod(methodName)
                .build();
    }

    private void assertError(int count, boolean withMethod, String methodName) {
        BugInstanceMatcher bugTypeMatcher = createBugInstanceMatcher(withMethod, methodName);
        assertThat(getBugCollection(), containsExactly(count, bugTypeMatcher));
    }
}
