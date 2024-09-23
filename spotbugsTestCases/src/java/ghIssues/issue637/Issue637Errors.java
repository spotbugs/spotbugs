package ghIssues.issue637;

import org.apache.commons.lang3.time.FastDateFormat;

import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/637">GitHub issue</a>
 */
public final class Issue637Errors {

    private static final String AM_PM_HOUR_1_12_INCORRECT = "h:mm";
    private static final String AM_PM_HOUR_0_11_INCORRECT = "K:mm";
    private static final String MILITARY_HOUR_0_23_INCORRECT = "HH:mm a";
    private static final String MILITARY_HOUR_1_24_INCORRECT = "kk:mm a";
    private static final String WEEK_YEAR_INCORRECT = "YYYY-MM-dd";

    private Issue637Errors(){
        //To prevent instantiation
    }

    /**
     * Raises errors when bad format flag combinations are used in one argument constructor.
     */
    public void oneArgConstructorTest(){
        SimpleDateFormat hour12format1 = new SimpleDateFormat(AM_PM_HOUR_1_12_INCORRECT);
        SimpleDateFormat hour12format2 = new SimpleDateFormat(AM_PM_HOUR_0_11_INCORRECT);
        SimpleDateFormat hour24format1 = new SimpleDateFormat(MILITARY_HOUR_0_23_INCORRECT);
        SimpleDateFormat hour24format2 = new SimpleDateFormat(MILITARY_HOUR_1_24_INCORRECT);
        SimpleDateFormat weekYear = new SimpleDateFormat(WEEK_YEAR_INCORRECT);

        System.out.println(hour12format1.format(new Date()));
        System.out.println(hour12format2.format(new Date()));
        System.out.println(hour24format1.format(new Date()));
        System.out.println(hour24format2.format(new Date()));
        System.out.println(weekYear.format(new Date()));
    }

    /**
     * Raises errors when bad format flag combinations are used in two argument constructor with specified Locale
     */
    public static void twoArgLocaleConstructorTest(){
        Locale defaultLocale = new Locale("en", "US");

        SimpleDateFormat hour12format1 = new SimpleDateFormat(AM_PM_HOUR_1_12_INCORRECT, defaultLocale);
        SimpleDateFormat hour12format2 = new SimpleDateFormat(AM_PM_HOUR_0_11_INCORRECT, defaultLocale);
        SimpleDateFormat hour24format1 = new SimpleDateFormat(MILITARY_HOUR_0_23_INCORRECT, defaultLocale);
        SimpleDateFormat hour24format2 = new SimpleDateFormat(MILITARY_HOUR_1_24_INCORRECT, defaultLocale);
        SimpleDateFormat weekYear = new SimpleDateFormat(WEEK_YEAR_INCORRECT, defaultLocale);

        System.out.println(hour12format1.format(new Date()));
        System.out.println(hour12format2.format(new Date()));
        System.out.println(hour24format1.format(new Date()));
        System.out.println(hour24format2.format(new Date()));
        System.out.println(weekYear.format(new Date()));
    }

    /**
     * Raises errors when bad format flag combinations are used in two argument constructor with specified date
     * format symbols
     */
    public static void twoArgDateFormatSymbolConstructorTest(){
        DateFormatSymbols dateFormatSymbols = new DateFormatSymbols(Locale.getDefault());

        SimpleDateFormat hour12format1 = new SimpleDateFormat(AM_PM_HOUR_1_12_INCORRECT, dateFormatSymbols);
        SimpleDateFormat hour12format2 = new SimpleDateFormat(AM_PM_HOUR_0_11_INCORRECT, dateFormatSymbols);
        SimpleDateFormat hour24format1 = new SimpleDateFormat(MILITARY_HOUR_0_23_INCORRECT, dateFormatSymbols);
        SimpleDateFormat hour24format2 = new SimpleDateFormat(MILITARY_HOUR_1_24_INCORRECT, dateFormatSymbols);
        SimpleDateFormat weekYear = new SimpleDateFormat(WEEK_YEAR_INCORRECT, dateFormatSymbols);

        System.out.println(hour12format1.format(new Date()));
        System.out.println(hour12format2.format(new Date()));
        System.out.println(hour24format1.format(new Date()));
        System.out.println(hour24format2.format(new Date()));
        System.out.println(weekYear.format(new Date()));
    }

