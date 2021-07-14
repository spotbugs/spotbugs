package edu.umd.cs.findbugs.ba;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/1338">GitHub issue</a>
 */
public class Issue1338Test extends AbstractIntegrationTest {

    private static final String[] CLASS_LIST = { "../java11/module-info.class", "../java11/Issue1338.class", };

    /**
     * Test that calling a method call when initializing a try-with-resource variable doesn't result in redundant nullcheck of nonnull value.
     */
    @Test
    public void testMethodCallInTryWithResource() {
        performAnalysis(CLASS_LIST);
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder().bugType("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
                .build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }
}
