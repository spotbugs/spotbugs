package ghIssues.issue637;

import edu.umd.cs.findbugs.detect.DateFormatStringChecker;
import org.apache.commons.lang3.time.FastDateFormat;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;


/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/637">GitHub issue</a>
 */
public final class Issue637NoErrors {

    private static final String AM_PM_HOUR_1_12_CORRECT = "h:mm a";
    private static final String AM_PM_HOUR_0_11_CORRECT = "K:mm a";
    private static final String MILITARY_HOUR_0_23_CORRECT = "HH:mm";
    private static final String MILITARY_HOUR_1_24_CORRECT = "kk:mm";
    private static final String WEEK_YEAR_CORRECT = "YYYY-MM-dd 'W'ww";

    private Issue637NoErrors(){
        //To prevent instantiation
    }

    /**
     * Expects no errors when proper flag combinations are used in one argument constructor.
     */
    public static void oneArgConstructorTest(){
        SimpleDateFormat hour12format1 = new SimpleDateFormat(AM_PM_HOUR_1_12_CORRECT);
        SimpleDateFormat hour12format2 = new SimpleDateFormat(AM_PM_HOUR_0_11_CORRECT);
        SimpleDateFormat hour24format1 = new SimpleDateFormat(MILITARY_HOUR_0_23_CORRECT);
        SimpleDateFormat hour24format2 = new SimpleDateFormat(MILITARY_HOUR_1_24_CORRECT);
        SimpleDateFormat weekYear = new SimpleDateFormat(WEEK_YEAR_CORRECT);

        System.out.println(hour12format1.format(new Date()));
        System.out.println(hour12format2.format(new Date()));
        System.out.println(hour24format1.format(new Date()));
        System.out.println(hour24format2.format(new Date()));
        System.out.println(weekYear.format(new Date()));
    }

    /**
     * Expects no errors when proper flag combinations are used in two argument constructor with specified Locale
     */
    public static void twoArgLocaleConstructorTest(){
        Locale defaultLocale = new Locale("en", "US");

        SimpleDateFormat hour12format1 = new SimpleDateFormat(AM_PM_HOUR_1_12_CORRECT, defaultLocale);
        SimpleDateFormat hour12format2 = new SimpleDateFormat(AM_PM_HOUR_0_11_CORRECT, defaultLocale);
        SimpleDateFormat hour24format1 = new SimpleDateFormat(MILITARY_HOUR_0_23_CORRECT, defaultLocale);
        SimpleDateFormat hour24format2 = new SimpleDateFormat(MILITARY_HOUR_1_24_CORRECT, defaultLocale);
        SimpleDateFormat weekYear = new SimpleDateFormat(WEEK_YEAR_CORRECT, defaultLocale);

        System.out.println(hour12format1.format(new Date()));
        System.out.println(hour12format2.format(new Date()));
        System.out.println(hour24format1.format(new Date()));
        System.out.println(hour24format2.format(new Date()));
        System.out.println(weekYear.format(new Date()));
    }

    /**
     * Expects no errors when proper flag combinations are used in two argument constructor with specified date
     * format symbols
     */
    public static void twoArgDateFormatSymbolConstructorTest(){
        DateFormatSymbols dateFormatSymbols = new DateFormatSymbols(Locale.getDefault());

        SimpleDateFormat hour12format1 = new SimpleDateFormat(AM_PM_HOUR_1_12_CORRECT, dateFormatSymbols);
        SimpleDateFormat hour12format2 = new SimpleDateFormat(AM_PM_HOUR_0_11_CORRECT, dateFormatSymbols);
        SimpleDateFormat hour24format1 = new SimpleDateFormat(MILITARY_HOUR_0_23_CORRECT, dateFormatSymbols);
        SimpleDateFormat hour24format2 = new SimpleDateFormat(MILITARY_HOUR_1_24_CORRECT, dateFormatSymbols);
        SimpleDateFormat weekYear = new SimpleDateFormat(WEEK_YEAR_CORRECT, dateFormatSymbols);

        System.out.println(hour12format1.format(new Date()));
        System.out.println(hour12format2.format(new Date()));
        System.out.println(hour24format1.format(new Date()));
        System.out.println(hour24format2.format(new Date()));
        System.out.println(weekYear.format(new Date()));
    }

    /**
     * Expects no errors when proper flag combinations are used in "applyPattern" function
     */
    public static void applyPatternTest(){
        SimpleDateFormat hour12format1 = new SimpleDateFormat();
        SimpleDateFormat hour12format2 = new SimpleDateFormat();
        SimpleDateFormat hour24format1 = new SimpleDateFormat();
        SimpleDateFormat hour24format2 = new SimpleDateFormat();
        SimpleDateFormat weekYear = new SimpleDateFormat();

        hour12format1.applyPattern(AM_PM_HOUR_1_12_CORRECT);
        hour12format2.applyPattern(AM_PM_HOUR_0_11_CORRECT);
        hour24format1.applyPattern(MILITARY_HOUR_0_23_CORRECT);
        hour24format2.applyPattern(MILITARY_HOUR_1_24_CORRECT);
        weekYear.applyPattern(WEEK_YEAR_CORRECT);
    }

