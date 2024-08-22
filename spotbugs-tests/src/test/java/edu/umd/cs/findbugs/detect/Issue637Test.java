package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

import org.junit.jupiter.api.Test;

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
     * Expects total of 25 errors that are:
     *  - of type defined in DESIRED_BUG_TYPE
     *  - inside the Issue637Errors class
     *  - was called in a specified function (parameter)
     *  - and caught on a proper line (parameter)
     */
    @Test
    public void testClassWithErrors() {
        performAnalysis("ghIssues/Issue637Errors.class");
        assertNumOfBug637(25);

        int line;
        for (line = 28; line <= 32; line++)
            assertBug637("oneArgConstructorTest", line);
        for (line = 47; line <= 51; line++)
            assertBug637("twoArgLocaleConstructorTest", line);
        for (line = 67; line <= 71; line++)
            assertBug637("twoArgDateFormatSymbolConstructorTest", line);
        for (line = 90; line <= 94; line++)
            assertBug637("applyPatternTest", line);
        for (line = 101; line <= 105; line++)
            assertBug637("applyLocalizedPatternTest", line);
    }

    /**
     * Expects no errors of type defined in DESIRED_BUG_TYPE
     */
    @Test
    public void testClassWithoutErrors() {
        performAnalysis("ghIssues/Issue637NoErrors.class");
        assertNumOfBug637(0);
    }

    private void assertNumOfBug637(int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(DESIRED_BUG_TYPE).build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertBug637(String method, int line) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType(DESIRED_BUG_TYPE)
                .inClass("Issue637Errors")
                .inMethod(method)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }
}
