package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

/**
 * SpotBugs should check for bad combinations of date format flags in SimpleDateFormat.
 *
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/637">GitHub issue</a>
 * @since 4.4.2
 */
public class Issue637Test extends AbstractIntegrationTest {
    private static final String DESIRED_BUG_TYPE = "FS_BAD_DATE_FORMAT_FLAG_COMBO";

    /**
     * Expects 25 "FS_BAD_DATE_FORMAT_FLAG_COMBO" errors in the "Issue637Errors.class" file.
     */
    @Test
    public void testClassWithErrors() {
        performAnalysis("ghIssues/Issue637Errors.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(DESIRED_BUG_TYPE)
                .build();
        assertThat(getBugCollection(), containsExactly(25, bugTypeMatcher));
    }

    /**
     * Expects no "FS_BAD_DATE_FORMAT_FLAG_COMBO" errors in the "Issue637NoErrors.class" file.
     */
    @Test
    public void testClassWithoutErrors() {
        performAnalysis("ghIssues/Issue637NoErrors.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(DESIRED_BUG_TYPE)
                .build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }

}
