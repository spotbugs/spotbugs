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
 */
class Issue637Test extends AbstractIntegrationTest {
    private static final String DESIRED_BUG_TYPE = "FS_BAD_DATE_FORMAT_FLAG_COMBO";

    /**
     * Expects total of 25 errors that are:
     *  - of type defined in DESIRED_BUG_TYPE
     *  - inside the Issue637Errors class
     *  - was called in a specified function (parameter)
     *  - and caught on a proper line (parameter)
     */
    @Test
    void testClassWithErrors() {
        performAnalysis("ghIssues/Issue637Errors.class");
        assertNumOfBug637(25);

        assertBug637("oneArgConstructorTest", 28);
        assertBug637("oneArgConstructorTest", 29);
        assertBug637("oneArgConstructorTest", 30);
        assertBug637("oneArgConstructorTest", 31);
        assertBug637("oneArgConstructorTest", 32);

        assertBug637("twoArgLocaleConstructorTest", 47);
        assertBug637("twoArgLocaleConstructorTest", 48);
        assertBug637("twoArgLocaleConstructorTest", 49);
        assertBug637("twoArgLocaleConstructorTest", 50);
        assertBug637("twoArgLocaleConstructorTest", 51);

        assertBug637("twoArgDateFormatSymbolConstructorTest", 67);
        assertBug637("twoArgDateFormatSymbolConstructorTest", 68);
        assertBug637("twoArgDateFormatSymbolConstructorTest", 69);
        assertBug637("twoArgDateFormatSymbolConstructorTest", 70);
        assertBug637("twoArgDateFormatSymbolConstructorTest", 71);

        assertBug637("applyPatternTest", 90);
        assertBug637("applyPatternTest", 91);
        assertBug637("applyPatternTest", 92);
        assertBug637("applyPatternTest", 93);
        assertBug637("applyPatternTest", 94);

        assertBug637("applyLocalizedPatternTest", 101);
        assertBug637("applyLocalizedPatternTest", 102);
        assertBug637("applyLocalizedPatternTest", 103);
        assertBug637("applyLocalizedPatternTest", 104);
        assertBug637("applyLocalizedPatternTest", 105);
    }

    /**
     * Expects no errors of type defined in DESIRED_BUG_TYPE
     */
    @Test
    void testClassWithoutErrors() {
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
