package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class VulnerableSecurityCheckMethodsTest extends AbstractIntegrationTest {

    BugInstanceMatcher bugTypeMatcherGeneral;
    BugInstanceMatcher bugTypeMatcherForGoodChecks;

    BugInstanceMatcher bugTypeMatcherForGoodChecks2;
    BugInstanceMatcher bugTypeMatcherForGoodChecks4;

    public VulnerableSecurityCheckMethodsTest() {
        bugTypeMatcherGeneral = new BugInstanceMatcherBuilder()
                .bugType("VSC_FIND_VULNERABLE_SECURITY_CHECK_METHODS")
                .build();
        bugTypeMatcherForGoodChecks = new BugInstanceMatcherBuilder()
                .bugType("VSC_FIND_VULNERABLE_SECURITY_CHECK_METHODS")
                .inMethod("goodVulnerableSecurityCheckMethodsTestCheck")
                .build();
        bugTypeMatcherForGoodChecks2 = new BugInstanceMatcherBuilder()
                .bugType("VSC_FIND_VULNERABLE_SECURITY_CHECK_METHODS")
                .inMethod("goodVulnerableSecurityCheckMethodsTestCheck2").build();
        bugTypeMatcherForGoodChecks4 = new BugInstanceMatcherBuilder()
                .bugType("VSC_FIND_VULNERABLE_SECURITY_CHECK_METHODS")
                .inMethod("goodVulnerableSecurityCheckMethodsTestCheck4").build();
    }

    @Test
    public void testingAllCases() {
        performAnalysis("VulnerableSecurityCheckMethodsTest/FindVulnerableSecurityCheckMethodsTest.class");
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcherGeneral));
    }

    @Test
    public void testingBadCases() {
        performAnalysis("VulnerableSecurityCheckMethodsTest/FindVulnerableSecurityCheckMethodsTest.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("VSC_FIND_VULNERABLE_SECURITY_CHECK_METHODS")
                .inMethod("badFindVulnerableSecurityCheckMethodsCheck").build();
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcher));
    }

    @Test
    public void testingGoodCase1() {
        performAnalysis("VulnerableSecurityCheckMethodsTest/FindVulnerableSecurityCheckMethodsTest.class");
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcherForGoodChecks));
    }

    @Test
    public void testingGoodCase2() {
        performAnalysis("VulnerableSecurityCheckMethodsTest/FindVulnerableSecurityCheckMethodsTest.class");
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcherForGoodChecks2));
    }

    @Test
    public void testingGoodCase3() {
        performAnalysis("VulnerableSecurityCheckMethodsTest/FindVulnerableSecurityCheckMethodsTest.class");
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcherForGoodChecks4));
    }

    @Test
    public void testingAllCases2() {
        performAnalysis("VulnerableSecurityCheckMethodsTest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcherGeneral));
    }

    @Test
    public void testingGoodCase5() {
        performAnalysis("VulnerableSecurityCheckMethodsTest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcherForGoodChecks));
    }

    @Test
    public void testingGoodCase6() {
        performAnalysis("VulnerableSecurityCheckMethodsTest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcherForGoodChecks2));
    }

    @Test
    public void testingGoodCase7() {
        performAnalysis("VulnerableSecurityCheckMethodsTest/GoodVulnerableSecurityCheckMethodsTest.class");
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcherForGoodChecks4));
    }
}