    /**
     * Expects no errors when proper format flag combinations are used in "applyLocalizedPattern" function
     */
    public static void applyLocalizedPatternTest(){
        SimpleDateFormat hour12format1 = new SimpleDateFormat();
        SimpleDateFormat hour12format2 = new SimpleDateFormat();
        SimpleDateFormat hour24format1 = new SimpleDateFormat();
        SimpleDateFormat hour24format2 = new SimpleDateFormat();
        SimpleDateFormat weekYear = new SimpleDateFormat();

        hour12format1.applyLocalizedPattern(AM_PM_HOUR_1_12_CORRECT);
        hour12format2.applyLocalizedPattern(AM_PM_HOUR_0_11_CORRECT);
        hour24format1.applyLocalizedPattern(MILITARY_HOUR_0_23_CORRECT);
        hour24format2.applyLocalizedPattern(MILITARY_HOUR_1_24_CORRECT);
        weekYear.applyLocalizedPattern(WEEK_YEAR_CORRECT);
    }

    public static void ofPatternTest(){
        DateTimeFormatter.ofPattern(AM_PM_HOUR_1_12_CORRECT);
        DateTimeFormatter.ofPattern(AM_PM_HOUR_0_11_CORRECT);
        DateTimeFormatter.ofPattern(MILITARY_HOUR_0_23_CORRECT);
        DateTimeFormatter.ofPattern(MILITARY_HOUR_1_24_CORRECT);
        DateTimeFormatter.ofPattern(WEEK_YEAR_CORRECT);
    }

    public static void ofPatternLocaleTest(){
        Locale defaultLocale = new Locale("en", "US");

        DateTimeFormatter.ofPattern(AM_PM_HOUR_1_12_CORRECT, defaultLocale);
        DateTimeFormatter.ofPattern(AM_PM_HOUR_0_11_CORRECT, defaultLocale);
        DateTimeFormatter.ofPattern(MILITARY_HOUR_0_23_CORRECT, defaultLocale);
        DateTimeFormatter.ofPattern(MILITARY_HOUR_1_24_CORRECT, defaultLocale);
        DateTimeFormatter.ofPattern(WEEK_YEAR_CORRECT, defaultLocale);
    }

    public static void getInstanceTest() {
        FastDateFormat.getInstance(AM_PM_HOUR_1_12_CORRECT);
        FastDateFormat.getInstance(AM_PM_HOUR_0_11_CORRECT);
        FastDateFormat.getInstance(MILITARY_HOUR_0_23_CORRECT);
        FastDateFormat.getInstance(MILITARY_HOUR_1_24_CORRECT);
        FastDateFormat.getInstance(WEEK_YEAR_CORRECT);
    }

    public static void getInstanceLocaleTest() {
        Locale defaultLocale = new Locale("en", "US");

        FastDateFormat.getInstance(AM_PM_HOUR_1_12_CORRECT, defaultLocale);
        FastDateFormat.getInstance(AM_PM_HOUR_0_11_CORRECT, defaultLocale);
        FastDateFormat.getInstance(MILITARY_HOUR_0_23_CORRECT, defaultLocale);
        FastDateFormat.getInstance(MILITARY_HOUR_1_24_CORRECT, defaultLocale);
        FastDateFormat.getInstance(WEEK_YEAR_CORRECT, defaultLocale);
    }

    public static void getInstanceTimezoneTest() {
        TimeZone tz = TimeZone.getDefault();

        FastDateFormat.getInstance(AM_PM_HOUR_1_12_CORRECT, tz);
        FastDateFormat.getInstance(AM_PM_HOUR_0_11_CORRECT, tz);
        FastDateFormat.getInstance(MILITARY_HOUR_0_23_CORRECT, tz);
        FastDateFormat.getInstance(MILITARY_HOUR_1_24_CORRECT, tz);
        FastDateFormat.getInstance(WEEK_YEAR_CORRECT, tz);
    }

    public static void periodTest() {
        // when "h" or "K" flags found, make sure that it ALSO CONTAINS "a" or "B"
        DateTimeFormatter.ofPattern(AM_PM_HOUR_1_12_CORRECT);
        DateTimeFormatter.ofPattern(AM_PM_HOUR_0_11_CORRECT);
        DateTimeFormatter.ofPattern("h:mm B");
        DateTimeFormatter.ofPattern("K:mm B");

        // when "H" or "k" flags found, make sure that it DOES NOT CONTAIN "a" or "B"
        DateTimeFormatter.ofPattern(MILITARY_HOUR_0_23_CORRECT);
        DateTimeFormatter.ofPattern(MILITARY_HOUR_1_24_CORRECT);

        // when "M" or "d" flags found, make sure that it DOES NOT CONTAIN "Y" (unless "w" is provided)
        DateTimeFormatter.ofPattern(WEEK_YEAR_CORRECT);

        // milli-of-day cannot be used together with Hours, Minutes, Seconds
        DateTimeFormatter.ofPattern("A");
        DateTimeFormatter.ofPattern("N");

        // fraction-of-second and nano-of-second cannot be used together
        DateTimeFormatter.ofPattern("S");
        DateTimeFormatter.ofPattern("n");

        // am-pm marker and period-of-day cannot be used together
        DateTimeFormatter.ofPattern("a");
        DateTimeFormatter.ofPattern("B");

        // year and year-of-era cannot be used together
        DateTimeFormatter.ofPattern("u");
        DateTimeFormatter.ofPattern("y");
    }
}