    /**
     * Raises errors when bad format flag combinations are used in "applyPattern" function
     */
    public static void applyPatternTest(){
        SimpleDateFormat hour12format1 = new SimpleDateFormat();
        SimpleDateFormat hour12format2 = new SimpleDateFormat();
        SimpleDateFormat hour24format1 = new SimpleDateFormat();
        SimpleDateFormat hour24format2 = new SimpleDateFormat();
        SimpleDateFormat weekYear = new SimpleDateFormat();

        hour12format1.applyPattern(AM_PM_HOUR_1_12_INCORRECT);
        hour12format2.applyPattern(AM_PM_HOUR_0_11_INCORRECT);
        hour24format1.applyPattern(MILITARY_HOUR_0_23_INCORRECT);
        hour24format2.applyPattern(MILITARY_HOUR_1_24_INCORRECT);
        weekYear.applyPattern(WEEK_YEAR_INCORRECT);
    }

    /**
     * Raises errors when bad format flag combinations are used in "applyLocalizedPattern" function
     */
    public static void applyLocalizedPatternTest(){
        SimpleDateFormat hour12format1 = new SimpleDateFormat();
        SimpleDateFormat hour12format2 = new SimpleDateFormat();
        SimpleDateFormat hour24format1 = new SimpleDateFormat();
        SimpleDateFormat hour24format2 = new SimpleDateFormat();
        SimpleDateFormat weekYear = new SimpleDateFormat();

        hour12format1.applyLocalizedPattern(AM_PM_HOUR_1_12_INCORRECT);
        hour12format2.applyLocalizedPattern(AM_PM_HOUR_0_11_INCORRECT);
        hour24format1.applyLocalizedPattern(MILITARY_HOUR_0_23_INCORRECT);
        hour24format2.applyLocalizedPattern(MILITARY_HOUR_1_24_INCORRECT);
        weekYear.applyLocalizedPattern(WEEK_YEAR_INCORRECT);
    }

    public static void ofPatternLocaleTest(){
        Locale defaultLocale = new Locale("en", "US");

        DateTimeFormatter.ofPattern(AM_PM_HOUR_1_12_INCORRECT, defaultLocale);
        DateTimeFormatter.ofPattern(AM_PM_HOUR_0_11_INCORRECT, defaultLocale);
        DateTimeFormatter.ofPattern(MILITARY_HOUR_0_23_INCORRECT, defaultLocale);
        DateTimeFormatter.ofPattern(MILITARY_HOUR_1_24_INCORRECT, defaultLocale);
        DateTimeFormatter.ofPattern(WEEK_YEAR_INCORRECT, defaultLocale);
    }

    public static void getInstanceTest() {
        FastDateFormat.getInstance(AM_PM_HOUR_1_12_INCORRECT);
        FastDateFormat.getInstance(AM_PM_HOUR_0_11_INCORRECT);
        FastDateFormat.getInstance(MILITARY_HOUR_0_23_INCORRECT);
        FastDateFormat.getInstance(MILITARY_HOUR_1_24_INCORRECT);
        FastDateFormat.getInstance(WEEK_YEAR_INCORRECT);
    }

    public static void getInstanceLocaleTest() {
        Locale defaultLocale = new Locale("en", "US");

        FastDateFormat.getInstance(AM_PM_HOUR_1_12_INCORRECT, defaultLocale);
        FastDateFormat.getInstance(AM_PM_HOUR_0_11_INCORRECT, defaultLocale);
        FastDateFormat.getInstance(MILITARY_HOUR_0_23_INCORRECT, defaultLocale);
        FastDateFormat.getInstance(MILITARY_HOUR_1_24_INCORRECT, defaultLocale);
        FastDateFormat.getInstance(WEEK_YEAR_INCORRECT, defaultLocale);
    }

