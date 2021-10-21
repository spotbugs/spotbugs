package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class FindPotentialSecurityCheckBasedOnUntrustedSourceTest extends AbstractIntegrationTest {

    @Test
    public void test() throws Exception {
        performAnalysis("PotentialSecurityCheckBasedOnUntrustedSource.class",
                "PotentialSecurityCheckBasedOnUntrustedSource$1.class",
                "PotentialSecurityCheckBasedOnUntrustedSource$2.class");

        assertNumOfUSCBugs(1);
        assertUSCBug("badOpenFile", 11);
    }

    private void assertNumOfUSCBugs(int num) throws Exception {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("USC_POTENTIAL_SECURITY_CHECK_BASED_ON_UNTRUSED_SOURCE").build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertUSCBug(String methodName, int line) throws Exception {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("USC_POTENTIAL_SECURITY_CHECK_BASED_ON_UNTRUSED_SOURCE")
                .inClass("PotentialSecurityCheckBasedOnUntrustedSource")
                .inMethod(methodName)
                .atLine(line)
                .build();

        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }
}
