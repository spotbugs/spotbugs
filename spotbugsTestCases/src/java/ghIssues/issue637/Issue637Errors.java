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

    public static void ofPatternTest(){
        DateTimeFormatter.ofPattern(AM_PM_HOUR_1_12_INCORRECT);
        DateTimeFormatter.ofPattern(AM_PM_HOUR_0_11_INCORRECT);
        DateTimeFormatter.ofPattern(MILITARY_HOUR_0_23_INCORRECT);
        DateTimeFormatter.ofPattern(MILITARY_HOUR_1_24_INCORRECT);
        DateTimeFormatter.ofPattern(WEEK_YEAR_INCORRECT);
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
}
