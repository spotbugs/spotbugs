package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;

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
     *  - inside the Issue637Errors class (as className)
     *  - was called in a specified function (parameter)
     *  - and caught on a proper line (parameter)
     */
    @Test
    void testClassWithErrors() {
        String className = "Issue637Errors";
        performAnalysis("ghIssues/issue637/" + className + ".class");
        assertBugTypeCount(DESIRED_BUG_TYPE, 80);

        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "oneArgConstructorTest", 31);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "oneArgConstructorTest", 32);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "oneArgConstructorTest", 33);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "oneArgConstructorTest", 34);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "oneArgConstructorTest", 35);

        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "twoArgLocaleConstructorTest", 50);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "twoArgLocaleConstructorTest", 51);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "twoArgLocaleConstructorTest", 52);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "twoArgLocaleConstructorTest", 53);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "twoArgLocaleConstructorTest", 54);

        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "twoArgDateFormatSymbolConstructorTest", 70);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "twoArgDateFormatSymbolConstructorTest", 71);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "twoArgDateFormatSymbolConstructorTest", 72);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "twoArgDateFormatSymbolConstructorTest", 73);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "twoArgDateFormatSymbolConstructorTest", 74);

        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "applyPatternTest", 93);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "applyPatternTest", 94);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "applyPatternTest", 95);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "applyPatternTest", 96);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "applyPatternTest", 97);

        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "applyLocalizedPatternTest", 110);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "applyLocalizedPatternTest", 111);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "applyLocalizedPatternTest", 112);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "applyLocalizedPatternTest", 113);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "applyLocalizedPatternTest", 114);

        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "ofPatternLocaleTest", 120);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "ofPatternLocaleTest", 121);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "ofPatternLocaleTest", 122);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "ofPatternLocaleTest", 123);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "ofPatternLocaleTest", 124);

        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "getInstanceTest", 128);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "getInstanceTest", 129);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "getInstanceTest", 130);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "getInstanceTest", 131);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "getInstanceTest", 132);

        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "getInstanceLocaleTest", 138);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "getInstanceLocaleTest", 139);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "getInstanceLocaleTest", 140);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "getInstanceLocaleTest", 141);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "getInstanceLocaleTest", 142);

        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "getInstanceTimezoneTest", 148);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "getInstanceTimezoneTest", 149);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "getInstanceTimezoneTest", 150);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "getInstanceTimezoneTest", 151);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "getInstanceTimezoneTest", 152);

        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 158);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 159);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 162);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 163);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 164);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 165);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 168);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 171);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 172);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 173);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 174);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 175);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 176);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 177);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 178);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 179);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 180);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 181);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 182);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 183);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 184);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 185);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 186);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 189);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 190);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 193);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 194);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 197);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 198);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 199);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 200);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 201);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 202);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 205);
        assertBugInMethodAtLine(DESIRED_BUG_TYPE, className, "dateTimeFormatterTest", 206);
    }

    /**
     * Expects no errors of type defined in DESIRED_BUG_TYPE
     */
    @Test
    void testClassWithoutErrors() {
        performAnalysis("ghIssues/issue637/Issue637NoErrors.class");
        assertNoBugType(DESIRED_BUG_TYPE);
    }
}
