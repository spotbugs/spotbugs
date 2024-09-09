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
        performAnalysis("ghIssues/issue637/Issue637Errors.class");
        assertNumOfBug637(50);

        assertBug637("oneArgConstructorTest", 31);
        assertBug637("oneArgConstructorTest", 32);
        assertBug637("oneArgConstructorTest", 33);
        assertBug637("oneArgConstructorTest", 34);
        assertBug637("oneArgConstructorTest", 35);

        assertBug637("twoArgLocaleConstructorTest", 50);
        assertBug637("twoArgLocaleConstructorTest", 51);
        assertBug637("twoArgLocaleConstructorTest", 52);
        assertBug637("twoArgLocaleConstructorTest", 53);
        assertBug637("twoArgLocaleConstructorTest", 54);

        assertBug637("twoArgDateFormatSymbolConstructorTest", 70);
        assertBug637("twoArgDateFormatSymbolConstructorTest", 71);
        assertBug637("twoArgDateFormatSymbolConstructorTest", 72);
        assertBug637("twoArgDateFormatSymbolConstructorTest", 73);
        assertBug637("twoArgDateFormatSymbolConstructorTest", 74);

        assertBug637("applyPatternTest", 93);
        assertBug637("applyPatternTest", 94);
        assertBug637("applyPatternTest", 95);
        assertBug637("applyPatternTest", 96);
        assertBug637("applyPatternTest", 97);

        assertBug637("applyLocalizedPatternTest", 110);
        assertBug637("applyLocalizedPatternTest", 111);
        assertBug637("applyLocalizedPatternTest", 112);
        assertBug637("applyLocalizedPatternTest", 113);
        assertBug637("applyLocalizedPatternTest", 114);

        assertBug637("ofPatternTest", 118);
        assertBug637("ofPatternTest", 119);
        assertBug637("ofPatternTest", 120);
        assertBug637("ofPatternTest", 121);
        assertBug637("ofPatternTest", 121);

        assertBug637("ofPatternLocaleTest", 128);
        assertBug637("ofPatternLocaleTest", 129);
        assertBug637("ofPatternLocaleTest", 130);
        assertBug637("ofPatternLocaleTest", 131);
        assertBug637("ofPatternLocaleTest", 132);

        assertBug637("getInstanceTest", 136);
        assertBug637("getInstanceTest", 137);
        assertBug637("getInstanceTest", 138);
        assertBug637("getInstanceTest", 139);
        assertBug637("getInstanceTest", 140);

        assertBug637("getInstanceLocaleTest", 146);
        assertBug637("getInstanceLocaleTest", 147);
        assertBug637("getInstanceLocaleTest", 148);
        assertBug637("getInstanceLocaleTest", 149);
        assertBug637("getInstanceLocaleTest", 150);

        assertBug637("getInstanceTimezoneTest", 156);
        assertBug637("getInstanceTimezoneTest", 157);
        assertBug637("getInstanceTimezoneTest", 158);
        assertBug637("getInstanceTimezoneTest", 159);
        assertBug637("getInstanceTimezoneTest", 160);
    }

    /**
     * Expects no errors of type defined in DESIRED_BUG_TYPE
     */
    @Test
    void testClassWithoutErrors() {
        performAnalysis("ghIssues/issue637/Issue637NoErrors.class");
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