    public static void getInstanceTimezoneTest() {
        TimeZone tz = TimeZone.getDefault();

        FastDateFormat.getInstance(AM_PM_HOUR_1_12_INCORRECT, tz);
        FastDateFormat.getInstance(AM_PM_HOUR_0_11_INCORRECT, tz);
        FastDateFormat.getInstance(MILITARY_HOUR_0_23_INCORRECT, tz);
        FastDateFormat.getInstance(MILITARY_HOUR_1_24_INCORRECT, tz);
        FastDateFormat.getInstance(WEEK_YEAR_INCORRECT, tz);
    }

    /* 35 testcases */
    public static void dateTimeFormatterTest() {
        // when "h" or "K" flags found, make sure that it ALSO CONTAINS "a" or "B"
        DateTimeFormatter.ofPattern(AM_PM_HOUR_1_12_INCORRECT);
        DateTimeFormatter.ofPattern(AM_PM_HOUR_0_11_INCORRECT);

        // when "H" or "k" flags found, make sure that it DOES NOT CONTAIN "a" or "B"
        DateTimeFormatter.ofPattern(MILITARY_HOUR_0_23_INCORRECT);
        DateTimeFormatter.ofPattern(MILITARY_HOUR_1_24_INCORRECT);
        DateTimeFormatter.ofPattern("HH:mm B");
        DateTimeFormatter.ofPattern("kk:mm B");

        // when "M" or "d" flags found, make sure that it DOES NOT CONTAIN "Y" (unless "w" is provided)
        DateTimeFormatter.ofPattern(WEEK_YEAR_INCORRECT);

        // milli-of-day cannot be used together with Hours, Minutes, Seconds
        DateTimeFormatter.ofPattern("h:mm:ss a A");
        DateTimeFormatter.ofPattern("K:mm:ss a A");
        DateTimeFormatter.ofPattern("HH:mm:ss a A");
        DateTimeFormatter.ofPattern("kk:mm:ss a A");
        DateTimeFormatter.ofPattern("h:mm:ss a N");
        DateTimeFormatter.ofPattern("K:mm:ss a N");
        DateTimeFormatter.ofPattern("HH:mm:ss a N");
        DateTimeFormatter.ofPattern("kk:mm a N");
        DateTimeFormatter.ofPattern("h:mm a A");
        DateTimeFormatter.ofPattern("K:mm a A");
        DateTimeFormatter.ofPattern("HH:mm a A");
        DateTimeFormatter.ofPattern("kk:mm a A");
        DateTimeFormatter.ofPattern("h:mm a N");
        DateTimeFormatter.ofPattern("K:mm a N");
        DateTimeFormatter.ofPattern("HH:mm a N");
        DateTimeFormatter.ofPattern("kk:mm a N");

        // milli-of-day and nano-of-day cannot be used together
        DateTimeFormatter.ofPattern("A N");
        DateTimeFormatter.ofPattern("N A");

        // fraction-of-second and nano-of-second cannot be used together
        DateTimeFormatter.ofPattern("S n");
        DateTimeFormatter.ofPattern("n S");

        // am-pm marker and period-of-day cannot be used together
        DateTimeFormatter.ofPattern("h:mm a B");
        DateTimeFormatter.ofPattern("K:mm a B");
        DateTimeFormatter.ofPattern("h:mm B a");
        DateTimeFormatter.ofPattern("K:mm B a");
        DateTimeFormatter.ofPattern("a B");
        DateTimeFormatter.ofPattern("B a");

        // year and year-of-era cannot be used together
        DateTimeFormatter.ofPattern("y u");
        DateTimeFormatter.ofPattern("u y");
    }
}
