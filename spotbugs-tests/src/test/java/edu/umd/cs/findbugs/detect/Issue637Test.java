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
     * Expects total of 80 errors that are:
     *  - of type defined in DESIRED_BUG_TYPE
     *  - inside the Issue637Errors class
     *  - was called in a specified function (parameter)
     *  - and caught on a proper line (parameter)
     */
    @Test
    void testClassWithErrors() {
        performAnalysis("ghIssues/issue637/Issue637Errors.class");
        assertNumOfBug637(80);

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

        assertBug637("ofPatternLocaleTest", 120);
        assertBug637("ofPatternLocaleTest", 121);
        assertBug637("ofPatternLocaleTest", 122);
        assertBug637("ofPatternLocaleTest", 123);
        assertBug637("ofPatternLocaleTest", 124);

        assertBug637("getInstanceTest", 128);
        assertBug637("getInstanceTest", 129);
        assertBug637("getInstanceTest", 130);
        assertBug637("getInstanceTest", 131);
        assertBug637("getInstanceTest", 132);

        assertBug637("getInstanceLocaleTest", 138);
        assertBug637("getInstanceLocaleTest", 139);
        assertBug637("getInstanceLocaleTest", 140);
        assertBug637("getInstanceLocaleTest", 141);
        assertBug637("getInstanceLocaleTest", 142);

        assertBug637("getInstanceTimezoneTest", 148);
        assertBug637("getInstanceTimezoneTest", 149);
        assertBug637("getInstanceTimezoneTest", 150);
        assertBug637("getInstanceTimezoneTest", 151);
        assertBug637("getInstanceTimezoneTest", 152);

        assertBug637("dateTimeFormatterTest", 158);
        assertBug637("dateTimeFormatterTest", 159);
        assertBug637("dateTimeFormatterTest", 162);
        assertBug637("dateTimeFormatterTest", 163);
        assertBug637("dateTimeFormatterTest", 164);
        assertBug637("dateTimeFormatterTest", 165);
        assertBug637("dateTimeFormatterTest", 168);
        assertBug637("dateTimeFormatterTest", 171);
        assertBug637("dateTimeFormatterTest", 172);
        assertBug637("dateTimeFormatterTest", 173);
        assertBug637("dateTimeFormatterTest", 174);
        assertBug637("dateTimeFormatterTest", 175);
        assertBug637("dateTimeFormatterTest", 176);
        assertBug637("dateTimeFormatterTest", 177);
        assertBug637("dateTimeFormatterTest", 178);
        assertBug637("dateTimeFormatterTest", 179);
        assertBug637("dateTimeFormatterTest", 180);
        assertBug637("dateTimeFormatterTest", 181);
        assertBug637("dateTimeFormatterTest", 182);
        assertBug637("dateTimeFormatterTest", 183);
        assertBug637("dateTimeFormatterTest", 184);
        assertBug637("dateTimeFormatterTest", 185);
        assertBug637("dateTimeFormatterTest", 186);
        assertBug637("dateTimeFormatterTest", 189);
        assertBug637("dateTimeFormatterTest", 190);
        assertBug637("dateTimeFormatterTest", 193);
        assertBug637("dateTimeFormatterTest", 194);
        assertBug637("dateTimeFormatterTest", 197);
        assertBug637("dateTimeFormatterTest", 198);
        assertBug637("dateTimeFormatterTest", 199);
        assertBug637("dateTimeFormatterTest", 200);
        assertBug637("dateTimeFormatterTest", 201);
        assertBug637("dateTimeFormatterTest", 202);
        assertBug637("dateTimeFormatterTest", 205);
        assertBug637("dateTimeFormatterTest", 206);
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
