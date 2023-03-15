package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class VulnerableSecurityCheckMethodsTest extends AbstractIntegrationTest {
    @Test
    public void testingAllCases() {
        performAnalysis("VulnerableSecurityCheckMethodsTest/FindVulnerableSecurityCheckMethodsTest.class");

        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("VSC_FIND_VULNERABLE_SECURITY_CHECK_METHODS").build();
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcher));
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
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("VSC_FIND_VULNERABLE_SECURITY_CHECK_METHODS")
                .inMethod("goodVulnerableSecurityCheckMethodsTestCheck").build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }

    @Test
    public void testingGoodCase2() {
        performAnalysis("VulnerableSecurityCheckMethodsTest/FindVulnerableSecurityCheckMethodsTest.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("VSC_FIND_VULNERABLE_SECURITY_CHECK_METHODS")
                .inMethod("goodVulnerableSecurityCheckMethodsTestCheck2").build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }

    @Test
    public void testingGoodCase3() {
        performAnalysis("VulnerableSecurityCheckMethodsTest/FindVulnerableSecurityCheckMethodsTest.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("VSC_FIND_VULNERABLE_SECURITY_CHECK_METHODS")
                .inMethod("goodVulnerableSecurityCheckMethodsTestCheck4").build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }

    @Test
    public void testingAllCases2() {
        performAnalysis("VulnerableSecurityCheckMethodsTest/GoodVulnerableSecurityCheckMethodsTest.class");

        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("VSC_FIND_VULNERABLE_SECURITY_CHECK_METHODS").build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }

    @Test
    public void testingGoodCase4() {
        performAnalysis("VulnerableSecurityCheckMethodsTest/GoodVulnerableSecurityCheckMethodsTest.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("VSC_FIND_VULNERABLE_SECURITY_CHECK_METHODS")
                .inMethod("goodVulnerableSecurityCheckMethodsTestCheck3").build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }

    @Test
    public void testingGoodCase5() {
        performAnalysis("VulnerableSecurityCheckMethodsTest/GoodVulnerableSecurityCheckMethodsTest.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("VSC_FIND_VULNERABLE_SECURITY_CHECK_METHODS")
                .inMethod("goodVulnerableSecurityCheckMethodsTestCheck").build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }

    @Test
    public void testingGoodCase6() {
        performAnalysis("VulnerableSecurityCheckMethodsTest/GoodVulnerableSecurityCheckMethodsTest.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("VSC_FIND_VULNERABLE_SECURITY_CHECK_METHODS")
                .inMethod("goodVulnerableSecurityCheckMethodsTestCheck2").build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }

    @Test
    public void testingGoodCase7() {
        performAnalysis("VulnerableSecurityCheckMethodsTest/GoodVulnerableSecurityCheckMethodsTest.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("VSC_FIND_VULNERABLE_SECURITY_CHECK_METHODS")
                .inMethod("goodVulnerableSecurityCheckMethodsTestCheck4").build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }
}
